package io.github.misode.packtest.mixin;

import io.github.misode.packtest.ChatListener;
import io.github.misode.packtest.SoundListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs all chat messages sent to players.
 * Logs sounds that are played.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @Inject(method = "sendSystemMessage(Lnet/minecraft/network/chat/Component;Z)V", at = @At("HEAD"))
    private void sendSystemMessage(Component message, boolean bl, CallbackInfo ci) {
        ChatListener.broadcast((Player)(Object)this, message);
    }

    @Inject(method = "playNotifySound", at = @At("HEAD"))
    private void playNotifySound(SoundEvent soundEvent, SoundSource soundSource, float volume, float pitch, CallbackInfo ci) {
        SoundListener.broadcast((Player)(Object)this, ((Player)(Object)this).position(), BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent));
    }
}
