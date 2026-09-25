package io.github.misode.packtest.commands.assertions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

public class Assertions {
    private static final List<Assertion> ASSERTIONS = List.of(
            new BiomeAssertion(),
            new BlockAssertion(),
            new BlocksAssertion(),
            new ChatAssertion(),
            new DataAssertion(),
            new DimensionAssertion(),
            new EntityAssertion(),
            new FunctionAssertion(),
            new ItemsAssertion(),
            new LoadedAssertion(),
            new PredicateAssertion(),
            new RunAssertion(),
            new ScoreAssertion(),
            new SlotsAssertion(),
            new StopwatchAssertion());

    public static LiteralArgumentBuilder<CommandSourceStack> build(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandBuildContext context,
            Assertion.Mode mode,
            LiteralArgumentBuilder<CommandSourceStack> builder) {
        for (Assertion assertion : ASSERTIONS) {
            assertion.attach(builder, dispatcher, context, mode);
        }
        return builder;
    }
}
