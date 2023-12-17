package io.github.misode.packtest.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerTickRateManager;tick()V", shift = At.Shift.AFTER))
    private void tickWorlds(BooleanSupplier shouldKeepTicking, CallbackInfo callbackInfo) {
        if (!SharedConstants.IS_RUNNING_IN_IDE) {
            GameTestTicker.SINGLETON.tick();
        }
    }
}
