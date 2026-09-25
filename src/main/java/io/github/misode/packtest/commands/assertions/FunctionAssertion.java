package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.misode.packtest.PackTestExecutor;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.item.FunctionArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.tasks.CallFunction;
import net.minecraft.commands.execution.tasks.FallthroughTask;
import net.minecraft.commands.execution.tasks.IsolatedCall;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.ExecuteCommand;
import net.minecraft.server.commands.FunctionCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class FunctionAssertion implements Assertion {
	@Override
	public void attach(
			LiteralArgumentBuilder<CommandSourceStack> builder,
			CommandDispatcher<CommandSourceStack> dispatcher,
			CommandBuildContext context,
			Mode mode) {
		builder.then(Commands.literal("function")
				.then(Commands.argument("function", FunctionArgument.functions())
						.suggests(FunctionCommand.SUGGEST_FUNCTION)
						.executes(new AssertFunction(mode))));
	}

	private record AssertFunction(Mode mode) implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
		@Override
		public void run(
				CommandSourceStack sender,
				ContextChain<CommandSourceStack> currentStep,
				ChainModifiers modifiers,
				ExecutionControl<CommandSourceStack> output) {
			try {
				this.runGuarded(sender, currentStep, output);
			} catch (CommandSyntaxException e) {
				sender.handleError(e, modifiers.isForked(), output.tracer());
				sender.callback().onFailure();
			}
		}

		private void runGuarded(
				CommandSourceStack sender,
				ContextChain<CommandSourceStack> currentStep,
				ExecutionControl<CommandSourceStack> output) throws CommandSyntaxException {
			PackTestExecutor test = PackTestExecutor.current();
			CommandContext<CommandSourceStack> context = currentStep.getTopContext().copyFor(sender);
			CommandSourceStack functionContext = FunctionCommand.modifySenderForExecution(sender.clearCallbacks());
			String name = Assertion.getRawArgument(context, "function");
			List<InstantiatedFunction<CommandSourceStack>> functions = instantiate(context, sender.dispatcher());

			queueFunctions(output, functionContext, functions, name, result ->
					this.mode.check(test, result, () -> pollFunctions(functionContext, functions, name)));
		}

		private static void queueFunctions(
				ExecutionControl<CommandSourceStack> output,
				CommandSourceStack functionContext,
				List<InstantiatedFunction<CommandSourceStack>> functions,
				String name,
				Consumer<AssertResult> onResult) {
            AtomicInteger passing = new AtomicInteger(0);
            AtomicInteger found = new AtomicInteger(0);

			output.queueNext(new IsolatedCall<>(control -> {
				for (InstantiatedFunction<CommandSourceStack> function : functions) {
					control.queueNext(new CallFunction<>(function, control.currentFrame().returnValueConsumer(), true).bind(functionContext));
				}
				control.queueNext(FallthroughTask.instance());
			}, (_, result) -> {
				found.set(result);
				if (result != 0) {
					passing.getAndAdd(1);
				}
			}));

			output.queueNext(new IsolatedCall<>(control -> {
				onResult.accept(functionResult(name, passing.get(), found.get()));
				control.queueNext(FallthroughTask.instance());
			}, CommandResultCallback.EMPTY));
		}

		private static AssertResult pollFunctions(
				CommandSourceStack functionContext,
				List<InstantiatedFunction<CommandSourceStack>> functions,
				String name) {
			int passing = 0;
			int found = 0;

			for (InstantiatedFunction<CommandSourceStack> function : functions) {
				AtomicInteger value = new AtomicInteger(0);

				CommandSourceStack capturing = functionContext.withCallback((_, result) -> value.set(result));
				Commands.executeCommandInContext(capturing, ctx -> ExecutionContext.queueInitialFunctionCall(ctx, function, capturing, CommandResultCallback.EMPTY));

				found = value.get();
				if (value.get() != 0) {
					passing++;
				}
			}

			return functionResult(name, passing, found);
		}

		private static List<InstantiatedFunction<CommandSourceStack>> instantiate(
				CommandContext<CommandSourceStack> context,
				CommandDispatcher<CommandSourceStack> dispatcher) throws CommandSyntaxException {
			List<InstantiatedFunction<CommandSourceStack>> functions = new ArrayList<>();

			for (CommandFunction<CommandSourceStack> function : FunctionArgument.getFunctions(context, "function")) {
				try {
					functions.add(function.instantiate(null, dispatcher));
				} catch (FunctionInstantiationException e) {
					throw ExecuteCommand.ERROR_FUNCTION_CONDITION_INSTANTATION_FAILURE.create(function.id(), e.messageComponent());
				}
			}

			return functions;
		}

        private static AssertResult functionResult(String name, int passing, int found) {
            return AssertResult.of(passing, negated -> Component.literal(!negated
                    ? String.format("Expected function %s to return nonzero, got %s", name, found)
                    : String.format("Did not expect function %s to return nonzero, got %s", name, found)));
        }
	}
}
