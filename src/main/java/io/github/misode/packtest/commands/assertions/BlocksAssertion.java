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

import java.util.OptionalInt;

class BlocksAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("blocks").then(Commands.argument("start", BlockPosArgument.blockPos())
				.then(Commands.argument("end", BlockPosArgument.blockPos())
						.then(Commands.argument("destination", BlockPosArgument.blockPos())
								.then(Commands.literal("all").executes(ctx -> run(ctx, mode, false)))
								.then(Commands.literal("masked").executes(ctx -> run(ctx, mode, true)))))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode, boolean skipAir) throws CommandSyntaxException {
		ServerLevel level = context.getSource().getLevel();

		return mode.check(() -> {
			BlockPos start = BlockPosArgument.getLoadedBlockPos(context, "start");
			BlockPos end = BlockPosArgument.getLoadedBlockPos(context, "end");
			BlockPos destination = BlockPosArgument.getLoadedBlockPos(context, "destination");
			OptionalInt matched = ExecuteCommand.checkRegions(level, start, end, destination, skipAir);
			int result = matched.isPresent() ? Math.max(matched.getAsInt(), 1) : 0;

			return AssertResult.of(result, negated -> Component.literal(!negated
					? String.format("Expected region %s..%s to match blocks at %s", start.toShortString(), end.toShortString(), destination.toShortString())
					: String.format("Did not expect region %s..%s to match blocks at %s", start.toShortString(), end.toShortString(), destination.toShortString())));
		});
	}
}
