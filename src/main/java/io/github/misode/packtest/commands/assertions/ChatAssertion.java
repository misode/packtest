package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import io.github.misode.packtest.PackTestExecutor;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ChatAssertion implements Assertion {
	private static final DynamicCommandExceptionType ERROR_INVALID_PATTERN = new DynamicCommandExceptionType(
			pattern -> Component.literal(String.format("Invalid pattern '%s'", pattern)));

	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("chat")
				.then(Commands.argument("pattern", StringArgumentType.string())
						.executes(ctx -> run(ctx, mode, false))
						.then(Commands.argument("players", EntityArgument.players())
								.executes(ctx -> run(ctx, mode, true)))));
	}

	private static int run(CommandContext<CommandSourceStack> context, Mode mode, boolean players) throws CommandSyntaxException {
		PackTestExecutor executor = PackTestExecutor.current();
		String patternString = StringArgumentType.getString(context, "pattern");
		Pattern pattern = compilePattern(patternString);

		return mode.check(() -> {
			Stream<String> messages = players
					? EntityArgument.getPlayers(context, "players").stream().flatMap(player -> executor.chatMessages(player.getUUID()))
					: executor.chatMessages();
			List<String> received = messages.toList();
			int count = (int)received.stream().filter(msg -> pattern.matcher(msg).find()).count();

			return AssertResult.of(count, negated -> Component.literal(!negated
					? String.format("Expected chat message matching %s, received %s", patternString, describeMessages(received))
					: String.format("Did not expect chat message matching %s, found %s", patternString, count)));
		});
	}

	private static String describeMessages(List<String> received) {
		if (received.isEmpty()) {
			return "nothing";
		}

		String sample = received.stream()
				.limit(5)
				.map(msg -> '"' + (msg.length() > 80 ? msg.substring(0, 80) + "…" : msg) + '"')
				.collect(Collectors.joining(", "));

		return received.size() > 5 ? sample + " and " + (received.size() - 5) + " more" : sample;
	}

	private static Pattern compilePattern(String pattern) throws CommandSyntaxException {
		try {
			return Pattern.compile(pattern);
		} catch (PatternSyntaxException e) {
			throw ERROR_INVALID_PATTERN.create(pattern);
		}
	}
}
