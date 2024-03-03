package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.TemporaryForcedChunks;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Only un-force-load chunks that were temporarily loaded for tests
 */
@Mixin(targets = "net.minecraft.gametest.framework.GameTestRunner$1")
public class GameTestRunner1Mixin {

    @WrapOperation(method = "method_56233", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setChunkForced(IIZ)Z"))
    private boolean testCompleted(ServerLevel level, int x, int z, boolean value, Operation<Boolean> original) {
        if (TemporaryForcedChunks.isTemporary(level, x, z)) {
            TemporaryForcedChunks.unmarkTemporary(level, x, z);
            return original.call(level, x, z, value);
        }
        return false;
    }
}
