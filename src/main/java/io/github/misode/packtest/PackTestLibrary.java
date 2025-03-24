package io.github.misode.packtest;

import com.google.common.collect.*;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class PackTestLibrary implements PreparableReloadListener {
    public static final PackTestLibrary INSTANCE = new PackTestLibrary(null,2);
    private static final FileToIdConverter LISTER = new FileToIdConverter("test", ".mcfunction");

    private HolderGetter.Provider registries;
    private int permissionLevel;

    public PackTestLibrary(HolderGetter.Provider registries, int permissionLevel) {
        this.registries = registries;
        this.permissionLevel = permissionLevel;
    }

    public void setRegistries(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    public void setPermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager resources, Executor executor1, Executor executor2) {
        CompletableFuture<Map<ResourceLocation, CompletableFuture<PackTestFunction>>> prep = CompletableFuture.supplyAsync(
                () -> LISTER.listMatchingResources(resources), executor1
        ).thenComposeAsync(map -> {
            Map<ResourceLocation, CompletableFuture<PackTestFunction>> result = Maps.newHashMap();
            for(Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                ResourceLocation id = LISTER.fileToId(entry.getKey());
                result.put(id, CompletableFuture.supplyAsync(() -> {
                    List<String> lines = readLines(entry.getValue());
                    return PackTestFunction.fromLines(lines, this.permissionLevel);
                }));
            }
            CompletableFuture<?>[] futures = result.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(futures).handle((a, b) -> result);
        });

        return prep.thenCompose(barrier::wait).thenAcceptAsync(functions -> {
            ImmutableMap.Builder<ResourceLocation, PackTestFunction> testsBuilder = ImmutableMap.builder();
            functions.forEach((id, future) -> future.handle((val, err) -> {
                if (err != null) {
                    PackTest.LOGGER.error("Failed to load test {}", id, err);
                } else {
                    testsBuilder.put(id, val);
                }
                return null;
            }).join());
            ImmutableMap<ResourceLocation, PackTestFunction> tests = testsBuilder.build();
            HolderGetter<GameTestInstance> testInstanceRegistry = this.registries.lookup(Registries.TEST_INSTANCE).orElseThrow();
            HolderGetter<Consumer<GameTestHelper>> testFunctionRegistry = this.registries.lookup(Registries.TEST_FUNCTION).orElseThrow();
            ((PackTestRegistry)testInstanceRegistry).packtest$setFrozen(false);
            ((PackTestRegistry)testFunctionRegistry).packtest$setFrozen(false);
            tests.forEach((id, test) -> {
                ResourceKey<Consumer<GameTestHelper>> functionKey = ResourceKey.create(Registries.TEST_FUNCTION, id);
                ((MappedRegistry<Consumer<GameTestHelper>>)testFunctionRegistry).register(functionKey, test::run, RegistrationInfo.BUILT_IN);
                ResourceKey<GameTestInstance> instanceKey = ResourceKey.create(Registries.TEST_INSTANCE, id);
                GameTestInstance testInstance = new FunctionGameTestInstance(functionKey, test.getTestData(this.registries));
                ((MappedRegistry<GameTestInstance>)testInstanceRegistry).register(instanceKey, testInstance, RegistrationInfo.BUILT_IN);
            });
            ((PackTestRegistry)testInstanceRegistry).packtest$setFrozen(true);
            ((PackTestRegistry)testFunctionRegistry).packtest$setFrozen(true);
            PackTest.LOGGER.info("Loaded {} tests", tests.size());
        });
    }

    private static List<String> readLines(Resource resource) {
        try (BufferedReader lvt1 = resource.openAsReader()) {
            return lvt1.lines().toList();
        } catch (IOException var6) {
            throw new CompletionException(var6);
        }
    }
}
