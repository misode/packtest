package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

class DimensionAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("dimension").then(Commands.argument("dimension", DimensionArgument.dimension())
				.executes(ctx -> run(ctx, mode))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		ServerLevel level = context.getSource().getLevel();

		return mode.check(() -> {
			ServerLevel expect = DimensionArgument.getDimension(context, "dimension");

			return AssertResult.of(expect == level ? 1 : 0, negated -> Component.literal(!negated
					? String.format("Expected dimension %s, found %s", expect.dimension().identifier(), level.dimension().identifier())
					: String.format("Did not expect dimension %s", expect.dimension().identifier())));
		});
	}
}
