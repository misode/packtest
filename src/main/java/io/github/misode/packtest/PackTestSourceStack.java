package io.github.misode.packtest;

import net.minecraft.gametest.framework.GameTestHelper;
import org.jetbrains.annotations.Nullable;

public interface PackTestSourceStack {
    @Nullable GameTestHelper packtest$getHelper();
    void packtest$setHelper(GameTestHelper helper);
}
