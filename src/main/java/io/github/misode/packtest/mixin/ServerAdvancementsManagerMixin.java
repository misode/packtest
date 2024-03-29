package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerAdvancementManager;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Catch advancement errors.
 */
@Mixin(ServerAdvancementManager.class)
public class ServerAdvancementsManagerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @WrapOperation(method = "method_20723", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private void apply(Logger logger, String message, Object id, Object error, Operation<Void> original) {
        LoadDiagnostics.error(LOGGER, "advancement", ((ResourceLocation)id).toString(), (String)error);
    }
}
