package io.github.misode.packtest.mixin;

import io.github.misode.packtest.ChatListener;
import io.github.misode.packtest.PackTestInfo;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds chat listener field and accessors. Removes the listener when finishing.
 * Prevents crash when test has already started.
 */
@Mixin(GameTestInfo.class)
public class GameTestInfoMixin implements PackTestInfo {

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
}
