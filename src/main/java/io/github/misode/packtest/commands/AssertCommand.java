package io.github.misode.packtest.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.misode.packtest.PackTestArgumentSource;
import io.github.misode.packtest.PackTestSourceStack;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AssertCommand {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (ctx, suggestions) -> {
        LootDataManager lootData = ctx.getSource().getServer().getLootData();
        return SharedSuggestionProvider.suggestResource(lootData.getKeys(LootDataType.PREDICATE), suggestions);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> assertBuilder = literal("assert")
                .requires(ctx -> ctx.hasPermission(2));
        addConditions(assertBuilder, buildContext, predicate -> new AssertCustomExecutor(true, predicate));
        LiteralArgumentBuilder<CommandSourceStack> notBuilder = literal("not");
        addConditions(notBuilder, buildContext, predicate -> new AssertCustomExecutor(false, predicate));
        assertBuilder = assertBuilder.then(notBuilder);
        dispatcher.register(assertBuilder);
    }

    public static LiteralArgumentBuilder<CommandSourceStack> addConditions(LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext buildContext, Function<AssertPredicate, Command<CommandSourceStack>> expect) {
        builder
                .then(literal("block")
                        .then(argument("pos", BlockPosArgument.blockPos())
                                .then(argument("block", BlockPredicateArgument.blockPredicate(buildContext))
                                        .executes(expect.apply(AssertCommand::assertBlock)))))
                .then(literal("data"))
                .then(literal("entity")
                        .then(argument("entities", EntityArgument.entities())
                                .executes(expect.apply(AssertCommand::assertEntity))))
                .then(literal("predicate")
                        .then(argument("predicate", ResourceLocationArgument.id())
                                .suggests(SUGGEST_PREDICATE)
                                .executes(expect.apply(AssertCommand::assertPredicate))))
                .then(literal("score")
                        .then(argument("target", ScoreHolderArgument.scoreHolder())
                                .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                .then(argument("targetObjective", ObjectiveArgument.objective())
                                        .then(addScoreCheck("=", Integer::equals, expect))
                                        .then(addScoreCheck("<", (a, b) -> a < b, expect))
                                        .then(addScoreCheck("<=", (a, b) -> a <= b, expect))
                                        .then(addScoreCheck(">", (a, b) -> a > b, expect))
                                        .then(addScoreCheck("<=", (a, b) -> a <= b, expect))
                                        .then(literal("matches")
                                                .then(argument("range", RangeArgument.intRange())
                                                        .executes(expect.apply(AssertCommand::assertScoreRange))))
                                )));

        for(DataCommands.DataProvider dataProvider : DataCommands.SOURCE_PROVIDERS) {
            builder.then(dataProvider.wrap(literal("data"),
                    dataBuilder -> dataBuilder.then(argument("path", NbtPathArgument.nbtPath())
                            .executes(expect.apply(ctx -> assertData(ctx, dataProvider))))));
        }

        return builder;
    }

    private static AssertResult assertBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        var blockPredicate = ctx.getArgument("block", BlockPredicateArgument.Result.class);
        String source = ((PackTestArgumentSource)blockPredicate).packtest$getSource();
        BlockInWorld found = new BlockInWorld(ctx.getSource().getLevel(), pos, true);
        String foundId = BuiltInRegistries.BLOCK.getKey(found.getState().getBlock()).toString();
        if (blockPredicate.test(found)) {
            return ok(source, foundId);
        }
        return err(source, foundId);
    }

    private static AssertResult assertEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        EntitySelector selector = ctx.getArgument("entities", EntitySelector.class);
        String source = ((PackTestArgumentSource)selector).packtest$getSource();
        Collection<? extends Entity> entities = selector.findEntities(ctx.getSource());
        if (!entities.isEmpty()) {
            Entity firstEntity = entities.stream().findFirst().orElseThrow();
            String firstName = Objects.requireNonNull(firstEntity.getDisplayName()).getString();
            return ok(source, firstName + (entities.size() <= 1 ? "" : " and " + (entities.size() - 1) + " more"));
        }
        return err(source);
    }

    private static AssertResult assertPredicate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ResourceLocation id = ctx.getArgument("predicate", ResourceLocation.class);
        LootItemCondition predicate = ResourceLocationArgument.getPredicate(ctx, "predicate");
        CommandSourceStack sourceStack = ctx.getSource();
        LootParams lootParams = new LootParams.Builder(sourceStack.getLevel())
                .withParameter(LootContextParams.ORIGIN, sourceStack.getPosition())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, sourceStack.getEntity())
                .create(LootContextParamSets.COMMAND);
        LootContext lootContext = new LootContext.Builder(lootParams).create(Optional.empty());
        lootContext.pushVisitedElement(LootContext.createVisitedEntry(predicate));
        String expected = "predicate " + id + " to pass";
        if (predicate.test(lootContext)) {
            return ok(expected);
        }
        return err(expected);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> addScoreCheck(String op, BiPredicate<Integer, Integer> predicate, Function<AssertPredicate, Command<CommandSourceStack>> expect) {
        return literal(op)
                .then(argument("source", ScoreHolderArgument.scoreHolder())
                        .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                        .then(argument("sourceObjective", ObjectiveArgument.objective())
                                .executes(expect.apply(ctx -> assertScores(ctx, op, predicate)))));
    }

    private static AssertResult assertScores(CommandContext<CommandSourceStack> ctx, String op, BiPredicate<Integer, Integer> predicate) throws  CommandSyntaxException {
        ScoreHolder targetHolder = ScoreHolderArgument.getName(ctx, "target");
        Objective targetObj = ObjectiveArgument.getObjective(ctx, "targetObjective");
        ScoreHolder sourceHolder = ScoreHolderArgument.getName(ctx, "source");
        Objective sourceObj = ObjectiveArgument.getObjective(ctx, "sourceObjective");
        Scoreboard scoreboard = ctx.getSource().getServer().getScoreboard();
        ReadOnlyScoreInfo targetVal = scoreboard.getPlayerScoreInfo(targetHolder, targetObj);
        ReadOnlyScoreInfo sourceVal = scoreboard.getPlayerScoreInfo(sourceHolder, sourceObj);
        String targetName = targetHolder.getFeedbackDisplayName().getString();
        String sourceName = sourceHolder.getFeedbackDisplayName().getString();
        if (targetVal == null) {
            return err(targetName + " to have a score on " + targetObj.getName());
        }
        if (sourceVal == null) {
            return err(sourceName + " to have a score on " + sourceObj.getName());
        }
        String expected = targetName + " " + targetObj.getName() + " " + op + " " + sourceName + " " + sourceObj.getName();
        String got = targetVal.value() + " " + op + " " + sourceVal.value();
        return result(predicate.test(targetVal.value(), sourceVal.value()), expected, got);
    }

    private static AssertResult assertScoreRange(CommandContext<CommandSourceStack> ctx) throws  CommandSyntaxException {
        MinMaxBounds.Ints range = RangeArgument.Ints.getRange(ctx, "range");
        ScoreHolder holder = ScoreHolderArgument.getName(ctx, "target");
        Objective obj = ObjectiveArgument.getObjective(ctx, "targetObjective");
        Scoreboard scoreboard = ctx.getSource().getServer().getScoreboard();
        ReadOnlyScoreInfo val = scoreboard.getPlayerScoreInfo(holder, obj);
        String name = holder.getFeedbackDisplayName().getString();
        if (val == null) {
            return err(name + " to have a score on " + obj.getName());
        }
        String expected = name + " " + obj.getName() + " to match " + formatRange(range);
        return result(range.matches(val.value()), expected, Integer.toString(val.value()));
    }

    private static String formatRange(MinMaxBounds<?> range) {
        if (range.min().equals(range.max()) && range.min().isPresent()) {
            return range.min().get().toString();
        }
        StringBuilder builder = new StringBuilder();
        range.min().ifPresent(builder::append);
        builder.append("..");
        range.max().ifPresent(builder::append);
        return builder.toString();
    }

    private static AssertResult assertData(CommandContext<CommandSourceStack> ctx, DataCommands.DataProvider dataProvider) throws CommandSyntaxException {
        NbtPathArgument.NbtPath path = NbtPathArgument.getPath(ctx, "path");
        Tag data = dataProvider.access(ctx).getData();
        return result(path.countMatching(data) > 0, path.asString() + " to match", data.getAsString());
    }

    static class AssertCustomExecutor implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        private final boolean expectOk;
        private final AssertPredicate predicate;

        public AssertCustomExecutor(boolean expectOk, AssertPredicate predicate) {
            this.expectOk = expectOk;
            this.predicate = predicate;
        }

        public void run(CommandSourceStack sourceStack, ContextChain<CommandSourceStack> chain, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> execution) {
            CommandContext<CommandSourceStack> ctx = chain.getTopContext().copyFor(sourceStack);
            var result = this.predicate.apply(ctx);
            result.get(this.expectOk).ifPresentOrElse(message -> {
                GameTestHelper helper = ((PackTestSourceStack)sourceStack).packtest$getHelper();
                if (helper != null) {
                    helper.fail(message);
                }
                sourceStack.callback().onFailure();
                Frame frame = execution.currentFrame();
                frame.returnFailure();
                frame.discard();
            }, () -> sourceStack.callback().onSuccess(1));
        }
    }

    @FunctionalInterface
    public interface AssertPredicate extends Function<CommandContext<CommandSourceStack>, AssertResult> {
        @Override
        default AssertResult apply(final CommandContext<CommandSourceStack> sourceStack) {
            try {
                return applyThrows(sourceStack);
            } catch (final CommandSyntaxException e) {
                return (expectOk) -> Optional.of(e.getMessage());
            }
        }

        AssertResult applyThrows(CommandContext<CommandSourceStack> elem) throws CommandSyntaxException;
    }

    private static AssertResult err(String expected) {
        return new ExpectedGot(false, expected, null);
    }
    
    private static AssertResult err(String expected, String got) {
        return new ExpectedGot(false, expected, got);
    }
    
    private static AssertResult ok(String match) {
        return new ExpectedGot(true, match, null);
    }

    private static AssertResult ok(String match, String got) {
        return new ExpectedGot(true, match, got);
    }

    private static AssertResult result(boolean ok, String expected, String got) {
        return new ExpectedGot(ok, expected, got);
    }

    public interface AssertResult {
        Optional<String> get(boolean expectOk);
    }

    record ExpectedGot(boolean ok, String expected, String got) implements AssertResult {
        public Optional<String> get(boolean expectOk) {
            if (expectOk && !ok) {
                if (got == null) {
                    return Optional.of("Expected " + expected);
                }
                return Optional.of("Expected " + expected + ", got " + got);
            }
            if (!expectOk && ok) {
                if (got == null) {
                    return Optional.of("Did not expect " + expected);
                }
                return Optional.of("Did not expect " + expected + ", but got " + got);
            }
            return Optional.empty();
        }
    }
}
