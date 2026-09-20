package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.PackTest;
import io.github.misode.packtest.accessor.MappedRegistryAccessor;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import net.minecraft.core.*;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(MappedRegistry.class)
public abstract class MappedRegistryMixin<T> implements MappedRegistryAccessor<T>, HolderOwner<T> {
    @Shadow
    @Final
    private ObjectList<Holder.Reference<T>> byId;
    @Shadow
    @Final
    private Reference2IntMap<T> toId;
    @Shadow
    @Final
    private Map<Identifier, Holder.Reference<T>> byLocation;
    @Shadow
    @Final
    private Map<ResourceKey<T>, Holder.Reference<T>> byKey;
    @Shadow
    @Final
    private Map<T, Holder.Reference<T>> byValue;
    @Shadow
    @Final
    private Map<ResourceKey<T>, RegistrationInfo> registrationInfos;
    @Shadow
    private boolean frozen;

    @Shadow
    private MappedRegistry.TagSet<T> allTags;

    @Shadow
    public abstract ResourceKey<? extends Registry<T>> key();

    @Unique
    private @Nullable HolderOwner<T> packtest$adopted;

    @Override
    public boolean canSerialize(@NonNull HolderOwner<T> owner) {
        return owner == this || owner == this.packtest$adopted;
    }

    @Override
    @Unique
    public void packtest$unfreeze() {
        this.frozen = false;
        this.allTags = MappedRegistry.TagSet.unbound();
    }

    @Override
    @Unique
    public void packtest$adopt(HolderOwner<T> owner) {
        this.packtest$adopted = owner;
    }

    @Override
    @Unique
    public void packtest$clearByPredicate(Predicate<ResourceKey<T>> predicate) {
        List<ResourceKey<T>> keysToRemove = byKey.keySet().stream().filter(predicate).toList();
        keysToRemove.forEach(this::removeEntry);
        rebuildIdMappings();
    }

    @Unique
    private void removeEntry(ResourceKey<T> key) {
        Holder.Reference<T> holder = byKey.remove(key);

        if (holder != null && holder.isBound()) {
            T value = holder.value();
            byLocation.remove(key.identifier());
            byValue.remove(value);
            toId.removeInt(value);
            registrationInfos.remove(key);
        }
    }

    @Unique
    private void rebuildIdMappings() {
        byId.clear();
        toId.clear();

        for (Holder.Reference<T> holder : byKey.values()) {
            if (holder.isBound()) {
                int newId = byId.size();
                byId.add(holder);
                toId.put(holder.value(), newId);
            }
        }
    }

    /**
     * Reports references to missing elements and lets the registry freeze anyway.
     * Vanilla would throw instead, dropping the whole registry and crashing later lookups.
     */
    @WrapOperation(method = "freeze", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z", ordinal = 0))
    private boolean reportUnboundValues(List<Identifier> unboundEntries, Operation<Boolean> original) {
        if (!original.call(unboundEntries)) {
            String registry = this.key().identifier().toString();
            unboundEntries.forEach(id ->
                PackTest.LOGGER.error("Unbound value in registry {}: {} is referenced but never defined", registry, id));
        }

        return true;
    }
}
