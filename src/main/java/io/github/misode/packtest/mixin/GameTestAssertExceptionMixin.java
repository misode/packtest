package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestAssertException;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameTestAssertException.class)
public class GameTestAssertExceptionMixin implements PackTestAssertException {
    @Shadow
    @Final
    protected Component message;

    @Shadow
    @Final
    protected int tick;

    @Override
    public Component packtest$getMessage() {
        return this.message;
    }

    @Override
    public int packtest$getTick() {
        return this.tick;
    }
}
