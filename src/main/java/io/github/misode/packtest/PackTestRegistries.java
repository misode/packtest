package io.github.misode.packtest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.github.misode.packtest.accessor.MappedRegistryAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;

public class PackTestRegistries {
    private static final Set<ResourceKey<? extends Registry<?>>> TEST_REGISTRY_KEYS = Set.of(Registries.TEST_ENVIRONMENT, Registries.TEST_INSTANCE);

    private static final List<RegistryDataLoader.RegistryData<?>> TEST_REGISTRIES = RegistryDataLoader.WORLD_REGISTRIES.stream()
            .filter(data -> TEST_REGISTRY_KEYS.contains(data.key()))
            .toList();

    private static final Set<ResourceKey<Consumer<GameTestHelper>>> registeredFunctionKeys = new HashSet<>();

    private final HolderLookup.Provider registries;

    public PackTestRegistries(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    public static boolean owns(ResourceKey<? extends Registry<?>> registryKey) {
        return TEST_REGISTRY_KEYS.contains(registryKey);
    }

    public CompletableFuture<RegistryAccess.Frozen> load(ResourceManager manager, Executor executor) {
        return RegistryDataLoader.load(manager, this.registries.listRegistries().toList(), TEST_REGISTRIES, executor);
    }

    public void register(RegistryAccess.Frozen loaded, Map<Identifier, PackTestFunction> tests) {
        MappedRegistry<TestEnvironmentDefinition<?>> environments = replace(Registries.TEST_ENVIRONMENT, loaded);
        environments.freeze();

        MappedRegistry<GameTestInstance> instances = replace(Registries.TEST_INSTANCE, loaded);
        MappedRegistry<Consumer<GameTestHelper>> functions = (MappedRegistry<Consumer<GameTestHelper>>) registries.lookupOrThrow(Registries.TEST_FUNCTION);
        clearRegistered(functions);

        for (Map.Entry<Identifier, PackTestFunction> entry : tests.entrySet()) {
            Identifier id = entry.getKey();
            PackTestFunction test = entry.getValue();

            ResourceKey<Consumer<GameTestHelper>> functionKey = ResourceKey.create(Registries.TEST_FUNCTION, id);
            ResourceKey<GameTestInstance> instanceKey = ResourceKey.create(Registries.TEST_INSTANCE, id);

            try {
                if (!instances.containsKey(instanceKey)) {
                    TestData<Holder<TestEnvironmentDefinition<?>>> testData = test.directives().createTestData(environments);
                    instances.register(instanceKey, new FunctionGameTestInstance(functionKey, testData), RegistrationInfo.BUILT_IN);
                }

                functions.register(functionKey, test::run, RegistrationInfo.BUILT_IN);
                registeredFunctionKeys.add(functionKey);
            } catch (Exception e) {
                PackTest.LOGGER.error("Failed to load test {}", id, e);
            }
        }

        instances.freeze();
        functions.freeze();

        PackTest.LOGGER.info("Loaded {} test functions", tests.size());
    }

    private <T> MappedRegistry<T> replace(ResourceKey<Registry<T>> registryKey, RegistryAccess.Frozen source) {
        MappedRegistry<T> registry = (MappedRegistry<T>) registries.lookupOrThrow(registryKey);
        MappedRegistryAccessor<T> accessor = unfrozen(registry);
        accessor.packtest$clearByPredicate(_ -> true);

        Registry<T> loaded = source.lookupOrThrow(registryKey);

        for (Holder.Reference<T> holder : loaded.listElements().toList()) {
            registry.register(holder.key(), holder.value(), RegistrationInfo.BUILT_IN);
        }

        registry.bindAllTagsToEmpty();
        registry.bindTags(rebind(loaded, registry));

        accessor.packtest$adopt(loaded);
        return registry;
    }

    private static void clearRegistered(MappedRegistry<Consumer<GameTestHelper>> functions) {
        MappedRegistryAccessor<Consumer<GameTestHelper>> accessor = unfrozen(functions);

        if (!registeredFunctionKeys.isEmpty()) {
            accessor.packtest$clearByPredicate(registeredFunctionKeys::contains);
            registeredFunctionKeys.clear();
        }
    }

    private static <T> Map<TagKey<T>, List<Holder<T>>> rebind(Registry<T> loaded, Registry<T> registry) {
        return loaded.getTags().collect(Collectors.toMap(
                HolderSet.Named::key,
                tag -> tag.stream().<Holder<T>>flatMap(holder -> holder.unwrapKey().flatMap(registry::get).stream()).toList()));
    }

    @SuppressWarnings("unchecked")
    private static <T> MappedRegistryAccessor<T> unfrozen(MappedRegistry<T> registry) {
        MappedRegistryAccessor<T> accessor = (MappedRegistryAccessor<T>) registry;
        accessor.packtest$unfreeze();
        return accessor;
    }
}
