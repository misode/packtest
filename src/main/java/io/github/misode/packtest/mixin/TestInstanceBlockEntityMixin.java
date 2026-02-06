package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.TemporaryForcedChunks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * When force-loading chunks for tests, mark them as temporary.
 */
@Mixin(TestInstanceBlockEntity.class)
public class TestInstanceBlockEntityMixin {
    @WrapOperation(method = "lambda$forceLoadChunks$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setChunkForced(IIZ)Z"))
    private static boolean setChunkForced(ServerLevel level, int x, int z, boolean bl, Operation<Boolean> original) {
        if (!level.getForceLoadedChunks().contains(ChunkPos.pack(x, z))) {
            TemporaryForcedChunks.markTemporary(level, x, z);
        }
        return original.call(level, x, z, bl);
    }
}
