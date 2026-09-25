package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceOrIdArgument;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

class PredicateAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("predicate")
				.then(Commands.argument("predicate", ResourceOrIdArgument.lootPredicate(context))
						.executes(ctx -> run(ctx, mode))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		CommandSourceStack source = context.getSource();

		return mode.check(() -> {
			Holder<LootItemCondition> predicate = ResourceOrIdArgument.getLootPredicate(context, "predicate");

			return AssertResult.of(ExecuteCommand.checkCustomPredicate(source, predicate) ? 1 : 0, negated -> Component.literal(!negated
					? String.format("Expected predicate %s to pass", predicate.getRegisteredName())
					: String.format("Did not expect predicate %s to pass", predicate.getRegisteredName())));
		});
	}
}
