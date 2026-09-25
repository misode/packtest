package io.github.misode.packtest.commands.assertions;

import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * Result of an assertion check: {@code count > 0} means the condition is met.
 */
public record AssertResult(int count, boolean errored, Function<Boolean, Component> message) {
	public static AssertResult of(int count, Function<Boolean, Component> message) {
		return new AssertResult(count, false, message);
	}

	public static AssertResult error(Component message) {
		return new AssertResult(0, true, _ -> message);
	}
}
