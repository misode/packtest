package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GameTestHelper.class)
public class GameTestHelperMixin implements PackTestHelper {
    @Shadow
    private boolean finalCheckAdded;

    @Unique
    public boolean packtest$isFinalCheckAdded() {
        return this.finalCheckAdded;
    }
}
