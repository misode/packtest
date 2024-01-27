package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SimpleJsonResourceReloadListener.class)
public class SimpleJsonResourceReloadListenerMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @WrapOperation(method = "scanDirectory", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private static void catchTagReferenceError(Logger logger, String message, Object[] args, Operation<Void> original) {
        String path = ((ResourceLocation)args[1]).getPath();
        String type = path.substring(0, path.indexOf("/")).replaceAll("_", " ").replaceFirst("s$", "");
        String error = ((Exception)args[2]).getMessage().replaceFirst("^[A-Za-z0-9.]+Exception: ", "");
        LoadDiagnostics.error(LOGGER, type, ((ResourceLocation)args[0]).toString(), error);
    }
}
