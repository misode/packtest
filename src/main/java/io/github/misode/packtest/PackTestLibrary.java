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
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionSet;
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
    public static final PackTestLibrary INSTANCE = new PackTestLibrary(null, LevelBasedPermissionSet.GAMEMASTER);
    private static final FileToIdConverter LISTER = new FileToIdConverter("test", ".mcfunction");

    private HolderGetter.Provider registries;
    private PermissionSet permissionSet;

    public PackTestLibrary(HolderGetter.Provider registries, PermissionSet permissionSet) {
        this.registries = registries;
        this.permissionSet = permissionSet;
    }

    public void setRegistries(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    public void setPermissionSet(PermissionSet permissionSet) {
        this.permissionSet = permissionSet;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparableReloadListener.SharedState sharedState, Executor executor1, PreparableReloadListener.PreparationBarrier barrier, Executor executor2) {
        CompletableFuture<Map<Identifier, CompletableFuture<PackTestFunction>>> prep = CompletableFuture.supplyAsync(
                () -> LISTER.listMatchingResources(sharedState.resourceManager()), executor1
        ).thenComposeAsync(map -> {
            Map<Identifier, CompletableFuture<PackTestFunction>> result = Maps.newHashMap();
            for(Map.Entry<Identifier, Resource> entry : map.entrySet()) {
                Identifier id = LISTER.fileToId(entry.getKey());
                result.put(id, CompletableFuture.supplyAsync(() -> {
                    List<String> lines = readLines(entry.getValue());
                    return PackTestFunction.fromLines(lines, this.permissionSet);
                }));
            }
            CompletableFuture<?>[] futures = result.values().toArray(new CompletableFuture[0]);
            return CompletableFuture.allOf(futures).handle((a, b) -> result);
        });

        return prep.thenCompose(barrier::wait).thenAcceptAsync(functions -> {
            ImmutableMap.Builder<Identifier, PackTestFunction> testsBuilder = ImmutableMap.builder();
            functions.forEach((id, future) -> future.handle((val, err) -> {
                if (err != null) {
                    PackTest.LOGGER.error("Failed to load test {}", id, err);
                } else {
                    testsBuilder.put(id, val);
                }
                return null;
            }).join());
            ImmutableMap<Identifier, PackTestFunction> tests = testsBuilder.build();
            HolderGetter<GameTestInstance> testInstanceRegistry = this.registries.lookup(Registries.TEST_INSTANCE).orElseThrow();
            HolderGetter<Consumer<GameTestHelper>> testFunctionRegistry = this.registries.lookup(Registries.TEST_FUNCTION).orElseThrow();
            ((PackTestRegistry)testInstanceRegistry).packtest$setFrozen(false);
            ((PackTestRegistry)testFunctionRegistry).packtest$setFrozen(false);
            tests.forEach((id, test) -> {
                ResourceKey<Consumer<GameTestHelper>> functionKey = ResourceKey.create(Registries.TEST_FUNCTION, id);
                if (!((MappedRegistry<Consumer<GameTestHelper>>)testFunctionRegistry).containsKey(functionKey)) {
                    ((MappedRegistry<Consumer<GameTestHelper>>)testFunctionRegistry).register(functionKey, test::run, RegistrationInfo.BUILT_IN);
                }
                ResourceKey<GameTestInstance> instanceKey = ResourceKey.create(Registries.TEST_INSTANCE, id);
                if (!((MappedRegistry<GameTestInstance>)testInstanceRegistry).containsKey(instanceKey)) {
                    GameTestInstance testInstance = new FunctionGameTestInstance(functionKey, test.getTestData(this.registries));
                    ((MappedRegistry<GameTestInstance>)testInstanceRegistry).register(instanceKey, testInstance, RegistrationInfo.BUILT_IN);
                }
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
