package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import io.github.misode.packtest.PackTestItemPredicate;
import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemPredicateArgument.class)
public class ItemPredicateArgumentMixin {

    @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/item/ItemPredicateArgument$Result;", at = @At("HEAD"))
    private void getCursor(StringReader stringReader, CallbackInfoReturnable<ItemPredicateArgument.Result> cir, @Share("cursor") LocalIntRef cursorRef) {
        cursorRef.set(stringReader.getCursor());
    }

    @ModifyReturnValue(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/item/ItemPredicateArgument$Result;", at = @At("RETURN"))
    private ItemPredicateArgument.Result returnPredicate(ItemPredicateArgument.Result predicate, @Local StringReader reader, @Share("cursor") LocalIntRef cursorRef) {
        return new PackTestItemPredicate(predicate, reader.getRead().substring(cursorRef.get()));
    }
}
