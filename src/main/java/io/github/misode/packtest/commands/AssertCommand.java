package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.github.misode.packtest.commands.assertions.Assertion;
import io.github.misode.packtest.commands.assertions.Assertions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class AssertCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        Assertion.Mode plain = new Assertion.Mode(true, false);
        Assertion.Mode not = new Assertion.Mode(true, true);

        dispatcher.register(Assertions.build(dispatcher, context, plain, Commands.literal("assert")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Assertions.build(dispatcher, context, not, Commands.literal("not")))));
    }
}
