package io.github.misode.packtest;

import com.google.common.collect.*;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.commands.execution.ExecutionContext;
import net.minecraft.commands.functions.CommandFunction;
import net.minecraft.commands.functions.InstantiatedFunction;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PackTestLibrary implements PreparableReloadListener {
    public static final Logger LOGGER = LoggerFactory.getLogger("packtest");
    public static final PackTestLibrary INSTANCE = new PackTestLibrary(2, new CommandDispatcher<>());
    private static final FileToIdConverter LISTER = new FileToIdConverter("tests", ".mcfunction");
    private static final String BATCH_NAME = "packtestBatch";

    private int permissionLevel;
    private CommandDispatcher<CommandSourceStack> dispatcher;
    private Collection<TestFunction> tests = Lists.newArrayList();
    private Set<String> namespaces = Sets.newHashSet();
    private Consumer<String> messageConsumer = null;

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

    public void failMessage(String message) {
        this.messageConsumer.accept(message);
    }

    @Override
    public @NotNull CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier, ResourceManager resources, ProfilerFiller profiler1, ProfilerFiller profiler2, Executor executor1, Executor executor2) {
        CompletableFuture<Map<ResourceLocation, CompletableFuture<CommandFunction<CommandSourceStack>>>> prep = CompletableFuture.supplyAsync(
                () -> LISTER.listMatchingResources(resources), executor1
        ).thenComposeAsync(map -> {
            Map<ResourceLocation, CompletableFuture<CommandFunction<CommandSourceStack>>> result = Maps.newHashMap();
            CommandSourceStack sourceStack = new CommandSourceStack(
                    CommandSource.NULL, Vec3.ZERO, Vec2.ZERO, null, 2, "", CommonComponents.EMPTY, null, null
            );
            for(Map.Entry<ResourceLocation, Resource> entry : map.entrySet()) {
                ResourceLocation id = LISTER.fileToId(entry.getKey());
                result.put(id, CompletableFuture.supplyAsync(() -> {
                    List<String> lines = readLines(entry.getValue());
                    return CommandFunction.fromLines(id, this.dispatcher, sourceStack, lines);
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
                    LOGGER.error("Failed to load test {}", id, err);
                } else {
                    testsBuilder.add(toTestFunction(val));
                    namespacesBuilder.add(id.getNamespace());
                }
                return null;
            }).join());
            this.tests = testsBuilder.build();
            this.namespaces = namespacesBuilder.build();
            LOGGER.info("Loaded {} tests", this.tests.size());
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

    private TestFunction toTestFunction(CommandFunction<CommandSourceStack> commandFn) {
        String testName = commandFn.id().toLanguageKey();
        String structureName = "fabric-gametest-api-v1:empty";
        Rotation rotation = StructureUtils.getRotationForRotationSteps(0);

        return new TestFunction(BATCH_NAME, testName, structureName, rotation, 100, 0L, true, 1, 1, (helper) -> {
            AtomicBoolean hasFailed = new AtomicBoolean(false);
            CommandSourceStack sourceStack = helper.getLevel().getServer().createCommandSourceStack()
                    .withPosition(helper.absoluteVec(Vec3.ZERO))
                    .withPermission(this.permissionLevel)
                    .withSuppressedOutput()
                    .withCallback((success, result) -> hasFailed.set(!success));
            try {
                AtomicReference<String> failMessage = new AtomicReference<>("Test failed");
                this.messageConsumer = failMessage::set;
                InstantiatedFunction<CommandSourceStack> instantiatedFn = commandFn.instantiate(null, this.dispatcher, sourceStack);
                Commands.executeCommandInContext(sourceStack, execution -> {
                    ExecutionContext.queueInitialFunctionCall(execution, instantiatedFn, sourceStack, CommandResultCallback.EMPTY);
                });
                if (hasFailed.get()) {
                    helper.fail(failMessage.toString());
                } else {
                    helper.succeed();
                }
            } catch (FunctionInstantiationException e) {
                String message = e.messageComponent().getString();
                helper.fail("Failed to instantiate test function: " + message);
            }
        });
    }
}
