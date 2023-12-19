package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestArgumentSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a new field for storing the original text of the block predicate
 */
@Mixin(targets = "net.minecraft.commands.arguments.blocks.BlockPredicateArgument$TagPredicate")
public class BlockPredicateArgumentTagMixin implements PackTestArgumentSource {
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
