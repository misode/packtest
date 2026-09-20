package io.github.misode.packtest.accessor;

import net.minecraft.core.HolderOwner;
import net.minecraft.resources.ResourceKey;

import java.util.function.Predicate;

public interface MappedRegistryAccessor<T> {
    void packtest$unfreeze();
    void packtest$adopt(HolderOwner<T> owner);
    void packtest$clearByPredicate(Predicate<ResourceKey<T>> predicate);
}
