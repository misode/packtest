package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import io.github.misode.packtest.fake.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes starting position of fake players when they load in.
 * Respawns fake players.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "load", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayer player, CallbackInfoReturnable<CompoundTag> cir) {
        if (player instanceof FakePlayer) {
            ((FakePlayer) player).fixStartingPosition.run();
        }
    }

    @WrapOperation(method = "respawn", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer createPlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli, Operation<ServerPlayer> original, @Local(ordinal = 0) ServerPlayer player) {
        if (player instanceof FakePlayer) {
            return new FakePlayer(server, level, profile, cli);
        } else {
            return original.call(server, level, profile, cli);
        }
    }
}
