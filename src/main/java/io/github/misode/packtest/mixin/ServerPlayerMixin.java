package io.github.misode.packtest.mixin;

import io.github.misode.packtest.ChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs all chat messages sent to players
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"))
    private void sendSystemMessage(Component message, boolean bl, CallbackInfo ci) {
        ChatListener.broadcast((ServerPlayer)(Object)this, message);
    }
}
