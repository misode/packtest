package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears dummies after succeeding.
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin {
    @Shadow
    public abstract ServerLevel getLevel();

    @Inject(method = "succeed", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private void succeed(CallbackInfo ci, @Local(name = "bounds") AABB bounds) {
        this.getLevel().getEntitiesOfClass(Dummy.class, bounds.inflate(1))
                .forEach(dummy -> dummy.leave(Component.literal("Test succeeded")));
    }
}
