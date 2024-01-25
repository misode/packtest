package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.DataResult;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagLoader;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/**
 * Catch tag errors and removes stacktrace.
 * Removes the duplicate data result error.
 */
@Mixin(TagLoader.class)
public class TagLoaderMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/DataResult;getOrThrow(ZLjava/util/function/Consumer;)Ljava/lang/Object;"))
    private static Object removeDuplicateError(DataResult<Object> dataResult, boolean allowPartial, Consumer<String> onError, Operation<TagFile> original) {
        return dataResult.getOrThrow(allowPartial, (error) -> {});
    }

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V"))
    private static void catchTagError(Logger logger, String message, Object[] args, Operation<Void> original) {
        String error = ((Exception)args[3]).getMessage().replaceFirst("^[A-Za-z0-9.]+Exception: ", "");
        String type = ((ResourceLocation)args[1]).getPath().replaceFirst("tags/", "").replaceFirst("s?/.*", "");
        LoadDiagnostics.error(type + " tag", ((ResourceLocation)args[0]).toString(), error);
        LOGGER.error("Couldn't read {} tag {} - {}", type, args[0], error);
    }

    @WrapOperation(method = "method_33175", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V"))
    private static void catchTagReferenceError(Logger logger, String message, Object id, Object refs, Operation<Void> original) {
        LoadDiagnostics.error("tag", ((ResourceLocation)id).toString(), "Missing references: " + refs);
        original.call(logger, message, id, refs);
    }
}
