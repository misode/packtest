package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import io.github.misode.packtest.SoundListener;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes starting position of dummies when they load in.
 * Respawns dummies and in the correct position.
 * Log respawn anchor depletion sound.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "load", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayer player, CallbackInfoReturnable<CompoundTag> cir) {
        if (player instanceof Dummy dummy) {
            Vec3 pos = dummy.originalSpawn;
            dummy.moveTo(pos.x, pos.y, pos.z, 0, 0);
        }
    }

    @WrapOperation(method = "respawn", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer createDummy(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli, Operation<ServerPlayer> original, @Local(ordinal = 0) ServerPlayer player) {
        if (player instanceof Dummy dummy) {
            return new Dummy(server, level, profile, cli, dummy.originalSpawn);
        } else {
            return original.call(server, level, profile, cli);
        }
    }

    @Inject(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V"))
    private void teleportDummy(ServerPlayer serverPlayer, boolean bl, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer player) {
        if (player instanceof Dummy dummy) {
            Vec3 pos = dummy.originalSpawn;
            dummy.moveTo(pos.x, pos.y, pos.z, 0, 0);
            dummy.teleportTo(dummy.serverLevel(), pos.x, pos.y, pos.z, 0, 0);
        }
    }

    @Inject(method = "respawn", at = @At(value = "NEW", target = "(Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;DDDFFJ)Lnet/minecraft/network/protocol/game/ClientboundSoundPacket;"))
    private void logSound(ServerPlayer oldPlayer, boolean bl, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer player, @Local(ordinal = 0) BlockPos pos) {
        SoundListener.broadcast(player, new Vec3(pos.getX(), pos.getY(), pos.getZ()), SoundEvents.RESPAWN_ANCHOR_DEPLETE);
    }
}
