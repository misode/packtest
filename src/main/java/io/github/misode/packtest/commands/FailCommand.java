package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class FailCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("fail")
                .requires(ctx -> ctx.hasPermission(2))
                .then(argument("message", ComponentArgument.textComponent())
                        .executes(new FailCommand.FailCustomExecutor())
                )
        );
    }

    static class FailCustomExecutor implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        public void run(CommandSourceStack sourceStack, ContextChain<CommandSourceStack> chain, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> execution) {
            CommandContext<CommandSourceStack> ctx = chain.getTopContext().copyFor(sourceStack);
            PackTestLibrary.INSTANCE.getHelperAt(sourceStack).ifPresent(helper -> {
                try {
                    Component message = ComponentUtils.updateForEntity(
                            ctx.getSource(),
                            ComponentArgument.getComponent(ctx, "message"),
                            null,
                            0
                    );
                    helper.fail(message.getString());
                } catch (CommandSyntaxException ignored) {}
            });
            sourceStack.callback().onFailure();
            Frame frame = execution.currentFrame();
            frame.returnFailure();
            frame.discard();
        }
    }
}
