package io.github.misode.packtest.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTestExecutor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class SucceedCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("succeed")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(_ -> succeed()));
    }

    private static int succeed() throws CommandSyntaxException {
        PackTestExecutor.current().succeed();
        return Command.SINGLE_SUCCESS;
    }
}
