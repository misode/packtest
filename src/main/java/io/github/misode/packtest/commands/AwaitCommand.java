package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import static net.minecraft.commands.Commands.literal;

public class AwaitCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> awaitBuilder = literal("await")
                .requires(ctx -> ctx.hasPermission(2));
        AssertCommand.addConditions(awaitBuilder, buildContext, predicate -> new AssertCommand.AssertCustomExecutor(true, predicate));
        LiteralArgumentBuilder<CommandSourceStack> notBuilder = literal("not");
        AssertCommand.addConditions(notBuilder, buildContext, predicate -> new AssertCommand.AssertCustomExecutor(false, predicate));
        awaitBuilder = awaitBuilder.then(notBuilder);
        dispatcher.register(awaitBuilder);
    }
}
