package io.github.misode.packtest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class PackTestServer {
    private final DedicatedServer server;
    private @Nullable MultipleTestTracker testTracker = null;

    public PackTestServer(DedicatedServer server) {
        this.server = server;
    }

    public void runTests() {
        ServerLevel serverLevel = this.server.overworld();
        BlockPos blockPos = new BlockPos(
                serverLevel.random.nextIntBetweenInclusive(-14999992, 14999992),
                -59,
                serverLevel.random.nextIntBetweenInclusive(-14999992, 14999992)
        );
        Collection<GameTestBatch> batches = GameTestRunner.groupTestsIntoBatches(GameTestRegistry.getAllTestFunctions());
        Collection<GameTestInfo> tests = GameTestRunner.runTestBatches(batches, blockPos, Rotation.NONE, serverLevel, GameTestTicker.SINGLETON, 8);
        this.testTracker = new MultipleTestTracker(tests);
    }

    public void tick() {
        if (this.testTracker != null && this.testTracker.isDone()) {
            if (testTracker.hasFailedRequired()) {
                PackTest.LOGGER.error(testTracker.getFailedRequiredCount() + " required tests failed :(");
            } else {
                PackTest.LOGGER.info("All required tests passed :)");
            }
            this.server.halt(false);
        }
    }
}
