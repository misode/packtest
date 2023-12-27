package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.SoundListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Log raid horn sound
 */
@Mixin(Raid.class)
public class RaidMixin {
    @Inject(method = "playSound", at = @At(value = "NEW", target = "(Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;DDDFFJ)Lnet/minecraft/network/protocol/game/ClientboundSoundPacket;"))
    private void playSound(BlockPos blockPos, CallbackInfo ci, @Local(ordinal = 0) ServerPlayer player, @Local(ordinal = 1) double x, @Local(ordinal = 2) double z) {
        SoundListener.broadcast(player, new Vec3(x, player.getY(), z), SoundEvents.RAID_HORN);
    }
}
