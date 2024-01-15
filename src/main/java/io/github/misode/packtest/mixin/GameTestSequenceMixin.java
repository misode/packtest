package io.github.misode.packtest.mixin;

import io.github.misode.packtest.LineNumberException;
import io.github.misode.packtest.PackTestInfo;
import io.github.misode.packtest.PackTestSequence;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.GameTestSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameTestSequence.class)
public abstract class GameTestSequenceMixin implements PackTestSequence {

    @Shadow public abstract GameTestSequence thenWaitUntil(Runnable runnable);

    @Shadow @Final GameTestInfo parent;

    @Shadow private long lastTick;

    @Override
    public void packtest$thenIdle(int delay, int lineNumber, String timeArgument) {
        this.thenWaitUntil(() -> {
            if (((PackTestInfo) this.parent).packtest$getTick() < this.lastTick + (long) delay) {
                throw new LineNumberException("Whilst waiting " + timeArgument, lineNumber);
            }
        });
    }
}
