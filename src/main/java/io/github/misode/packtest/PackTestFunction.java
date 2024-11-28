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
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackTestFunction {
    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^#\\s*@(\\w+)(?:\\s+(.+))?$");
    private static final String DEFAULT_BATCH = "packtestBatch";
    private static final String DEFAULT_TEMPLATE = "packtest:empty";
    private final ResourceLocation id;
    private final Map<String, String> directives;
    private final List<Step> steps;

    public PackTestFunction(ResourceLocation id, Map<String, String> directives, List<Step> steps) {
        this.id = id;
        this.directives = directives;
        this.steps = steps;
    }

    public static PackTestFunction fromLines(ResourceLocation id, CommandDispatcher<CommandSourceStack> dispatcher, List<String> lines) {
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
            if (line.startsWith("await delay ")) {
                steps.add(new IdleStep(line.substring("await delay ".length()), i + 1));
            } else if (!line.startsWith("#") && !line.isEmpty()) {
                steps.add(new CommandStep(lines.get(i), i + 1, dispatcher));
            }
        }

        return new PackTestFunction(id, directives, steps);
    }

    private String getTestName() {
        return this.id.toLanguageKey();
    }

    private String getTemplateName() {
        return this.directives.getOrDefault("template", DEFAULT_TEMPLATE);
    }

    private String getBatchName() {
        return this.directives.getOrDefault("batch", DEFAULT_BATCH);
    }

    private int getTimeout() {
        return Optional.ofNullable(this.directives.get("timeout")).map(Integer::parseInt).orElse(100);
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

    private boolean isRequired() {
        return Optional.ofNullable(this.directives.get("optional")).map(s -> !Boolean.parseBoolean(s)).orElse(true);
    }

    private boolean needsSkyAccess() {
        return Optional.ofNullable(this.directives.get("skyaccess")).map(Boolean::parseBoolean).orElse(false);
    }

    public void registerBatchHook(int permissionLevel, Map<String, Consumer<ServerLevel>> map, String type) {
        String command = this.directives.get(type + "batch");
        if (command == null) {
            return;
        }
        String batchName = this.getBatchName();
        Consumer<ServerLevel> oldBefore = map.putIfAbsent(batchName, (level) -> {
            CommandSourceStack source = level.getServer().createCommandSourceStack()
                    .withPermission(permissionLevel);
            level.getServer().getCommands().performPrefixedCommand(source, command);
        });
        if (oldBefore != null) {
            PackTest.LOGGER.error("Only one @{}batch is allowed per batch. Batch '{}' has more than one!", type, batchName);
        }
    }

    public TestFunction toTestFunction(int permissionLevel) {
        return new TestFunction(
                this.getBatchName(),
                this.getTestName(),
                this.getTemplateName(),
                StructureUtils.getRotationForRotationSteps(0),
                this.getTimeout(),
                0L,
                this.isRequired(),
                false,
                1,
                1,
                this.needsSkyAccess(),
                createTestBody(permissionLevel));
    }

    private Consumer<GameTestHelper> createTestBody(int permissionLevel) {
        return (helper) -> {
            CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                    .withPosition(helper.absoluteVec(Vec3.ZERO))
                    .withPermission(permissionLevel)
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
                    throw new GameTestAssertException("Failed to initialize test with dummy");
                }
            }

            ChatListener chatListener = new ChatListener();
            ((PackTestInfo)((PackTestHelper)helper).packtest$getInfo()).packtest$setChatListener(chatListener);
            helper.onEachTick(chatListener::reset);

            GameTestSequence sequence = helper.startSequence();
            for (Step step : this.steps) {
                step.register(sequence, source);
            }
            sequence.thenSucceed();
        };
    }

    public interface Step {
        void register(GameTestSequence sequence, CommandSourceStack source);
    }

    public record CommandStep(String line, int lineNumber, CommandDispatcher<CommandSourceStack> dispatcher) implements Step {
        @Override
        public void register(GameTestSequence sequence, CommandSourceStack source) {
            try {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath("packtest", "internal");
                CommandFunction<CommandSourceStack> function = CommandFunction.fromLines(id, this.dispatcher, source, List.of(this.line));
                InstantiatedFunction<CommandSourceStack> instantiated = function.instantiate(null, this.dispatcher);
                Runnable runCommands = () -> {
                    try {
                        Commands.executeCommandInContext(
                                source,
                                e -> ExecutionContext.queueInitialFunctionCall(e, instantiated, source, CommandResultCallback.EMPTY)
                        );
                    } catch (GameTestAssertException e) {
                        throw new LineNumberException(e.getMessage(), lineNumber);
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
                throw new LineNumberException("Whilst parsing command: " + message, lineNumber);
            } catch (FunctionInstantiationException e) {
                String message = e.messageComponent().getString();
                throw new LineNumberException("Whilst instantiating command: " + message, lineNumber);
            }
        }
    }

    public record IdleStep(String argument, int lineNumber) implements Step {
        @Override
        public void register(GameTestSequence sequence, CommandSourceStack source) {
            try {
                int ticks = TimeArgument.time().parse(new StringReader(argument));
                ((PackTestSequence)sequence).packtest$thenIdle(ticks, lineNumber, argument);
            } catch (CommandSyntaxException e) {
                throw new LineNumberException("Whilst parsing command: " + e.getMessage(), lineNumber);
            }
        }
    }
}
