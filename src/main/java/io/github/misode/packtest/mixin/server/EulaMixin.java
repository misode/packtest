package io.github.misode.packtest.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.PackTest;
import net.minecraft.server.Eula;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Removes warnings when it fails to load eula.txt.
 * Automatically agrees to the EULA and shows a message in the log.
 */
@Mixin(Eula.class)
public class EulaMixin {
    @Shadow @Final private boolean agreed;

    @WrapOperation(method = "readFile", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V", remap = false))
    private static void readFile(Logger logger, String message, Object path, Operation<Void> original) {
        if (!PackTest.isAutoEnabled()) {
            original.call(logger, message, path);
        }
    }

    @Inject(method = "hasAgreedToEULA", at = @At("HEAD"), cancellable = true)
    public void hasAgreedToEULA(CallbackInfoReturnable<Boolean> cir) {
        if (!this.agreed && PackTest.isAutoEnabled()) {
            PackTest.LOGGER.info("By using the auto test server you are indicating your agreement to the EULA (https://aka.ms/MinecraftEULA).");
            cir.setReturnValue(true);
        }
    }
}
