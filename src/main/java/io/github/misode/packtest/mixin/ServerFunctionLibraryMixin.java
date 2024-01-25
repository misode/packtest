package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ServerFunctionLibrary;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Catch function errors and removes stacktrace.
 */
@Mixin(ServerFunctionLibrary.class)
public class ServerFunctionLibraryMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @WrapOperation(method = "method_29457", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private static void catchFunctionError(Logger logger, String message, Object id, Object e, Operation<Void> original) {
        String error = ((Exception)e).getMessage().replaceFirst("^[A-Za-z0-9.]+Exception: ", "");
        LoadDiagnostics.error(LOGGER, "function", ((ResourceLocation)id).toString(), error);
    }
}
