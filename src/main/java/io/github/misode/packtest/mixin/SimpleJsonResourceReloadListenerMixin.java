package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.DataResult;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
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

    @Shadow
    @Final
    private FileToIdConverter lister;

    @WrapOperation(method = "lambda$prepare$1", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private static void resourceParseError(Logger logger, String message, Object[] args, Operation<Void> original) {
        String resourcePath = ((Identifier)args[1]).getPath();
        String type = resourcePath.substring(0, resourcePath.indexOf('/')).replace("_", " ").replace("/", " ");
        LoadDiagnostics.error(LOGGER, type, ((Identifier)args[0]).toString(), ((DataResult.Error<?>)args[2]).message());
    }

    @WrapOperation(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/Map;", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private void resourceException(Logger logger, String message, Object[] args, Operation<Void> original) {
        String directory = this.lister.prefix();
        String type = directory.replace("_", " ").replace("/", " ");
        LoadDiagnostics.error(LOGGER, type, ((Identifier)args[0]).toString(), (args[2]).toString());
    }
}
