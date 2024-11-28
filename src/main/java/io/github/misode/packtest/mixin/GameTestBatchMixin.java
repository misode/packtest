package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/**
 * Overwrite batch before and after consumers
 */
@Mixin(GameTestBatch.class)
public class GameTestBatchMixin {
    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    private static Consumer<ServerLevel> modifyBeforeBatch(Consumer<ServerLevel> original, @Local(ordinal = 0, argsOnly = true) String name) {
        String batchName = name.substring(0, name.lastIndexOf(":"));
        Consumer<ServerLevel> beforeBatch = PackTestLibrary.INSTANCE.getBeforeBatchFunction(batchName);
        return beforeBatch != null ? beforeBatch : original;
    }

    @ModifyVariable(method = "<init>", at = @At(value = "HEAD"), ordinal = 0, argsOnly = true)
    private static Consumer<ServerLevel> modifyAfterBatch(Consumer<ServerLevel> original, @Local(ordinal = 0, argsOnly = true) String name) {
        String batchName = name.substring(0, name.lastIndexOf(":"));
        Consumer<ServerLevel> afterBatch = PackTestLibrary.INSTANCE.getAfterBatchFunction(batchName);
        return afterBatch != null ? afterBatch : original;
    }
}
