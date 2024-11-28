package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.DataResult;
import io.github.misode.packtest.LoadDiagnostics;
import io.github.misode.packtest.PackTestFileToIdConverter;
import net.minecraft.resources.FileToIdConverter;
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

    @WrapOperation(method = "scanDirectory(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/resources/FileToIdConverter;Lcom/mojang/serialization/DynamicOps;Lcom/mojang/serialization/Codec;Ljava/util/Map;)V", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private static void resourceException(Logger logger, String message, Object[] args, Operation<Void> original, @Local(argsOnly = true) FileToIdConverter converter) {
        String directory = ((PackTestFileToIdConverter)converter).packtest$getPrefix();
        String type = directory.replace("_", " ").replace("/", " ");
        LoadDiagnostics.error(LOGGER, type, ((ResourceLocation)args[0]).toString(), (args[2]).toString());
    }
}
