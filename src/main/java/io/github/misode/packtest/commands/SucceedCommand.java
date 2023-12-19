package io.github.misode.packtest.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.ContextChain;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import static net.minecraft.commands.Commands.literal;

public class SucceedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("succeed")
                .requires(ctx -> ctx.hasPermission(2))
                .executes(new SucceedCommand.SucceedCustomExecutor())
                .then(AssertCommand.addConditions(literal("when"), buildContext, predicate -> when(true, predicate))
                        .then(AssertCommand.addConditions(literal("not"), buildContext, predicate -> when(false, predicate))))
        );
    }

    static class SucceedCustomExecutor implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        public void run(CommandSourceStack sourceStack, ContextChain<CommandSourceStack> chain, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> execution) {
            PackTestLibrary.INSTANCE.getHelperAt(sourceStack).ifPresent(GameTestHelper::succeed);
            sourceStack.callback().onSuccess(1);
            Frame frame = execution.currentFrame();
            frame.returnSuccess(1);
            frame.discard();
        }
    }

    private static Command<CommandSourceStack> when(boolean expectOk, AssertCommand.AssertPredicate predicate) {
        return ctx -> {
            PackTestLibrary.INSTANCE.getHelperAt(ctx.getSource()).ifPresent(helper -> {
                helper.succeedWhen(() -> {
                    predicate.apply(ctx).get(expectOk).ifPresent(message -> {
                        throw new GameTestAssertException(message);
                    });
                });
            });
            return 1;
        };
    }
}
