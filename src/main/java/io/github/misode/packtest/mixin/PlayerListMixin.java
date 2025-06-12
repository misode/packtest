package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Fixes starting position of dummies when they load in.
 * Respawns dummies and in the correct position.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "load", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayer player, ProblemReporter problemReporter, CallbackInfoReturnable<Optional<ValueInput>> cir) {
        if (player instanceof Dummy dummy) {
            Vec3 pos = dummy.originalSpawn;
            dummy.snapTo(pos.x, pos.y, pos.z, 0, 0);
        }
    }

    @WrapOperation(method = "load", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/PlayerDataStorage;load(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/util/ProblemReporter;)Ljava/util/Optional;"))
    private Optional<ValueInput> skipLoadDummy(PlayerDataStorage playerIo, Player player, ProblemReporter problemReporter, Operation<Optional<ValueInput>> original) {
        if (player instanceof Dummy) {
            return Optional.empty();
        } else {
            return original.call(playerIo, player, problemReporter);
        }
    }

    @Inject(method = "save", at = @At(value = "HEAD"), cancellable = true)
    private void skipSaveDummy(ServerPlayer player, CallbackInfo ci) {
        if (player instanceof Dummy) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "getPlayerAdvancements", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object getPlayerAdvancements(Map<Object, Object> map, Object key, Operation<Object> original, @Local(ordinal = 0, argsOnly = true) ServerPlayer player) {
        if (player instanceof Dummy) {
            return null;
        } else {
            return original.call(map, key);
        }
    }

    @WrapOperation(method = "respawn", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer createDummy(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli, Operation<ServerPlayer> original, @Local(ordinal = 0, argsOnly = true) ServerPlayer player) {
        if (player instanceof Dummy dummy) {
            return new Dummy(server, level, profile, cli, dummy.originalSpawn);
        } else {
            return original.call(server, level, profile, cli);
        }
    }

    @Inject(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V"))
    private void teleportDummy(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir, @Local(ordinal = 1) ServerPlayer player) {
        if (player instanceof Dummy dummy) {
            Vec3 pos = dummy.originalSpawn;
            dummy.snapTo(pos.x, pos.y, pos.z, 0, 0);
            dummy.teleportTo(dummy.level(), pos.x, pos.y, pos.z, Set.of(), 0, 0, true);
        }
    }
}
