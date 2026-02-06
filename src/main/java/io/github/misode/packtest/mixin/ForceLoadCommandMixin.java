package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.TemporaryForcedChunks;
import net.minecraft.server.commands.ForceLoadCommand;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * When force-loading a chunk that was temporarily marked by a test, unmark it as temporary.
 * When un-force-loading a chunk that was temporarily marked by a test, do not unload it.
 */
@Mixin(ForceLoadCommand.class)
public class ForceLoadCommandMixin {

    @WrapOperation(method = "changeForceLoad", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setChunkForced(IIZ)Z"))
    private static boolean changeForceLoad(ServerLevel level, int x, int z, boolean toggle, Operation<Boolean> original) {
        return handleChange(level, x, z, toggle, original);
    }

    @WrapOperation(method = "lambda$removeAll$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setChunkForced(IIZ)Z"))
    private static boolean removeAll(ServerLevel level, int x, int z, boolean toggle, Operation<Boolean> original) {
        return handleChange(level, x, z, toggle, original);
    }

    @Unique
    private static boolean handleChange(ServerLevel level, int x, int z, boolean toggle, Operation<Boolean> original) {
        if (TemporaryForcedChunks.isTemporary(level, x, z)) {
            if (toggle) {
                TemporaryForcedChunks.unmarkTemporary(level, x, z);
                return true;
            } else {
                return false;
            }
        }
        return original.call(level, x, z, toggle);
    }
}
