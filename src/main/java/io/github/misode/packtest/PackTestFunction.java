package io.github.misode.packtest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.compress.utils.Lists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

        CommandSourceStack sourceStack = new CommandSourceStack(
                CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 2, "", CommonComponents.EMPTY, null, null
        );
        try {
            CommandFunction.fromLines(id, dispatcher, sourceStack, lines);
        } catch (IllegalArgumentException e) {
            return new PackTestFunction(id, directives, List.of((sequence, source) -> {
                throw new GameTestAssertException(e.getMessage());
            }));
        }

        List<Step> steps = Lists.newArrayList();
        List<String> currentLines = Lists.newArrayList();
        for (String line : lines) {
            String sLine = line.stripLeading();
            if (sLine.startsWith("await ")) {
                if (!currentLines.isEmpty()) {
                    steps.add(new ExecuteStep(currentLines, dispatcher));
                    currentLines = Lists.newArrayList();
                }
                if (sLine.startsWith("await for ")) {
                    steps.add(new IdleStep(line.substring("await for ".length())));
                } else {
                    steps.add(new AwaitStep(line, dispatcher));
                }
            } else {
                currentLines.add(line);
            }
        }
        if (!currentLines.isEmpty()) {
            steps.add(new ExecuteStep(currentLines, dispatcher));
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
            PackTest.LOGGER.error("Only one @" + type + "batch is allowed per batch. Batch '" + batchName + "' has more than one!");
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
                1,
                1,
                createTestBody(permissionLevel));
    }

    private Consumer<GameTestHelper> createTestBody(int permissionLevel) {
        return (helper) -> {
            CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                    .withPosition(helper.absoluteVec(Vec3.ZERO).add(0, 1, 0))
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

            SoundListener soundListener = new SoundListener();
            ((PackTestInfo)((PackTestHelper)helper).packtest$getInfo()).packtest$setSoundListener(soundListener);
            helper.onEachTick(soundListener::reset);

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

    public record ExecuteStep(List<String> lines, CommandDispatcher<CommandSourceStack> dispatcher) implements Step {
        @Override
        public void register(GameTestSequence sequence, CommandSourceStack source) {
            try {
                ResourceLocation id = new ResourceLocation("packtest", "internal");
                CommandFunction<CommandSourceStack> function = CommandFunction.fromLines(id, this.dispatcher, source, this.lines);
                InstantiatedFunction<CommandSourceStack> instantiated = function.instantiate(null, this.dispatcher, source);
                sequence.thenExecute(() -> Commands.executeCommandInContext(
                        source,
                        e -> ExecutionContext.queueInitialFunctionCall(e, instantiated, source, CommandResultCallback.EMPTY)
                ));
            } catch (IllegalArgumentException e) {
                throw new GameTestAssertException(e.getMessage());
            } catch (FunctionInstantiationException e) {
                String message = e.messageComponent().getString();
                throw new GameTestAssertException("Failed to instantiate test function: " + message);
            }
        }
    }

    public record AwaitStep(String line, CommandDispatcher<CommandSourceStack> dispatcher) implements Step {
        @Override
        public void register(GameTestSequence sequence, CommandSourceStack source) {
            ParseResults<CommandSourceStack> parseResults = dispatcher.parse(line, source);
            ContextChain<CommandSourceStack> chain = ContextChain.tryFlatten(parseResults.getContext().build(line))
                    .orElseThrow(() -> new GameTestAssertException("Unknown or incomplete command"));
            sequence.thenWaitUntil(() -> Commands.executeCommandInContext(
                    source,
                    e -> ExecutionContext.queueInitialCommandExecution(e, line, chain, source, CommandResultCallback.EMPTY)
            ));
        }
    }

    public record IdleStep(String argument) implements Step {
        @Override
        public void register(GameTestSequence sequence, CommandSourceStack source) {
            try {
                int ticks = TimeArgument.time().parse(new StringReader(argument));
                sequence.thenIdle(ticks);
            } catch (CommandSyntaxException e) {
                throw new GameTestAssertException(e.getMessage());
            }
        }
    }
}
