package io.github.misode.packtest.mixin;

import com.google.common.collect.Lists;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.misode.packtest.PackTest;
import net.minecraft.gametest.framework.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Don't fail when initial batches is empty.
 * Get list of batches after data packs have been loaded.
 */
@Mixin(GameTestServer.class)
public class GameTestServerMixin {

    @ModifyExpressionValue(method = "create", at = @At(value = "INVOKE", target = "Ljava/util/Collection;isEmpty()Z"))
    private static boolean isBatchesEmpty(boolean original) {
        return original && !PackTest.isAutoEnabled();
    }

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/Lists;newArrayList(Ljava/lang/Iterable;)Ljava/util/ArrayList;", remap = false))
    private ArrayList<GameTestBatch> modifyBatches(ArrayList<GameTestBatch> batches) {
        Collection<TestFunction> testFunctions = GameTestRegistry.getAllTestFunctions();
        return Lists.newArrayList(GameTestRunner.groupTestsIntoBatches(testFunctions));
    }
}
