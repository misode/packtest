package io.github.misode.packtest;

import com.google.common.collect.*;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class PackTestLibrary implements PreparableReloadListener {
    public static final PackTestLibrary INSTANCE = new PackTestLibrary(2, new CommandDispatcher<>());
    private static final FileToIdConverter LISTER = new FileToIdConverter("tests", ".mcfunction");

    private int permissionLevel;
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private Collection<TestFunction> tests = Lists.newArrayList();
    private Set<String> namespaces = Sets.newHashSet();
    private final Map<String, GameTestHelper> testHelperMap = Maps.newHashMap();

    public PackTestLibrary(int permissionLevel, CommandDispatcher<CommandSourceStack> dispatcher) {
        this.permissionLevel = permissionLevel;
        this.dispatcher = dispatcher;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public void setDispatcher(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void registerTestHelper(String testName, GameTestHelper helper) {
        this.testHelperMap.put(testName, helper);
    }
    public void unregisterTestHelper(String testName) {
        this.testHelperMap.remove(testName);
    }
    public Optional<GameTestHelper> getTestHelper(String testName) {
        GameTestHelper helper = this.testHelperMap.get(testName);
        if (helper == null) {
            PackTest.LOGGER.warn("No helper registered for {}", testName);
        }
        return Optional.ofNullable(helper);
    }

    public Optional<GameTestHelper> getHelperAt(CommandSourceStack sourceStack) {
        BlockPos blockPos = BlockPos.containing(sourceStack.getPosition());
        return getHelperAt(blockPos, sourceStack.getLevel());
    }

    public Optional<GameTestHelper> getHelperAt(BlockPos pos, ServerLevel level) {
        Optional<BlockPos> structurePos = StructureUtils.findStructureBlockContainingPos(pos, 15, level);
        if (structurePos.isEmpty()) {
            PackTest.LOGGER.warn("No structure at {}", pos);
            return Optional.empty();
        }
        StructureBlockEntity structureBlockEntity = (StructureBlockEntity)level.getBlockEntity(structurePos.get());
        if (structureBlockEntity == null) {
            PackTest.LOGGER.warn("No block entity at {}", structurePos);
            return Optional.empty();
        }
        String metadata = structureBlockEntity.getMetaData();
        Optional<TestFunction> testFunction = GameTestRegistry.findTestFunction(metadata);
        if (testFunction.isEmpty()) {
            PackTest.LOGGER.warn("No test function for {}", metadata);
            return Optional.empty();
        }
        String testName = testFunction.get().getTestName();
        return this.getTestHelper(testName);
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager resources, ProfilerFiller profiler1, ProfilerFiller profiler2, Executor executor1, Executor executor2) {
        CompletableFuture<Map<ResourceLocation, CompletableFuture<PackTestFunction>>> prep = CompletableFuture.supplyAsync(
                () -> LISTER.listMatchingResources(resources), executor1
        ).thenComposeAsync(map -> {
            Map<ResourceLocation, CompletableFuture<PackTestFunction>> result = Maps.newHashMap();
            for(Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                ResourceLocation id = LISTER.fileToId(entry.getKey());
                result.put(id, CompletableFuture.supplyAsync(() -> {
                    List<String> lines = readLines(entry.getValue());
                    return PackTestFunction.fromLines(id, this.dispatcher, lines);
                }));
            }
            CompletableFuture<?>[] futures = result.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(futures).handle((a, b) -> result);
        });

        return prep.thenCompose(barrier::wait).thenAcceptAsync(functions -> {
            ImmutableList.Builder<TestFunction> testsBuilder = ImmutableList.builder();
            ImmutableSet.Builder<String> namespacesBuilder = ImmutableSet.builder();
            functions.forEach((id, future) -> future.handle((val, err) -> {
                if (err != null) {
                    PackTest.LOGGER.error("Failed to load test {}", id, err);
                } else {
                    testsBuilder.add(val.toTestFunction(this.permissionLevel, this.dispatcher));
                    namespacesBuilder.add(id.getNamespace());
                }
                return null;
            }).join());
            this.tests = testsBuilder.build();
            this.namespaces = namespacesBuilder.build();
            PackTest.LOGGER.info("Loaded {} tests", this.tests.size());
        });
    }

    public Collection<TestFunction> getAllTestFunctions() {
        return tests;
    }

    public Collection<String> getAllTestClassNames() {
        return namespaces;
    }

    private static List<String> readLines(Resource resource) {
        try (BufferedReader lvt1 = resource.openAsReader()) {
            return lvt1.lines().toList();
        } catch (IOException var6) {
            throw new CompletionException(var6);
        }
    }
}
