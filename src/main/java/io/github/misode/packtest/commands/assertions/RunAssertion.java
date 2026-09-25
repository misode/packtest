package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTestExecutor;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandResultCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.RangeArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomModifierExecutor;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.BuildContexts;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.execution.tasks.IsolatedCall;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class RunAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(buildRun(dispatcher, mode, false));
		builder.then(Commands.literal("result")
				.then(Commands.argument("range", RangeArgument.intRange())
						.then(buildRun(dispatcher, mode, true))));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> buildRun(CommandDispatcher<CommandSourceStack> dispatcher, Mode mode, boolean ranged) {
		return Commands.literal("run").fork(dispatcher.getRoot(), new AssertRun(mode, ranged));
	}

	private record AssertRun(Mode mode, boolean ranged) implements CustomModifierExecutor.ModifierAdapter<CommandSourceStack> {
		@Override
		public void apply(
				CommandSourceStack originalSource,
				List<CommandSourceStack> sources,
				ContextChain<CommandSourceStack> currentStep,
				ChainModifiers modifiers,
				ExecutionControl<CommandSourceStack> output) {
			if (sources.isEmpty()) {
				return;
			}

			try {
				PackTestExecutor test = PackTestExecutor.current();
				CommandContext<CommandSourceStack> context = currentStep.getTopContext().copyFor(originalSource);
				MinMaxBounds.Ints range = this.ranged ? RangeArgument.Ints.getRange(context, "range") : null;
				String rawRange = this.ranged ? Assertion.getRawArgument(context, "range") : null;
				String input = currentStep.getTopContext().getInput();
				ContextChain<CommandSourceStack> tail = currentStep.nextStage();
				List<CommandSourceStack> captured = List.copyOf(sources);

				queueTail(output, input, tail, modifiers, originalSource, captured, range, rawRange, result ->
						this.mode.check(test, result, () -> pollTail(input, tail, captured, range, rawRange)));
			} catch (CommandSyntaxException e) {
				originalSource.handleError(e, modifiers.isForked(), output.tracer());
			}
		}

		private static void queueTail(
				ExecutionControl<CommandSourceStack> output,
				String input,
				ContextChain<CommandSourceStack> tail,
				ChainModifiers modifiers,
				CommandSourceStack originalSource,
				List<CommandSourceStack> sources,
				MinMaxBounds.@Nullable Ints range,
				@Nullable String rawRange,
				Consumer<AssertResult> onResult) {
			AtomicInteger fires = new AtomicInteger(0);
			AtomicInteger misses = new AtomicInteger(0);
			AtomicInteger found = new AtomicInteger(0);
			List<CommandSourceStack> wrapped = sources.stream()
					.map(source -> counting(source, range, fires, misses, found))
					.toList();

			output.queueNext(new IsolatedCall<>(control -> {
				control.queueNext(new BuildContexts.Continuation<>(input, tail, modifiers, originalSource, wrapped));
				control.queueNext(FallthroughTask.instance());
			}, CommandResultCallback.EMPTY));

			output.queueNext(new IsolatedCall<>(control -> {
				onResult.accept(runResult(rawRange, fires.get(), misses.get(), found.get()));
				control.queueNext(FallthroughTask.instance());
			}, CommandResultCallback.EMPTY));
		}

		private static AssertResult pollTail(
				String input,
				ContextChain<CommandSourceStack> tail,
				List<CommandSourceStack> sources,
				MinMaxBounds.@Nullable Ints range,
				@Nullable String rawRange) {
			AtomicInteger fires = new AtomicInteger(0);
			AtomicInteger misses = new AtomicInteger(0);
			AtomicInteger found = new AtomicInteger(0);

			for (CommandSourceStack source : sources) {
				CommandSourceStack capturing = counting(source, range, fires, misses, found);
				Commands.executeCommandInContext(capturing, ctx ->
						ExecutionContext.queueInitialCommandExecution(ctx, input, tail, capturing, CommandResultCallback.EMPTY));
			}

			return runResult(rawRange, fires.get(), misses.get(), found.get());
		}

		private static CommandSourceStack counting(
				CommandSourceStack source,
				MinMaxBounds.@Nullable Ints range,
				AtomicInteger fires,
				AtomicInteger misses,
				AtomicInteger found) {
			return source.withCallback((success, result) -> {
				fires.getAndIncrement();
				found.set(result);

				if (!matches(range, success, result)) {
					misses.getAndIncrement();
				}
			}, CommandResultCallback::chain);
		}

		private static boolean matches(MinMaxBounds.@Nullable Ints range, boolean success, int result) {
			return range == null ? success : range.matches(result);
		}

		private static AssertResult runResult(@Nullable String rawRange, int fires, int misses, int found) {
			int satisfied = fires > 0 && misses == 0 ? 1 : 0;
			if (rawRange == null) {
				return AssertResult.of(satisfied, negated -> Component.literal(!negated
						? "Expected command to succeed"
						: "Did not expect command to succeed"));
			}
			return AssertResult.of(satisfied, negated -> Component.literal(!negated
					? String.format("Expected result matching %s, got %s", rawRange, found)
					: String.format("Did not expect result matching %s, got %s", rawRange, found)));
		}
	}
}
