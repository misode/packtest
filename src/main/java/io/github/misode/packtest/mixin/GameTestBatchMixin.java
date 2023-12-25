package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.gametest.framework.GameTestBatch;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(GameTestBatch.class)
public class GameTestBatchMixin {

    @Shadow
    @Final
    private String name;

    @Inject(method = "runBeforeBatchFunction", at = @At("TAIL"))
    private void runBeforeBatchFunction(ServerLevel level, CallbackInfo ci) {
        String batchName = this.name.substring(0, this.name.lastIndexOf(":"));
        Consumer<ServerLevel> consumer = PackTestLibrary.INSTANCE.getBeforeBatchFunction(batchName);
        if (consumer != null) {
            consumer.accept(level);
        }
    }

    @Inject(method = "runAfterBatchFunction", at = @At("TAIL"))
    private void runAfterBatchFunction(ServerLevel level, CallbackInfo ci) {
        String batchName = this.name.substring(0, this.name.lastIndexOf(":"));
        Consumer<ServerLevel> consumer = PackTestLibrary.INSTANCE.getAfterBatchFunction(batchName);
        if (consumer != null) {
            consumer.accept(level);
        }
    }
}
