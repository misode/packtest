package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.repository.Pack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Pack.class)
public class PackMixin {
    @WrapOperation(method = "readPackMetadata", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Throwable;)V"))
    private static void catchPackParseError(Logger logger, String message, Throwable throwable, Operation<Void> original, @Local(argsOnly = true, name = "location") PackLocationInfo location) {
        LoadDiagnostics.error(logger, "pack.mcmeta", location.id(), throwable.getMessage());
    }

    @WrapOperation(method = "readPackMetadata", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private static void catchPackValidationError(Logger logger, String message, Object location, Object throwable, Operation<Void> original) {
        LoadDiagnostics.error(logger, "pack.mcmeta", location.toString(), ((Throwable)throwable).getMessage());
    }
}
