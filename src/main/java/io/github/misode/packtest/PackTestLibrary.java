package io.github.misode.packtest;

import com.google.common.collect.*;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.permissions.PermissionSet;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public class PackTestLibrary implements PreparableReloadListener {
    private static final FileToIdConverter LISTER = new FileToIdConverter("test", ".mcfunction");

    private final PackTestRegistries registries;
    private final PermissionSet testCompilationPermissions;
    private final CommandDispatcher<CommandSourceStack> dispatcher;

    public PackTestLibrary(
            HolderLookup.Provider registries,
            PermissionSet testCompilationPermissions,
            CommandDispatcher<CommandSourceStack> dispatcher) {
        this.registries = new PackTestRegistries(registries);
        this.testCompilationPermissions = testCompilationPermissions;
        this.dispatcher = dispatcher;
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(
            @NotNull SharedState currentReload,
            @NotNull Executor taskExecutor,
            PreparationBarrier preparationBarrier,
            @NotNull Executor reloadExecutor) {
        ResourceManager manager = currentReload.resourceManager();
        CompletableFuture<RegistryAccess.Frozen> registryLoad = this.registries.load(manager, taskExecutor);

        CompletableFuture<Map<Identifier, CompletableFuture<PackTestFunction>>> testFunctions = CompletableFuture
                .supplyAsync(() -> LISTER.listMatchingResources(manager), taskExecutor)
                .thenComposeAsync(map -> prepareTestFunctions(map, taskExecutor));

        return CompletableFuture.allOf(registryLoad, testFunctions)
                .thenCompose(preparationBarrier::wait)
                .thenAcceptAsync((_) ->
                        this.registries.register(registryLoad.join(), collectTestFunctions(testFunctions.join())),
                        reloadExecutor);
    }

    private CompletableFuture<Map<Identifier, CompletableFuture<PackTestFunction>>> prepareTestFunctions(
            Map<Identifier, Resource> resources,
            Executor taskExecutor) {
        Map<Identifier, CompletableFuture<PackTestFunction>> result = new HashMap<>();
        CommandSourceStack compilationContext = Commands.createCompilationContext(this.testCompilationPermissions);

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier id = LISTER.fileToId(entry.getKey());
            result.put(id, CompletableFuture.supplyAsync(() -> {
                List<String> lines = readLines(entry.getValue());
                return PackTestFunction.fromLines(this.dispatcher, compilationContext, lines);
            }, taskExecutor));
        }

        return CompletableFuture.allOf(result.values().toArray(new CompletableFuture[0])).handle((_, _) -> result);
    }

    private static Map<Identifier, PackTestFunction> collectTestFunctions(Map<Identifier, CompletableFuture<PackTestFunction>> futures) {
        ImmutableMap.Builder<Identifier, PackTestFunction> result = ImmutableMap.builder();
        futures.forEach((id, future) -> future.handle((test, e) -> {
            if (e == null) {
                result.put(id, test);
            } else {
                PackTest.LOGGER.error("Failed to load test {}", id, e);
            }
            return null;
        }).join());
        return result.build();
    }

    private static List<String> readLines(Resource resource) {
        try (BufferedReader lvt1 = resource.openAsReader()) {
            return lvt1.lines().toList();
        } catch (IOException var6) {
            throw new CompletionException(var6);
        }
    }
}
