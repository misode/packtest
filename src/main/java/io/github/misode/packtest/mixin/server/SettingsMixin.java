package io.github.misode.packtest.mixin.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.PackTest;
import net.minecraft.server.dedicated.Settings;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Remove warning when it fails to load server.properties
 */
@Mixin(Settings.class)
public class SettingsMixin {
    @WrapOperation(method = "loadFromFile", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false))
    private static void loadFromFile(Logger logger, String message, Object path, Object e, Operation<Void> original) {
        if (!PackTest.isAutoEnabled()) {
            original.call(logger, message, path, e);
        }
    }
}
