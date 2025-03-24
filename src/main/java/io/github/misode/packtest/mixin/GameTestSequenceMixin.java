package io.github.misode.packtest.mixin;

import io.github.misode.packtest.LineNumberException;
import io.github.misode.packtest.PackTestInfo;
import io.github.misode.packtest.PackTestSequence;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Add an idle method that includes the line number in the failure message
 */
@Mixin(GameTestSequence.class)
public abstract class GameTestSequenceMixin implements PackTestSequence {

    @Shadow public abstract GameTestSequence thenWaitUntil(Runnable runnable);

    @Shadow @Final GameTestInfo parent;

    @Shadow private int lastTick;

    @Override
    public void packtest$thenIdle(int delay, int lineNumber, String timeArgument) {
        this.thenWaitUntil(() -> {
            int tickCount = ((PackTestInfo) this.parent).packtest$getTick();
            if (tickCount < this.lastTick + (long) delay) {
                throw new LineNumberException(Component.literal("Whilst waiting " + timeArgument), tickCount, lineNumber);
            }
        });
    }
}
