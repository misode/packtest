package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.LoadDiagnostics;
import io.github.misode.packtest.PackTestRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagLoader;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Catch tag errors and removes stacktrace.
 */
@Mixin(TagLoader.class)
public class TagLoaderMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Shadow
    @Final
    private String directory;

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private void catchTagError(Logger logger, String message, Object[] args, Operation<Void> original) {
        String identifier = ((Identifier)args[0]).toString();
        String error = ((Exception)args[3]).getMessage().replaceFirst("^[A-Za-z0-9.]+Exception: ", "");
        LoadDiagnostics.error(LOGGER, this.directory, identifier, error);
    }

    @WrapOperation(method = "lambda$build$2", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private static void catchTagReferenceError(Logger logger, String message, Object id, Object refs, Operation<Void> original) {
        LoadDiagnostics.error(LOGGER, "tag", ((Identifier)id).toString(), "Missing references: " + refs);
    }

    @ModifyReturnValue(method = "loadTagsForExistingRegistries", at = @At("RETURN"))
    private static List<Registry.PendingTags<?>> dropReloadedRegistries(List<Registry.PendingTags<?>> tags) {
        return tags.stream().filter(pending -> !PackTestRegistries.owns(pending.key())).toList();
    }
}
