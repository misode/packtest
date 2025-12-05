package io.github.misode.packtest.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;

import java.util.function.Function;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AwaitCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> awaitBuilder = literal("await")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        addConditions(awaitBuilder, buildContext, predicate -> new AssertCommand.AssertCustomExecutor(true, predicate));
        LiteralArgumentBuilder<CommandSourceStack> notBuilder = literal("not");
        addConditions(notBuilder, buildContext, predicate -> new AssertCommand.AssertCustomExecutor(false, predicate));
        awaitBuilder = awaitBuilder.then(notBuilder);
        dispatcher.register(awaitBuilder);
    }

    public static void addConditions(LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext buildContext, Function<AssertCommand.AssertPredicate, Command<CommandSourceStack>> expect) {
        AssertCommand.addConditions(builder, buildContext, expect);
        builder.then(literal("delay")
                .then(argument("time", TimeArgument.time())
                        .executes(expect.apply(ctx -> AssertCommand.err("Timed out")))));
    }
}
