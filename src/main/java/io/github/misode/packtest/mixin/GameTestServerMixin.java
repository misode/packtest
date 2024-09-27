package io.github.misode.packtest.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.LoadDiagnostics;
import io.github.misode.packtest.PackTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Don't fail when initial batches is empty.
 * Get list of batches after data packs have been loaded.
 * Prints summary of resources that failed to load.
 * Fails when no tests were loaded.
 */
@Mixin(GameTestServer.class)
public class GameTestServerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nullable private MultipleTestTracker testTracker;

    @ModifyExpressionValue(method = "create", at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z"))
    private static boolean isBatchesEmpty(boolean original) {
        return original && !PackTest.isAutoEnabled();
    }

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList(Ljava/lang/Iterable;)Ljava/util/ArrayList;", remap = false))
    private ArrayList<TestFunction> modifyTests(ArrayList<TestFunction> original) {
        Collection<TestFunction> testFunctions = GameTestRegistry.getAllTestFunctions();
        return Lists.newArrayList(testFunctions);
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false, shift = At.Shift.AFTER))
    private void tickServer(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        List<LoadDiagnostics.Diagnostic> errors = LoadDiagnostics.loadErrors();
        if (!errors.isEmpty()) {
            LOGGER.info("{} resources failed to load :(", errors.size());
            errors.forEach(diagnostic -> LOGGER.info("   - {} {}", diagnostic.resource(), diagnostic.id()));
        }
    }

    @WrapOperation(method = "tickServer", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;)V", remap = false, ordinal = 2))
    private void checkNoTests(Logger logger, String message, Object arg, Operation<Void> original) {
        if (Objects.requireNonNull(this.testTracker).getTotalCount() == 0) {
            LOGGER.info("No tests were loaded :(");
        } else {
            original.call(logger, message, arg);
        }
    }

    @ModifyExpressionValue(method = "onServerExit", at = @At(value = "INVOKE", target = "Lnet/minecraft/gametest/framework/MultipleTestTracker;getFailedRequiredCount()I"))
    private int onServerExit(int original) {
        if (Objects.requireNonNull(this.testTracker).getTotalCount() == 0) {
            return 1;
        }
        return original + LoadDiagnostics.loadErrors().size();
    }

    @ModifyExpressionValue(method = "startTests", at = @At(value = "NEW", target = "(III)Lnet/minecraft/core/BlockPos;"))
    private BlockPos startTests(BlockPos original, ServerLevel level) {
        WorldBorder border = level.getWorldBorder();
        int x = level.random.nextIntBetweenInclusive((int) border.getMinX(), (int) border.getMaxX());
        int z = level.random.nextIntBetweenInclusive((int) border.getMinZ(), (int) border.getMaxZ());
        return new BlockPos(x, original.getY(), z);
    }
}
