package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestException;
import io.github.misode.packtest.PackTestFunction;
import io.github.misode.packtest.PackTestLibrary;
import io.github.misode.packtest.PackTest;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
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
 * Remove coordinates and add command numbers from failing test logs when auto is enabled.
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
    private void onTestFailed(GameTestInfo testInfo, CallbackInfo ci) {
        if (PackTest.isAutoEnabled()) {
            String testName = testInfo.id().toString();
            String lineNumber = testInfo.getError() instanceof PackTestException err
                    ? " on line " + err.getLine()
                    : "";
            String message = Util.describeError(testInfo.getError());
            if (testInfo.isRequired()) {
                if (PackTest.isAnnotationsEnabled()) {
                    LOGGER.error(PackTest.wrapError("{} failed{}!") + "\n::error title=Test {} failed{}!::{}", testName, lineNumber, testName, lineNumber, message);
                } else {
                    LOGGER.error(PackTest.wrapError("{} failed{}! {}"), testName, lineNumber, message);
                }
            } else {
                LOGGER.warn(PackTest.wrapWarning("(optional) {} failed{}! {}"), testName, lineNumber, message);
            }
            ci.cancel();
        }
    }

    @Inject(method = "onTestSuccess", at = @At(value = "HEAD"))
    private void onTestSuccess(GameTestInfo testInfo, CallbackInfo ci) {
        BlockPos blockPos = testInfo.getTestBlockPos();
        if (blockPos == null) {
            LOGGER.info("{} passed on tick {}!" , testInfo.id(), testInfo.getTick());
        } else {
            LOGGER.info("{} passed at {} on tick {}!" , testInfo.id(), blockPos.toShortString(), testInfo.getTick());
        }
    }
}
