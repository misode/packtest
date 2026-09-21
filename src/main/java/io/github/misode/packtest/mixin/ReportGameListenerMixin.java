package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.LineNumberException;
import net.minecraft.gametest.framework.ReportGameListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Add command number to failure message in chat.
 */
@Mixin(ReportGameListener.class)
public class ReportGameListenerMixin {

    @ModifyArg(method = "visualizeFailedTest", at = @At(value = "INVOKE", target = "Lnet/minecraft/gametest/framework/ReportGameListener;say(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/ChatFormatting;Ljava/lang/String;)V"), index = 2)
    private static String visualizeFailedTest(String message, @Local(argsOnly = true, name = "error") Throwable error) {
        if (error instanceof LineNumberException e) {
            return message.replaceFirst(" failed!", " failed on command " + e.getLineNumber() + "!");
        } else {
            return message;
        }
    }
}
