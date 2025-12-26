package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import io.github.misode.packtest.PackTestArgumentSource;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stores the original text in the block predicate result
 */
@Mixin(BlockPredicateArgument.class)
public class BlockPredicateArgumentMixin {
    @Inject(method = "parse(Lnet/minecraft/core/HolderLookup;Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/blocks/BlockPredicateArgument$Result;", at = @At("HEAD"))
    private static void getCursor(HolderLookup<Block> holderLookup, StringReader stringReader, CallbackInfoReturnable<BlockPredicateArgument.Result> cir, @Share("cursor") LocalIntRef cursorRef) {
        cursorRef.set(stringReader.getCursor());
    }

    @ModifyReturnValue(method = "parse(Lnet/minecraft/core/HolderLookup;Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/blocks/BlockPredicateArgument$Result;", at = @At("RETURN"))
    private static BlockPredicateArgument.Result returnSelector(BlockPredicateArgument.Result result, @Local(argsOnly = true) StringReader reader, @Share("cursor") LocalIntRef cursorRef) {
        ((PackTestArgumentSource)result).packtest$setSource(reader.getRead().substring(cursorRef.get()));
        return result;
    }
}
