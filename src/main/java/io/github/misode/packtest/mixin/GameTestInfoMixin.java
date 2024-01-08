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
 * Prevents crash when test has already started.
 * Clears dummies after succeeding.
 */
@Mixin(GameTestInfo.class)
public abstract class GameTestInfoMixin implements PackTestInfo {

    @Shadow
    public abstract ServerLevel getLevel();

    @Unique
    private ChatListener chatListener;

    @Override
    public void packtest$setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    @Override
    public ChatListener packtest$getChatListener() {
        return this.chatListener;
    }

    @Inject(method = "startTest", cancellable = true, at = @At(value = "INVOKE", target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V"))
    private void startTest(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "finish", at = @At("HEAD"))
    private void finish(CallbackInfo ci) {
        this.chatListener.stop();
    }

    @Inject(method = "succeed", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V", shift = At.Shift.AFTER))
    private void succeed(CallbackInfo ci, @Local(ordinal = 0) AABB aabb) {
        this.getLevel().getEntitiesOfClass(Dummy.class, aabb.inflate(1))
                .forEach(dummy -> dummy.leave(Component.literal("Test succeeded")));
    }
}
