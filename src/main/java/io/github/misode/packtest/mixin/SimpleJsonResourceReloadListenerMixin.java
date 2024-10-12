package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Catches loot table, predicate, item modifier, advancement, and recipe errors.
 * Improves error message.
 */
@Mixin(SimpleJsonResourceReloadListener.class)
public class SimpleJsonResourceReloadListenerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @WrapOperation(method = "method_63567", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private static void resourceParseError(Logger logger, String message, Object[] args, Operation<Void> original) {
        String resourcePath = ((ResourceLocation)args[1]).getPath();
        String type = resourcePath.substring(0, resourcePath.indexOf('/')).replace("_", " ").replace("/", " ");
        LoadDiagnostics.error(LOGGER, type, ((ResourceLocation)args[0]).toString(), ((DataResult.Error<?>)args[2]).message());
    }

    @WrapOperation(method = "scanDirectory", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private static void resourceException(Logger logger, String message, Object[] args, Operation<Void> original, @Local(ordinal = 0, argsOnly = true) String directory) {
        String type = directory.replace("_", " ").replace("/", " ");
        LoadDiagnostics.error(LOGGER, type, ((ResourceLocation)args[0]).toString(), (args[2]).toString());
    }
}
