package io.github.misode.packtest.mixin;

import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestRunner;
import net.minecraft.gametest.framework.GameTestTicker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Remove all test players when running <code>/test clearall</code>.
 */
@Mixin(GameTestRunner.class)
public class GameTestRunnerMixin {

    @Inject(method = "clearAllTests", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;betweenClosedStream(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/util/stream/Stream;", shift = At.Shift.AFTER))
    private static void clearTestPlayers(ServerLevel level, BlockPos pos, GameTestTicker ticker, int radius, CallbackInfo ci) {
        List<Dummy> testPlayers = level.getServer().getPlayerList().getPlayers().stream()
                .filter(p -> p instanceof Dummy && p.distanceToSqr(pos.getCenter()) <= radius)
                .map(p -> (Dummy)p)
                .toList();
        testPlayers.forEach(p -> p.leave(Component.literal("Cleared tests")));
    }
}
