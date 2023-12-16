package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.ContextChain;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;

import static net.minecraft.commands.Commands.literal;

public class SucceedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("succeed")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(new SucceedCommand.SucceedCustomExecutor())
        );
    }

    static class SucceedCustomExecutor implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        public void run(CommandSourceStack sourceStack, ContextChain<CommandSourceStack> chain, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> execution) {
            sourceStack.callback().onSuccess(1);
            Frame frame = execution.currentFrame();
            frame.returnSuccess(1);
            frame.discard();
        }
    }
}
