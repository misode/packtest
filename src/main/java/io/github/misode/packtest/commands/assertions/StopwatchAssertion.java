package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.StopwatchCommand;
import net.minecraft.world.Stopwatch;
import net.minecraft.world.Stopwatches;

class StopwatchAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("stopwatch").then(Commands.argument("id", IdentifierArgument.id())
				.suggests(StopwatchCommand.SUGGEST_STOPWATCHES)
				.then(Commands.argument("range", RangeArgument.floatRange())
						.executes(ctx -> run(ctx, mode)))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		MinecraftServer server = context.getSource().getServer();
		MinMaxBounds.Doubles range = RangeArgument.Floats.getRange(context, "range");

		return mode.check(() -> {
			Identifier id = IdentifierArgument.getId(context, "id");
			Stopwatch stopwatch = server.getStopwatches().get(id);
			if (stopwatch == null) {
				throw StopwatchCommand.ERROR_DOES_NOT_EXIST.create(id);
			}
			double elapsed = stopwatch.elapsedSeconds(Stopwatches.currentTime());
			String expectedRange = Assertion.getRawArgument(context, "range");

			return AssertResult.of(range.matches(elapsed) ? 1 : 0, negated -> Component.literal(!negated
					? String.format("Expected stopwatch %s to match %s, found %s", id, expectedRange, elapsed)
					: String.format("Did not expect stopwatch %s to match %s, found %s", id, expectedRange, elapsed)));
		});
	}
}
