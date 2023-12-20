package io.github.misode.packtest.mixin;

import io.github.misode.packtest.fake.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fixes starting position of fake players when they load in
 */
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "load", at = @At(value = "RETURN", shift = At.Shift.BEFORE))
    private void fixStartingPos(ServerPlayer player, CallbackInfoReturnable<CompoundTag> cir) {
        if (player instanceof FakePlayer) {
            ((FakePlayer) player).fixStartingPosition.run();
        }
    }
}
