package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTestExecutor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

public class FailCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        dispatcher.register(Commands.literal("fail")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(_ -> fail())
                .then(Commands.argument("message", ComponentArgument.textComponent(context))
                        .executes(FailCommand::fail)));
    }

    private static int fail() throws CommandSyntaxException {
        PackTestExecutor.current().fail(Component.literal("Fail command invoked"));
        return 0;
    }

    private static int fail(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Component resolvedMessage;
        try {
            resolvedMessage = ComponentArgument.getResolvedComponent(context, "message");
        } catch (CommandSyntaxException e) {
            resolvedMessage = ComponentUtils.fromMessage(e.getRawMessage());
        }
        PackTestExecutor.current().fail(resolvedMessage);
        return 0;
    }
}
