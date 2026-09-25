package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.level.ServerLevel;

class LoadedAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("loaded").then(Commands.argument("pos", BlockPosArgument.blockPos())
				.executes(ctx -> run(ctx, mode))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		ServerLevel level = context.getSource().getLevel();

		return mode.check(() -> {
			BlockPos pos = BlockPosArgument.getBlockPos(context, "pos");

			return AssertResult.of(ExecuteCommand.isChunkLoaded(level, pos) ? 1 : 0, negated -> Component.literal(!negated
					? String.format("Expected chunk at %s to be loaded", pos.toShortString())
					: String.format("Did not expect chunk at %s to be loaded", pos.toShortString())));
		});
	}
}
