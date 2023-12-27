package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.SoundListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.PlaySoundCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * Logs all sounds that are played
 */
@Mixin(PlaySoundCommand.class)
public class PlaySoundCommandMixin {

    @Inject(method = "playSound", at = @At(value = "NEW", target = "(Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;DDDFFJ)Lnet/minecraft/network/protocol/game/ClientboundSoundPacket;"))
    private static void playSound(CommandSourceStack commandSourceStack, Collection<ServerPlayer> players, ResourceLocation resourceLocation, SoundSource soundSource, Vec3 vec3, float f, float g, float h, CallbackInfoReturnable<Integer> cir, @Local(ordinal = 0) ServerPlayer player, @Local(ordinal = 1) Vec3 pos, @Local(ordinal = 0) Holder<SoundEvent> sound) {
        SoundListener.broadcast(player, pos, sound);
    }
}
