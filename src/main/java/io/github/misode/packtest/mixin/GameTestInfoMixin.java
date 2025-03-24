package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.ChatListener;
import io.github.misode.packtest.PackTestInfo;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds chat listener field and accessors. Removes the listener when finishing.
 * Clears dummies after succeeding.
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin implements PackTestInfo {

    @Shadow
    public abstract ServerLevel getLevel();

    @Shadow private int tickCount;
    @Unique
    private ChatListener chatListener;

    @Override
    public int packtest$getTick() {
        return this.tickCount;
    }

    @Override
    public void packtest$setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    @Override
    public ChatListener packtest$getChatListener() {
        return this.chatListener;
    }

    @Inject(method = "finish", at = @At("HEAD"))
    private void finish(CallbackInfo ci) {
        if (this.chatListener != null) {
            this.chatListener.stop();
        }
    }

    @Inject(method = "succeed", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private void succeed(CallbackInfo ci, @Local(ordinal = 0) AABB aabb) {
        this.getLevel().getEntitiesOfClass(Dummy.class, aabb.inflate(1))
                .forEach(dummy -> dummy.leave(Component.literal("Test succeeded")));
    }
}
