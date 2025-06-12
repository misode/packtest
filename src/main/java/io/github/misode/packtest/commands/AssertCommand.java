package io.github.misode.packtest.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.misode.packtest.*;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.DataCommands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.SlotRange;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AssertCommand {
    private static final SimpleCommandExceptionType ERROR_NO_HELPER = new SimpleCommandExceptionType(
            Component.literal("Not inside a test")
    );
    private static final Dynamic3CommandExceptionType ERROR_SOURCE_NOT_A_CONTAINER = new Dynamic3CommandExceptionType(
            (x, y, z) -> Component.translatableEscape("commands.item.source.not_a_container", x, y, z)
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        LiteralArgumentBuilder<CommandSourceStack> assertBuilder = literal("assert")
                .requires(ctx -> ctx.hasPermission(2));
        addConditions(assertBuilder, buildContext, predicate -> new AssertCustomExecutor(true, predicate));
        LiteralArgumentBuilder<CommandSourceStack> notBuilder = literal("not");
        addConditions(notBuilder, buildContext, predicate -> new AssertCustomExecutor(false, predicate));
        assertBuilder = assertBuilder.then(notBuilder);
        dispatcher.register(assertBuilder);
    }

    public static void addConditions(LiteralArgumentBuilder<CommandSourceStack> builder, CommandBuildContext buildContext, Function<AssertPredicate, Command<CommandSourceStack>> expect) {
        builder
                .then(literal("block")
                        .then(argument("pos", BlockPosArgument.blockPos())
                                .then(argument("block", BlockPredicateArgument.blockPredicate(buildContext))
                                        .executes(expect.apply(AssertCommand::assertBlock)))))
                .then(literal("data"))
                .then(literal("entity")
                        .then(argument("entities", EntityArgument.entities())
                                .executes(expect.apply(AssertCommand::assertEntity))
                                .then(literal("inside")
                                        .executes(expect.apply(AssertCommand::assertEntityInside)))))
                .then(literal("predicate")
                        .then(argument("predicate", ResourceOrIdArgument.lootPredicate(buildContext))
                                .executes(expect.apply(AssertCommand::assertPredicate))))
                .then(literal("items")
                        .then(literal("entity")
                                .then(argument("entities", EntityArgument.entities())
                                        .then(argument("slots", SlotsArgument.slots())
                                                .then(argument("item_predicate", ItemPredicateArgument.itemPredicate(buildContext))
                                                        .executes(expect.apply(AssertCommand::assertItemsEntity))))))
                        .then(literal("block")
                                .then(argument("pos", BlockPosArgument.blockPos())
                                        .then(argument("slots", SlotsArgument.slots())
                                                .then(argument("item_predicate", ItemPredicateArgument.itemPredicate(buildContext))
                                                        .executes(expect.apply(AssertCommand::assertItemsBlock)))))))
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
                                )))
                .then(literal("chat")
                        .then(argument("pattern", StringArgumentType.string())
                                .executes(expect.apply(AssertCommand::assertChatUnfiltered))
                                .then(argument("receivers", EntityArgument.players())
                                        .executes(expect.apply(AssertCommand::assertChatFiltered)))));

        for(DataCommands.DataProvider dataProvider : DataCommands.SOURCE_PROVIDERS) {
            builder.then(dataProvider.wrap(literal("data"),
                    dataBuilder -> dataBuilder.then(argument("path", NbtPathArgument.nbtPath())
                            .executes(expect.apply(ctx -> assertData(ctx, dataProvider))))));
        }
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
        List<? extends Entity> entities = selector.findEntities(ctx.getSource());
        if (!entities.isEmpty()) {
            Entity firstEntity = entities.stream().findFirst().orElseThrow();
            String firstName = Objects.requireNonNull(firstEntity.getDisplayName()).getString();
            return ok(source, firstName + (entities.size() == 1 ? "" : " and " + (entities.size() - 1) + " more"));
        }
        return err(source);
    }

    private static AssertResult assertEntityInside(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        EntitySelector selector = ctx.getArgument("entities", EntitySelector.class);
        String source = ((PackTestArgumentSource)selector).packtest$getSource();
        GameTestHelper helper = ((PackTestSourceStack)ctx.getSource()).packtest$getHelper();
        if (helper == null) {
            throw ERROR_NO_HELPER.create();
        }
        AABB bounds = helper.getBounds().inflate(1);
        List<? extends Entity> entities = selector.findEntities(ctx.getSource()).stream()
                .filter(entity -> bounds.contains(entity.position()))
                .toList();
        if (!entities.isEmpty()) {
            Entity firstEntity = entities.stream().findFirst().orElseThrow();
            String firstName = Objects.requireNonNull(firstEntity.getDisplayName()).getString();
            return ok(source + " inside test", firstName + (entities.size() == 1 ? "" : " and " + (entities.size() - 1) + " more"));
        }
        return err(source + " inside test");
    }

    private static AssertResult assertPredicate(CommandContext<CommandSourceStack> ctx) {
        Holder<LootItemCondition> predicate = ResourceOrIdArgument.getLootPredicate(ctx, "predicate");
        CommandSourceStack sourceStack = ctx.getSource();
        LootParams lootParams = new LootParams.Builder(sourceStack.getLevel())
                .withParameter(LootContextParams.ORIGIN, sourceStack.getPosition())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, sourceStack.getEntity())
                .create(LootContextParamSets.COMMAND);
        LootContext lootContext = new LootContext.Builder(lootParams).create(Optional.empty());
        lootContext.pushVisitedElement(LootContext.createVisitedEntry(predicate.value()));
        String expected = "predicate " + predicate.getRegisteredName() + " to pass";
        if (predicate.value().test(lootContext)) {
            return ok(expected);
        }
        return err(expected);
    }

    private static AssertResult assertItemsEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        EntitySelector selector = ctx.getArgument("entities", EntitySelector.class);
        String selectorSource = ((PackTestArgumentSource)selector).packtest$getSource();
        List<? extends Entity> entities = selector.findEntities(ctx.getSource());
        SlotRange slotRange = SlotsArgument.getSlots(ctx, "slots");
        ItemPredicateArgument.Result itemPredicate = ItemPredicateArgument.getItemPredicate(ctx, "item_predicate");
        int count = 0;
        for(Entity entity : entities) {
            IntList slots = slotRange.slots();
            for(int i = 0; i < slots.size(); ++i) {
                ItemStack itemStack = entity.getSlot(slots.getInt(i)).get();
                if (itemPredicate.test(itemStack)) {
                    count += itemStack.getCount();
                }
            }
        }
        String expected = selectorSource + " to have items"; // TODO: add item predicate context
        if (count > 0) {
            return ok(expected);
        }
        return err(expected);
    }

    private static AssertResult assertItemsBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
        SlotRange slotRange = SlotsArgument.getSlots(ctx, "slots");
        ItemPredicateArgument.Result itemPredicate = ItemPredicateArgument.getItemPredicate(ctx, "item_predicate");
        BlockEntity blockEntity = ctx.getSource().getLevel().getBlockEntity(pos);
        if (blockEntity instanceof Container container) {
            int count = 0;
            int lvt6 = container.getContainerSize();
            IntList slots = slotRange.slots();
            for(int i = 0; i < slots.size(); ++i) {
                int slot = slots.getInt(i);
                if (slot >= 0 && slot < lvt6) {
                    ItemStack itemStack = container.getItem(slot);
                    if (itemPredicate.test(itemStack)) {
                        count += itemStack.getCount();
                    }
                }
            }
            String expected = "block to have items"; // TODO: add item predicate context
            if (count > 0) {
                return ok(expected);
            }
            return err(expected);
        } else {
            throw ERROR_SOURCE_NOT_A_CONTAINER.create(pos.getX(), pos.getY(), pos.getZ());
        }
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
        return result(path.countMatching(data) > 0, path.asString() + " to match", data.toString());
    }

    private static AssertResult assertChatUnfiltered(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return assertChat(ctx, m -> true);
    }

    private static AssertResult assertChatFiltered(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Collection<ServerPlayer> receivers = EntityArgument.getPlayers(ctx, "receivers");
        List<String> receiverNames = receivers.stream().map(p -> p.getName().getString()).toList();
        return assertChat(ctx, m -> receiverNames.contains(m.player()));
    }

    private static AssertResult assertChat(CommandContext<CommandSourceStack> ctx, Predicate<ChatListener.Message> filter) throws CommandSyntaxException {
        String pattern = StringArgumentType.getString(ctx, "pattern");
        GameTestHelper helper = ((PackTestSourceStack)ctx.getSource()).packtest$getHelper();
        if (helper == null) {
            throw ERROR_NO_HELPER.create();
        }
        ChatListener chatListener = ((PackTestInfo)((PackTestHelper)helper).packtest$getInfo()).packtest$getChatListener();
        Predicate<String> predicate = Pattern.compile(pattern).asPredicate();
        List<String> matching = chatListener.filter(m -> filter.test(m) && predicate.test(m.content()));
        List<String> all = matching.isEmpty() ? chatListener.filter(filter) : matching;
        String got = all.isEmpty() ? "no messages"
                : all.size() == 1 ? all.getFirst()
                : all.getLast() + " and " + (all.size() - 1) + " more";
        return result(!matching.isEmpty(), pattern + " in chat", got);
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
                    helper.fail(Component.literal(message));
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

    public static AssertResult err(String expected) {
        return new ExpectedGot(false, expected, null);
    }

    public static AssertResult err(String expected, String got) {
        return new ExpectedGot(false, expected, got);
    }

    public static AssertResult ok(String match) {
        return new ExpectedGot(true, match, null);
    }

    public static AssertResult ok(String match, String got) {
        return new ExpectedGot(true, match, got);
    }

    public static AssertResult result(boolean ok, String expected, String got) {
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
                return Optional.of("Expected " + expected + ", but got " + got);
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
