package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ContextChain;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.execution.ChainModifiers;
import net.minecraft.commands.execution.CustomCommandExecutor;
import net.minecraft.commands.execution.ExecutionControl;
import net.minecraft.commands.execution.Frame;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AssertCommand {
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PREDICATE = (ctx, suggestions) -> {
        LootDataManager lvt2 = ctx.getSource().getServer().getLootData();
        return SharedSuggestionProvider.suggestResource(lvt2.getKeys(LootDataType.PREDICATE), suggestions);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(literal("assert")
                .requires(ctx -> ctx.hasPermission(2))
                .then(literal("block")
                        .then(argument("pos", BlockPosArgument.blockPos())
                                .then(argument("block", BlockPredicateArgument.blockPredicate(buildContext))
                                        .executes(new AssertCustomExecutor(ctx -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(ctx, "pos");
                                            Predicate<BlockInWorld> predicate = BlockPredicateArgument.getBlockPredicate(ctx, "block");
                                            BlockInWorld found = new BlockInWorld(ctx.getSource().getLevel(), pos, true);
                                            if (predicate.test(found)) {
                                                return Optional.empty();
                                            }
                                            return Optional.of("Expected block, found " + BuiltInRegistries.BLOCK.getKey(found.getState().getBlock()));
                                        })))))
                .then(literal("entity")
                        .then(argument("entities", EntityArgument.entities())
                                .executes(new AssertCustomExecutor(ctx -> {
                                    Collection<? extends Entity> entities = EntityArgument.getOptionalEntities(ctx, "entities");
                                    if (!entities.isEmpty()) {
                                        return Optional.empty();
                                    }
                                    return Optional.of("Expected entity");
                                }))))
                .then(literal("predicate")
                        .then(argument("predicate", ResourceLocationArgument.id())
                                .suggests(SUGGEST_PREDICATE)
                                .executes(new AssertCustomExecutor(ctx -> {
                                    ResourceLocation id = ctx.getArgument("predicate", ResourceLocation.class);
                                    LootItemCondition predicate = ResourceLocationArgument.getPredicate(ctx, "predicate");
                                    CommandSourceStack sourceStack = ctx.getSource();
                                    LootParams lootParams = new LootParams.Builder(sourceStack.getLevel())
                                            .withParameter(LootContextParams.ORIGIN, sourceStack.getPosition())
                                            .withOptionalParameter(LootContextParams.THIS_ENTITY, sourceStack.getEntity())
                                            .create(LootContextParamSets.COMMAND);
                                    LootContext lootContext = new LootContext.Builder(lootParams).create(Optional.empty());
                                    lootContext.pushVisitedElement(LootContext.createVisitedEntry(predicate));
                                    if (predicate.test(lootContext)) {
                                        return Optional.empty();
                                    }
                                    return Optional.of("Predicate " + id + " failed");
                                }))))
        );
    }

    static class AssertCustomExecutor implements CustomCommandExecutor.CommandAdapter<CommandSourceStack> {
        private final AssertPredicate predicate;

        public AssertCustomExecutor(AssertPredicate predicate) {
            this.predicate = predicate;
        }

        public void run(CommandSourceStack sourceStack, ContextChain<CommandSourceStack> chain, ChainModifiers modifiers, ExecutionControl<CommandSourceStack> execution) {
            CommandContext<CommandSourceStack> ctx = chain.getTopContext().copyFor(sourceStack);
            this.predicate.apply(ctx).ifPresent(message -> {
                PackTestLibrary.INSTANCE.getHelperAt(sourceStack)
                        .ifPresent(helper -> helper.fail(message));
                sourceStack.callback().onFailure();
                Frame frame = execution.currentFrame();
                frame.returnFailure();
                frame.discard();
            });
        }
    }

    @FunctionalInterface
    interface AssertPredicate extends Function<CommandContext<CommandSourceStack>, Optional<String>> {
        @Override
        default Optional<String> apply(final CommandContext<CommandSourceStack> sourceStack) {
            try {
                return applyThrows(sourceStack);
            } catch (final CommandSyntaxException e) {
                return Optional.of(e.getMessage());
            }
        }

        Optional<String> applyThrows(CommandContext<CommandSourceStack> elem) throws CommandSyntaxException;
    }
}
