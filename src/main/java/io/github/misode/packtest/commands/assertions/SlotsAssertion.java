package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.SlotSourceArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.item.BlockItemAccessor;
import net.minecraft.server.commands.item.EntityItemAccessor;
import net.minecraft.server.commands.item.ItemAccessor;

class SlotsAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("slots")
				.then(Commands.literal("entity").then(Commands.argument("entities", EntityArgument.entities())
						.then(Commands.argument("slots", SlotSourceArgument.slotSource(context))
								.executes(ctx -> runForEntity(ctx, mode)))))
				.then(Commands.literal("block").then(Commands.argument("pos", BlockPosArgument.blockPos())
						.then(Commands.argument("slots", SlotSourceArgument.slotSource(context))
								.executes(ctx -> runForBlock(ctx, mode))))));
	}

	private static int runForEntity(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		EntitySelector selector = context.getArgument("entities", EntitySelector.class);

		return mode.check(() -> count(context, new EntityItemAccessor(selector.findEntities(context.getSource()))));
	}

	private static int runForBlock(CommandContext<CommandSourceStack> context, Mode mode) throws CommandSyntaxException {
		return mode.check(() -> count(context, new BlockItemAccessor(BlockPosArgument.getLoadedBlockPos(context, "pos"))));
	}

	private static AssertResult count(CommandContext<CommandSourceStack> context, ItemAccessor<?> accessor) throws CommandSyntaxException {
		SlotSourceArgument.Result slots = SlotSourceArgument.getSlotSource(context, "slots");
		int count = ExecuteCommand.countSlots(context.getSource(), accessor, slots);
		String expectedSlots = Assertion.getRawArgument(context, "slots");

		return AssertResult.of(count, negated -> Component.literal(!negated
				? String.format("Expected slots matching %s", expectedSlots)
				: String.format("Did not expect slots matching %s, found %d", expectedSlots, count)));
	}
}
