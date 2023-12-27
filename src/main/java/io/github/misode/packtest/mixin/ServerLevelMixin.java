package io.github.misode.packtest.mixin;

import io.github.misode.packtest.SoundListener;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Logs all sounds that are played
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"))
    private void playSeededSound(Player player, double x, double y, double z, Holder<SoundEvent> holder, SoundSource soundSource, float volume, float pitch, long seed, CallbackInfo ci) {
        SoundListener.broadcast(player, new Vec3((float)x, (float)y, (float)z), holder);
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V", at = @At("HEAD"))
    private void playSeededSound(Player player, Entity entity, Holder<SoundEvent> holder, SoundSource soundSource, float volume, float pitch, long seed, CallbackInfo ci) {
        SoundListener.broadcast(player, entity.position(), holder);
    }
}
