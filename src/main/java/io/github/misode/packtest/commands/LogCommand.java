package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTest;
import io.github.misode.packtest.PackTestExecutor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class LogCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("log")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("message", ComponentArgument.textComponent(context))
                        .executes(LogCommand::log)));
    }

    private static int log(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Component resolvedMessage;
        try {
            resolvedMessage = ComponentArgument.getResolvedComponent(context, "message");
        } catch (CommandSyntaxException e) {
            resolvedMessage = ComponentUtils.fromMessage(e.getRawMessage());
        }
        PackTest.LOGGER.info("{} logged: {}", PackTestExecutor.current().testId, resolvedMessage.getString());
        return 0;
    }
}