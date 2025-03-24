package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestRegistry;
import net.minecraft.core.MappedRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MappedRegistry.class)
public class MappedRegistryMixin implements PackTestRegistry {
    @Shadow
    private boolean frozen;

    @Override
    public void packtest$setFrozen(boolean frozen) {
        this.frozen = frozen;
    }
}
