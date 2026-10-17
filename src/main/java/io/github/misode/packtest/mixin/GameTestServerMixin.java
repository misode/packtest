package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * Load data pack dimensions in test world.
 * Prints summary of resources that failed to load.
 * Fails when no tests were loaded.
 * Guarantee that the tests run inside the world border.
 */
@Mixin(GameTestServer.class)
public class GameTestServerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow @Nullable private MultipleTestTracker testTracker;

    @WrapOperation(method = "lambda$create$1", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/WorldDimensions;bake(Lnet/minecraft/core/Registry;)Lnet/minecraft/world/level/levelgen/WorldDimensions$Complete;"))
    private static WorldDimensions.Complete addDatapackLevels(WorldDimensions worldDimensions, Registry<LevelStem> baseDimensions, Operation<WorldDimensions.Complete> original, @Local(argsOnly = true, name = "context") WorldLoader.DataLoadContext context) {
        return original.call(worldDimensions, context.datapackDimensions().lookupOrThrow(Registries.LEVEL_STEM));
    }

    @Inject(method = "tickServer", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;info(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V", remap = false, shift = At.Shift.AFTER))
    private void tickServer(BooleanSupplier haveTime, CallbackInfo ci) {
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
        int x = level.getRandom().nextIntBetweenInclusive((int) border.getMinX(), (int) border.getMaxX());
        int z = level.getRandom().nextIntBetweenInclusive((int) border.getMinZ(), (int) border.getMaxZ());
        return new BlockPos(x, original.getY(), z);
    }
}
