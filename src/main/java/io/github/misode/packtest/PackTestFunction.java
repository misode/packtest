package io.github.misode.packtest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import java.util.*;

public record PackTestFunction(List<Step> steps, Map<String, String> directives) {
    public void run(GameTestHelper helper) {
        PackTestExecutor executor = new PackTestExecutor(helper, this.maxTicks());
        executor.run(this);
    }

    public TestData<Holder<TestEnvironmentDefinition<?>>> getTestData(HolderGetter.Provider registries) {
        var environments = registries.lookup(Registries.TEST_ENVIRONMENT).orElseThrow();
        Identifier environmentId = Optional.ofNullable(this.directives.get("environment")).map(Identifier::parse).orElse(GameTestEnvironments.DEFAULT_KEY.identifier());
        Holder<TestEnvironmentDefinition<?>> environment = environments.getOrThrow(ResourceKey.create(Registries.TEST_ENVIRONMENT, environmentId));
        ResourceKey<Level> dimension = Level.OVERWORLD; // TODO: make configurable?
        Identifier structure = Optional.ofNullable(this.directives.get("template")).map(Identifier::parse).orElse(Identifier.withDefaultNamespace("empty"));
        int maxTicks = this.maxTicks();
        boolean required = Optional.ofNullable(this.directives.get("optional")).map(s -> !Boolean.parseBoolean(s)).orElse(true);
        boolean skyAccess = Optional.ofNullable(this.directives.get("skyaccess")).map(Boolean::parseBoolean).orElse(false);
        return new TestData<>(environment, dimension, structure, maxTicks, 0, required, Rotation.NONE, false, 1, 1, skyAccess, 0);
    }

    private int maxTicks() {
        return Optional.ofNullable(this.directives.get("timeout")).map(Integer::parseInt).orElse(100);
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
