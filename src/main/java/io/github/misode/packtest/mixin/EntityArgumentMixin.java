package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.mojang.brigadier.StringReader;
import io.github.misode.packtest.PackTestArgumentSource;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stores the original text in the entity selector
 */
@Mixin(EntityArgument.class)
public class EntityArgumentMixin {

    @Inject(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/selector/EntitySelector;", at = @At("HEAD"))
    private void getCursor(StringReader stringReader, CallbackInfoReturnable<EntitySelector> cir, @Share("cursor") LocalIntRef cursorRef) {
        cursorRef.set(stringReader.getCursor());
    }

    @ModifyReturnValue(method = "parse(Lcom/mojang/brigadier/StringReader;)Lnet/minecraft/commands/arguments/selector/EntitySelector;", at = @At("RETURN"))
    private EntitySelector returnSelector(EntitySelector selector, @Local(argsOnly = true) StringReader reader, @Share("cursor") LocalIntRef cursorRef) {
        ((PackTestArgumentSource)selector).packtest$setSource(reader.getRead().substring(cursorRef.get()));
        return selector;
    }
}
