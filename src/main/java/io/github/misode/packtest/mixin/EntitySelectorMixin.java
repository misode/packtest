package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestArgumentSource;
import io.github.misode.packtest.PackTestPlayerName;
import net.minecraft.commands.arguments.selector.EntitySelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a new field for storing the original text of the entity selector
 */
@Mixin(EntitySelector.class)
public class EntitySelectorMixin implements PackTestArgumentSource, PackTestPlayerName {
    @Unique
    private String packtestSource;

    @Shadow
    @Final
    private String playerName;

    @Override
    public String packtest$getSource() {
        return this.packtestSource;
    }

    @Override
    public void packtest$setSource(String source) {
        this.packtestSource = source;
    }

    public String packtest$getPlayerName() {
        return this.playerName;
    }
}
