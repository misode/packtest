package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestHelper;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds an accessor for the GameTestInfo
 */
@Mixin(GameTestHelper.class)
public class GameTestHelperMixin implements PackTestHelper {
    @Shadow
    @Final
    private GameTestInfo testInfo;

    @Unique
    public GameTestInfo packtest$getInfo() {
        return this.testInfo;
    }
}
