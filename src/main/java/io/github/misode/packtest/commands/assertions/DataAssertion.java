package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ArgProvider;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;

class DataAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		for (ArgProvider<DataAccessor> provider : DataCommands.SOURCE_PROVIDERS) {
			builder.then(provider.wrap(Commands.literal("data"), p -> p
					.then(Commands.argument("path", NbtPathArgument.nbtPath())
							.executes(ctx -> run(ctx, mode, provider)))));
		}
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode, ArgProvider<DataAccessor> provider) throws CommandSyntaxException {
		return mode.check(() -> {
			NbtPathArgument.NbtPath path = NbtPathArgument.getPath(context, "path");
			DataAccessor accessor = provider.access(context);
			CompoundTag data = accessor.getData();

			return AssertResult.of(path.countMatching(data), negated -> Component.literal(!negated
					? String.format("Expected data at %s, found %s", path.asString(), data)
					: String.format("Did not expect data at %s", path.asString())));
		});
	}
}
