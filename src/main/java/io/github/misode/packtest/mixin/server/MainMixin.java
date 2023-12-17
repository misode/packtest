package io.github.misode.packtest.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.misode.packtest.PackTest;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Automatically agree to the EULA when auto is enabled
 */
@Mixin(Main.class)
public class MainMixin {
    @ModifyExpressionValue(method = "main", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/Eula;hasAgreedToEULA()Z"))
    private static boolean hasAgreedToEULA(boolean original) {
        return PackTest.isAutoEnabled() || original;
    }
}
