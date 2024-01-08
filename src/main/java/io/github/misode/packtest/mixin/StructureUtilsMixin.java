package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.TemporaryForcedChunks;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Remove all dummies when clearing space for tests.
 * When force-loading chunks for tests, mark them as temporary.
 */
@Mixin(StructureUtils.class)
public class StructureUtilsMixin {

    @Inject(method = "clearSpaceForStructure", at = @At("TAIL"))
    private static void clearDummies(BoundingBox boundingBox, ServerLevel level, CallbackInfo ci) {
        List<Dummy> testPlayers = level.getServer().getPlayerList().getPlayers().stream()
                .filter(p -> p instanceof Dummy && boundingBox.isInside(p.blockPosition()))
                .map(p -> (Dummy)p)
                .toList();
        testPlayers.forEach(p -> p.leave(Component.literal("Cleared tests")));
    }

    @WrapOperation(method = "method_54901", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setChunkForced(IIZ)Z"))
    private static boolean setChunkForced(ServerLevel level, int x, int z, boolean value, Operation<Boolean> original) {
        if (!level.getForcedChunks().contains(ChunkPos.asLong(x, z))) {
            TemporaryForcedChunks.markTemporary(level, x, z);
        }
        return original.call(level, x, z, value);
    }
}
