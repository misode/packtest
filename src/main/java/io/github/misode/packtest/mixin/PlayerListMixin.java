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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Prevent player data and advancements from being saved for dummies.
 * Create Dummy object when respawning.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
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
}
