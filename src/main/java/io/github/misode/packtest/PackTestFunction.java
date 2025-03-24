package io.github.misode.packtest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.FunctionInstantiationException;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record PackTestFunction(Map<String, String> directives, List<Step> steps, int permissionLevel) {
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^#\\s*@(\\w+)(?:\\s+(.+))?$");

    public void run(GameTestHelper helper) {
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withPosition(helper.absoluteVec(Vec3.ZERO))
                .withPermission(this.permissionLevel)
                .withSuppressedOutput();
        ((PackTestSourceStack) source).packtest$setHelper(helper);

        Vec3 dummyPos = this.getDummyPos(source).orElse(null);
        Dummy dummy;
        if (dummyPos != null) {
            try {
                dummy = Dummy.createRandom(helper.getLevel().getServer(), helper.getLevel().dimension(), dummyPos);
                dummy.setOnGround(true); // little hack because we know the dummy will be on the ground
                source = source.withEntity(dummy);
            } catch (IllegalArgumentException e) {
                throw new GameTestAssertException(Component.literal("Failed to initialize test with dummy"), 0);
            }
        }

        ChatListener chatListener = new ChatListener();
        ((PackTestInfo)((PackTestHelper)helper).packtest$getInfo()).packtest$setChatListener(chatListener);
        helper.onEachTick(chatListener::reset);

        CommandDispatcher<CommandSourceStack> dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        GameTestSequence sequence = helper.startSequence();
        for (Step step : this.steps) {
            step.register(sequence, dispatcher, source);
        }
        sequence.thenSucceed();
    }

    public static PackTestFunction fromLines(List<String> lines, int permissionLevel) {
        HashMap<String, String> directives = new HashMap<>();
        for (String line : lines) {
            if (!line.startsWith("#")) {
                break;
            }
            Matcher matcher = DIRECTIVE_PATTERN.matcher(line);
            if (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                directives.put(key, value != null ? value : "true");
            }
        }

        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += 1) {
            String line = lines.get(i).stripLeading();
            if (!line.startsWith("#") && !line.isEmpty()) {
                steps.add(new Step(lines.get(i), i + 1));
            }
        }

        return new PackTestFunction(directives, steps, permissionLevel);
    }

    private Optional<Vec3> getDummyPos(CommandSourceStack source) {
        String dummyValue = this.directives.get("dummy");
        if (dummyValue == null) {
            return Optional.empty();
        }
        if (dummyValue.equals("true")) {
            dummyValue = "~0.5 ~ ~0.5";
        }
        try {
            return Optional.of(Vec3Argument.vec3().parse(new StringReader(dummyValue)).getPosition(source));
        } catch (CommandSyntaxException e) {
            return Optional.empty();
        }
    }

    public TestData<Holder<TestEnvironmentDefinition>> getTestData(HolderGetter.Provider registries) {
        var environments = registries.lookup(Registries.TEST_ENVIRONMENT).orElseThrow();
        ResourceLocation environmentId = Optional.ofNullable(this.directives.get("environment")).map(ResourceLocation::parse).orElse(GameTestEnvironments.DEFAULT_KEY.location());
        Holder<TestEnvironmentDefinition> environment = environments.getOrThrow(ResourceKey.create(Registries.TEST_ENVIRONMENT, environmentId));
        ResourceLocation structure = Optional.ofNullable(this.directives.get("template")).map(ResourceLocation::parse).orElse(ResourceLocation.withDefaultNamespace("empty"));
        int maxTicks = Optional.ofNullable(this.directives.get("timeout")).map(Integer::parseInt).orElse(100);
        boolean required = Optional.ofNullable(this.directives.get("optional")).map(s -> !Boolean.parseBoolean(s)).orElse(true);
        boolean skyAccess = Optional.ofNullable(this.directives.get("skyaccess")).map(Boolean::parseBoolean).orElse(false);
        return new TestData<>(environment, structure, maxTicks, 0, required, Rotation.NONE, false, 1, 1, skyAccess);
    }

    public record Step(String line, int lineNumber) {
        public void register(GameTestSequence sequence, CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source) {
            if (this.line.startsWith("await delay ")) {
                try {
                    String timeArgument = line.substring("await delay ".length());
                    int ticks = TimeArgument.time().parse(new StringReader(timeArgument));
                    ((PackTestSequence)sequence).packtest$thenIdle(ticks, lineNumber, timeArgument);
                } catch (CommandSyntaxException e) {
                    throw new LineNumberException(Component.literal("Whilst parsing command: " + e.getMessage()), 0, lineNumber);
                }
                return;
            }
            try {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("packtest", "internal");
                CommandFunction<CommandSourceStack> function = CommandFunction.fromLines(id, dispatcher, source, List.of(this.line));
                InstantiatedFunction<CommandSourceStack> instantiated = function.instantiate(null, dispatcher);
                Runnable runCommands = () -> {
                    try {
                        Commands.executeCommandInContext(
                                source,
                                e -> ExecutionContext.queueInitialFunctionCall(e, instantiated, source, CommandResultCallback.EMPTY)
                        );
                    } catch (GameTestAssertException e) {
                        throw new LineNumberException(((PackTestAssertException)e).packtest$getMessage(), ((PackTestAssertException)e).packtest$getTick(), lineNumber);
                    }
                };
                if (line.stripLeading().startsWith("await ")) {
                    sequence.thenWaitUntil(runCommands);
                } else {
                    sequence.thenExecute(runCommands);
                }
            } catch (IllegalArgumentException e) {
                String message = e.getMessage().replaceFirst("^Whilst parsing command on line \\d+: ", "");
                if (message.equals("Line continuation at end of file")) {
                    message = "Line continuation is not supported in tests";
                }
                throw new LineNumberException(Component.literal("Whilst parsing command: " + message), 0, lineNumber);
            } catch (FunctionInstantiationException e) {
                String message = e.messageComponent().getString();
                throw new LineNumberException(Component.literal("Whilst instantiating command: " + message), 0, lineNumber);
            }
        }
    }
}
