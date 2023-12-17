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

@Mixin(GameTestInfo.class)
public class GameTestInfoMixin {
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
}
