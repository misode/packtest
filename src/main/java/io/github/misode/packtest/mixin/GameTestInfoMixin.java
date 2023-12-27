package io.github.misode.packtest.mixin;

import io.github.misode.packtest.ChatListener;
import io.github.misode.packtest.PackTestInfo;
import io.github.misode.packtest.SoundListener;
import net.minecraft.gametest.framework.GameTestInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds chat and sound listener field and accessors.
 * Removes the listeners when finished.
 * Prevents crash when test has already started.
 */
@Mixin(GameTestInfo.class)
public class GameTestInfoMixin implements PackTestInfo {

    @Unique
    private ChatListener chatListener;
    @Unique
    private SoundListener soundListener;

    @Override
    public void packtest$setChatListener(ChatListener chatListener) {
        this.chatListener = chatListener;
    }

    @Override
    public ChatListener packtest$getChatListener() {
        return this.chatListener;
    }

    @Override
    public void packtest$setSoundListener(SoundListener soundListener) {
        this.soundListener = soundListener;
    }

    @Override
    public SoundListener packtest$getSoundListener() {
        return this.soundListener;
    }

    @Inject(method = "startTest", cancellable = true, at = @At(value = "INVOKE", target = "Ljava/lang/IllegalStateException;<init>(Ljava/lang/String;)V"))
    private void startTest(CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "finish", at = @At("HEAD"))
    private void finish(CallbackInfo ci) {
        this.chatListener.stop();
        this.soundListener.stop();
    }
}
