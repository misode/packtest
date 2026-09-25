package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.BlockDataAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.function.Predicate;
import java.util.stream.Collectors;

class BlockAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos())
				.then(Commands.argument("block", BlockPredicateArgument.blockPredicate(context))
						.executes(ctx -> run(ctx, mode)))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		ServerLevel level = context.getSource().getLevel();
		Predicate<BlockInWorld> expect = BlockPredicateArgument.getBlockPredicate(context, "block");

		return mode.check(() -> {
			BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
			BlockInWorld blockInWorld = new BlockInWorld(level, pos, true);
			String expectedBlock = Assertion.getRawArgument(context, "block");

			return AssertResult.of(expect.test(blockInWorld) ? 1 : 0, negated -> Component.literal(!negated
					? String.format("Expected block %s at %s, found %s", expectedBlock, pos.toShortString(), getFormattedBlock(level, pos))
					: String.format("Did not expect block %s at %s", expectedBlock, pos.toShortString())));
		});
	}

	private static String getFormattedBlock(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		BlockEntity entity = level.getBlockEntity(pos);

		StringBuilder result = new StringBuilder(BuiltInRegistries.BLOCK.wrapAsHolder(state.getBlock()).getRegisteredName());
		String props = state.getValues().map(Property.Value::toString).collect(Collectors.joining(","));

		if (!props.isEmpty()) result.append('[').append(props).append(']');
		if (entity != null) result.append(new BlockDataAccessor(entity, pos).getData());

		return result.toString();
	}
}
