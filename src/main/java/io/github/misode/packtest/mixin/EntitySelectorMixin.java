package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestArgumentSource;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a new field for storing the original text of the entity selector
 */
@Mixin(EntitySelector.class)
public class EntitySelectorMixin implements PackTestArgumentSource {
    @Unique
    public String packtestSource;

    @Override
    public String packtest$getSource() {
        return this.packtestSource;
    }

    @Override
    public void packtest$setSource(String source) {
        this.packtestSource = source;
    }
}
