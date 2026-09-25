package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import io.github.misode.packtest.ChatRecorder;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Prevent player data and advancements from being saved for dummies.
 * Create Dummy object when respawning.
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V", at = @At("HEAD"))
    private void recordBroadcast(Component message, Function<ServerPlayer, Component> playerMessages, boolean overlay, CallbackInfo ci) {
        ChatRecorder.record(Util.NIL_UUID, message.getString());
    }

    @Inject(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V", at = @At("HEAD"))
    private void recordChatBroadcast(PlayerChatMessage message, Predicate<ServerPlayer> isFiltered, ServerPlayer senderPlayer, ChatType.Bound chatType, CallbackInfo ci) {
        ChatRecorder.record(Util.NIL_UUID, message.decoratedContent().getString());
    }

    @Inject(method = "save", at = @At(value = "HEAD"), cancellable = true)
    private void skipSaveDummy(ServerPlayer player, CallbackInfo ci) {
        if (player instanceof Dummy) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "getPlayerAdvancements", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object getPlayerAdvancements(Map<Object, Object> map, Object key, Operation<Object> original, @Local(argsOnly = true, name = "player") ServerPlayer player) {
        if (player instanceof Dummy) {
            return null;
        } else {
            return original.call(map, key);
        }
    }

    @WrapOperation(method = "respawn", at = @At(value = "NEW", target = "(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/server/level/ServerLevel;Lcom/mojang/authlib/GameProfile;Lnet/minecraft/server/level/ClientInformation;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer createDummy(MinecraftServer server, ServerLevel level, GameProfile gameProfile, ClientInformation clientInformation, Operation<ServerPlayer> original, @Local(argsOnly = true, name = "serverPlayer") ServerPlayer serverPlayer) {
        if (serverPlayer instanceof Dummy dummy) {
            return new Dummy(server, level, gameProfile, clientInformation, dummy.spawnPosition, dummy.spawnRotation);
        } else {
            return original.call(server, level, gameProfile, clientInformation);
        }
    }
}
