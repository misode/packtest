package io.github.misode.packtest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

public class PackTestRegistry {
    private static final String BATCH_NAME = "packtestBatch";
    private static final Collection<TestFunction> TEST_FUNCTIONS = Lists.newArrayList();
    private static final Set<String> TEST_CLASS_NAMES = Sets.newHashSet();

    public static void register(ResourceLocation name, Consumer<GameTestHelper> consumer) {
        String testName = name.getNamespace() + "." + name.getPath();
        String structureName = "fabric-gametest-api-v1:empty";
        Rotation rotation = StructureUtils.getRotationForRotationSteps(0);
        TEST_FUNCTIONS.add(new TestFunction(BATCH_NAME, testName, structureName, rotation, 100, 0L, true, 1, 1, consumer));
        TEST_CLASS_NAMES.add(name.getNamespace());
    }

    public static Collection<TestFunction> getAllTestFunctions() {
        return TEST_FUNCTIONS;
    }

    public static Collection<String> getAllTestClassNames() {
        return TEST_CLASS_NAMES;
    }
}
