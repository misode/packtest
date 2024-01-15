package io.github.misode.packtest.mixin;

import io.github.misode.packtest.LineNumberException;
import io.github.misode.packtest.PackTest;
import net.minecraft.Util;
import net.minecraft.gametest.framework.GameTestInfo;
import net.minecraft.gametest.framework.LogTestReporter;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Remove coordinates and add line numbers from failing test logs when auto is enabled.
 * Apply ascii color codes to failure messages.
 */
@Mixin(LogTestReporter.class)
public class LogTestReporterMixin {
    @Shadow
    @Final
    @Mutable
    private static Logger LOGGER;

    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "onTestFailed", at = @At(value = "HEAD"), cancellable = true)
    private void onTestFailed(GameTestInfo info, CallbackInfo ci) {
        if (PackTest.isAutoEnabled()) {
            boolean color = PackTest.isAutoColoringEnabled();
            String lineNumber = info.getError() instanceof LineNumberException err
                    ? " on line " + err.getLineNumber()
                    : "";
            if (info.isRequired()) {
                LOGGER.error((color ? "\u001b[0;31m" : "") + "{} failed{}! {}" + (color ? "\u001b[0m" : ""), info.getTestName(), lineNumber, Util.describeError(info.getError()));
            } else {
                LOGGER.warn((color ? "\u001b[0;33m" : "") + "(optional) {} failed{}! {}" + (color ? "\u001b[0m" : ""), info.getTestName(), lineNumber, Util.describeError(info.getError()));
            }
            ci.cancel();
        }
    }
}
