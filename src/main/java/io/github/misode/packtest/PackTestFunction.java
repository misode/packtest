package io.github.misode.packtest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public record PackTestFunction(List<Step> steps, Map<String, String> directives) {
    public void run(GameTestHelper helper) {
        CommandSourceStack source = this.createCommandSourceStack(helper);

        ChatListener chatListener = new ChatListener();
        ((PackTestInfo)((PackTestHelper)helper).packtest$getInfo()).packtest$setChatListener(chatListener);
        helper.onEachTick(chatListener::reset);

        GameTestInfo testInfo = ((PackTestHelper)helper).packtest$getInfo();
        GameTestSequence sequence = helper.startSequence();
        for (Step step : this.steps) {
            if (step.command.startsWith("await delay ")) {
                try {
                    String timeArgument = step.command.substring("await delay ".length());
                    int ticks = TimeArgument.time().parse(new StringReader(timeArgument));
                    ((PackTestSequence)sequence).packtest$thenIdle(ticks, step.line, timeArgument);
                } catch (CommandSyntaxException e) {
                    throw new LineNumberException(Component.literal("Whilst parsing command: " + e.getMessage()), 0, step.line);
                }
                continue;
            }
            Runnable runStep = () -> {
                if (testInfo.isDone() || testInfo.hasFailed()) {
                    return;
                }
                try {
                    Commands.executeCommandInContext(source,ctx ->
                            ExecutionContext.queueInitialCommandExecution(ctx, step.command, step.chain, source, CommandResultCallback.EMPTY));
                } catch (GameTestAssertException e) {
                    throw new LineNumberException(((PackTestAssertException)e).packtest$getMessage(), ((PackTestAssertException)e).packtest$getTick(), step.line);
                }
            };
            if (step.command.stripLeading().startsWith("await ")) {
                sequence.thenWaitUntil(runStep);
            } else {
                sequence.thenExecute(runStep);
            }
        }
        sequence.thenSucceed();
    }

    private CommandSourceStack createCommandSourceStack(GameTestHelper helper) {
        CommandSourceStack source = helper.getLevel().getServer().createCommandSourceStack()
                .withLevel(helper.getLevel())
                .withPosition(helper.absoluteVec(Vec3.ZERO))
                .withSuppressedOutput();
        ((PackTestSourceStack) source).packtest$setHelper(helper);

        Optional<Coordinates> coordinates = this.getDummyPos();
        if (coordinates.isPresent()) {
            try {
                Vec3 pos = coordinates.get().getPosition(source);
                Vec2 rot = coordinates.get().getRotation(source);
                Dummy dummy = Dummy.createRandom(helper.getLevel(), pos, rot);
                dummy.setOnGround(true); // little hack because we know the dummy will be on the ground
                source = source.withEntity(dummy);
            } catch (IllegalArgumentException e) {
                helper.fail(Component.literal("Failed to initialize test with dummy"));
            }
        }

        return source;
    }

    private Optional<Coordinates> getDummyPos() {
        String dummyValue = this.directives.get("dummy");
        if (dummyValue == null) {
            return Optional.empty();
        }
        if (dummyValue.equals("true")) {
            dummyValue = "~0.5 ~ ~0.5";
        }
        try {
            return Optional.of(Vec3Argument.vec3().parse(new StringReader(dummyValue)));
        } catch (CommandSyntaxException e) {
            return Optional.empty();
        }
    }

    public TestData<Holder<TestEnvironmentDefinition<?>>> getTestData(HolderGetter.Provider registries) {
        var environments = registries.lookup(Registries.TEST_ENVIRONMENT).orElseThrow();
        Identifier environmentId = Optional.ofNullable(this.directives.get("environment")).map(Identifier::parse).orElse(GameTestEnvironments.DEFAULT_KEY.identifier());
        Holder<TestEnvironmentDefinition<?>> environment = environments.getOrThrow(ResourceKey.create(Registries.TEST_ENVIRONMENT, environmentId));
        ResourceKey<Level> dimension = Level.OVERWORLD; // TODO: make configurable?
        Identifier structure = Optional.ofNullable(this.directives.get("template")).map(Identifier::parse).orElse(Identifier.withDefaultNamespace("empty"));
        int maxTicks = Optional.ofNullable(this.directives.get("timeout")).map(Integer::parseInt).orElse(100);
        boolean required = Optional.ofNullable(this.directives.get("optional")).map(s -> !Boolean.parseBoolean(s)).orElse(true);
        boolean skyAccess = Optional.ofNullable(this.directives.get("skyaccess")).map(Boolean::parseBoolean).orElse(false);
        return new TestData<>(environment, dimension, structure, maxTicks, 0, required, Rotation.NONE, false, 1, 1, skyAccess, 0);
    }

    public static PackTestFunction fromLines(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack context,
            List<String> lines) throws IllegalArgumentException {
        HashMap<String, String> directives = new HashMap<>();
        List<Step> steps = new ArrayList<>();
        int i = 0;

        while (i < lines.size()) {
            final int line = i + 1;
            StringBuilder builder = new StringBuilder(lines.get(i++).trim());

            while (!builder.isEmpty() && builder.charAt(builder.length() - 1) == '\\') {
                if (i >= lines.size()) {
                    throw new IllegalArgumentException("Line continuation at end of file");
                }

                builder.deleteCharAt(builder.length() - 1);
                builder.append(lines.get(i++).trim());
                CommandFunction.checkCommandLineLength(builder);
            }

            String command = builder.toString();
            if (command.isEmpty()) continue;

            CommandFunction.checkCommandLineLength(command);
            StringReader reader = new StringReader(command);
            if (!reader.canRead()) continue;

            if (reader.peek() == '#') {
                parseDirective(reader, directives);
                continue;
            }

            try {
                steps.add(new Step(command, parseCommand(dispatcher, context, command), line));
            } catch (CommandSyntaxException e) {
                throw new IllegalArgumentException("Whilst parsing command on line " + line + ": " + e.getMessage());
            }
        }

        return new PackTestFunction(steps, directives);
    }

    private static void parseDirective(
            StringReader reader,
            Map<String, String> directives) throws IllegalArgumentException {
        reader.skip();
        reader.skipWhitespace();

        if (reader.canRead() && reader.peek() == '@') {
            reader.skip();
            String name = reader.readUnquotedString();
            reader.skipWhitespace();
            String value = reader.canRead() ? reader.getRemaining() : null;
            directives.put(name, value != null ? value : "true");
        }
    }

    private static ContextChain<CommandSourceStack> parseCommand(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack context,
            String command) throws CommandSyntaxException {
        ParseResults<CommandSourceStack> parseResults = dispatcher.parse(command, context);
        Commands.validateParseResults(parseResults);
        return ContextChain.tryFlatten(parseResults.getContext().build(command))
                .orElseThrow(() -> CommandSyntaxException.BUILT_IN_EXCEPTIONS
                        .dispatcherUnknownCommand()
                        .createWithContext(parseResults.getReader()));
    }

    public record Step(String command, ContextChain<CommandSourceStack> chain, int line) {}
}
