package io.github.misode.packtest.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTestExecutor;
import io.github.misode.packtest.commands.assertions.Assertion;
import io.github.misode.packtest.commands.assertions.Assertions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;

public class AwaitCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        Assertion.Mode plain = new Assertion.Mode(false, false);
        Assertion.Mode not = new Assertion.Mode(false, true);

        dispatcher.register(Assertions.build(dispatcher, context, plain, Commands.literal("await")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Assertions.build(dispatcher, context, not, Commands.literal("not")))
                .then(Commands.literal("delay")
                        .then(Commands.argument("time", TimeArgument.time())
                                .executes(AwaitCommand::delay)))));
    }

    private static int delay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        int time = IntegerArgumentType.getInteger(context, "time");
        PackTestExecutor.current().await(time);
        return Command.SINGLE_SUCCESS;
    }
}
