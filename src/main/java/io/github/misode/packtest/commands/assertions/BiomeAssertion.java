package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrTagArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

class BiomeAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("biome").then(Commands.argument("pos", BlockPosArgument.blockPos())
				.then(Commands.argument("biome", ResourceOrTagArgument.resourceOrTag(context, Registries.BIOME))
						.executes(ctx -> run(ctx, mode)))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		ServerLevel level = context.getSource().getLevel();

		return mode.check(() -> {
			BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
			ResourceOrTagArgument.Result<Biome> expect = ResourceOrTagArgument.getResourceOrTag(context, "biome", Registries.BIOME);
			Holder<Biome> found = level.getBiome(pos);

			return AssertResult.of(expect.test(found) ? 1 : 0, negated -> Component.literal(!negated
					? String.format("Expected biome %s at %s, found %s", expect.asPrintable(), pos.toShortString(), found.getRegisteredName())
					: String.format("Did not expect biome %s at %s", expect.asPrintable(), pos.toShortString())));
		});
	}
}
