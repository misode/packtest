package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTestExecutor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.Collection;

class EntityAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("entity")
				.then(Commands.argument("entities", EntityArgument.entities())
						.executes(ctx -> run(ctx, mode, false))
						.then(Commands.literal("inside")
								.executes(ctx -> run(ctx, mode, true)))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode, boolean insideBounds) throws CommandSyntaxException {
		EntitySelector selector = context.getArgument("entities", EntitySelector.class);
		PackTestExecutor executor = PackTestExecutor.current();
		AABB bounds = executor.getBounds().inflate(1);

		return mode.check(() -> {
			Collection<? extends Entity> entities = selector.findEntities(context.getSource());
			String expectedEntity = Assertion.getRawArgument(context, "entities");

			if (insideBounds) {
				int count = (int)entities.stream().filter(e -> bounds.contains(e.position())).count();
				return AssertResult.of(count, negated -> Component.literal(!negated
						? String.format("Expected entity matching %s inside test bounds", expectedEntity)
						: String.format("Did not expect entity matching %s inside test bounds, found %d", expectedEntity, count)));
			}
			int count = entities.size();
			return AssertResult.of(count, negated -> Component.literal(!negated
					? String.format("Expected entity matching %s", expectedEntity)
					: String.format("Did not expect entity matching %s, found %d", expectedEntity, count)));
		});
	}
}
