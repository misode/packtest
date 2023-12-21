package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Fixes starting position of test players when they load in.
 * Respawns test players and in the correct position.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "load", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayer player, CallbackInfoReturnable<CompoundTag> cir) {
        if (player instanceof Dummy) {
            ((Dummy) player).fixStartingPosition.run();
        }
    }

    @WrapOperation(method = "respawn", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer createPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli, Operation<ServerPlayer> original, @Local(ordinal = 0) ServerPlayer player) {
        if (player instanceof Dummy) {
            return new Dummy(server, level, profile, cli);
        } else {
            return original.call(server, level, profile, cli);
        }
    }

    @WrapOperation(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getRespawnPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos getRespawnBlock(ServerPlayer player, Operation<BlockPos> original) {
        if (player instanceof Dummy testPlayer && testPlayer.origin != null) {
            return testPlayer.origin;
        } else {
            return original.call(player);
        }
    }

    @WrapOperation(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;findRespawnPositionAndUseSpawnBlock(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;FZZ)Ljava/util/Optional;"))
    private Optional<Vec3> getRespawnPos(ServerLevel level, BlockPos pos, float angle, boolean forced, boolean other, Operation<Optional<Vec3>> original, @Local(ordinal = 0) ServerPlayer player) {
        if (player instanceof Dummy testPlayer && testPlayer.origin != null) {
            return Optional.of(new Vec3(testPlayer.origin.getX() + 0.5, testPlayer.origin.getY(), testPlayer.origin.getZ() + 0.5));
        } else {
            return original.call(level, pos, angle, forced, other);
        }
    }
}
