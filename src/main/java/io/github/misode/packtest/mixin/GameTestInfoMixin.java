package io.github.misode.packtest.mixin;

import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents crash when test has already started.
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin {

    @Inject(method = "startTest", cancellable = true, at = @At(value = "INVOKE", target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V"))
    private void startTest(CallbackInfo ci) {
        ci.cancel();
    }
}
