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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ArgProvider;
import net.minecraft.server.commands.data.DataAccessor;
import net.minecraft.server.commands.data.DataCommands;

import java.util.List;
import java.util.Objects;

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

			return AssertResult.of(path.countMatching(data), negated -> negated
					? Component.literal(String.format("Did not expect data at %s", path.asString()))
					: mismatch(path, data));
		});
	}

	private static Component mismatch(NbtPathArgument.NbtPath path, Tag data) {
		String raw = path.asString();
		List<Tag> found = List.of(data);
		String reached = "";

		for (NbtPathArgument.Node node : path.nodes) {
			List<Tag> next = node.get(found);
			if (next.isEmpty()) {
				return describe(raw, found, reached, node);
			}
			found = next;
			reached = raw.substring(0, path.nodeToOriginalPosition.getInt(node));
		}

		return describe(raw, found, reached);
	}

	private static Component describe(String raw, List<Tag> found, String at) {
		String value = found.size() == 1 ? found.getFirst().toString() : found.toString();

		return Component.literal(at.isEmpty()
				? String.format("Expected data at %s, found %s", raw, value)
				: String.format("Expected data at %s, found %s at %s", raw, value, at));
	}

	private static Component describe(String raw, List<Tag> found, String reached, NbtPathArgument.Node unmatched) {
		if (!(unmatched instanceof NbtPathArgument.MatchObjectNode filter)) {
			return describe(raw, found, reached);
		}

		List<Tag> filtered = found.stream()
				.map(tag -> tag instanceof CompoundTag compound ? compound.get(filter.name) : null)
				.filter(Objects::nonNull)
				.toList();

		return filtered.isEmpty()
				? describe(raw, found, reached)
				: describe(raw, filtered, reached.isEmpty() ? filter.name : reached + "." + filter.name);
	}
}
