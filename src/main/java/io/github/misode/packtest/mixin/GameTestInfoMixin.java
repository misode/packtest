package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers the {@link GameTestHelper} for each test so custom commands can access it.
 * Prevents crash when test has already started.
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin {
    @Shadow
    public String getTestName() {
        throw new AssertionError("Nope.");
    }

    @ModifyExpressionValue(method = "startTest", at = @At(value="NEW", target = "(Lnet/minecraft/gametest/framework/GameTestInfo;)Lnet/minecraft/gametest/framework/GameTestHelper;"))
    private GameTestHelper createHelper(GameTestHelper helper) {
        PackTestLibrary.INSTANCE.registerTestHelper(this.getTestName(), helper);
        return helper;
    }

    @Inject(method = "finish", at = @At("HEAD"))
    private void finish(CallbackInfo ci) {
        PackTestLibrary.INSTANCE.unregisterTestHelper(this.getTestName());
    }

    @Inject(method = "startTest", cancellable = true, at = @At(value = "INVOKE", target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V"))
    private void startTest(CallbackInfo ci) {
        ci.cancel();
    }
}
