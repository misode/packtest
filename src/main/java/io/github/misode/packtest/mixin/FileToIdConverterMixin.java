package io.github.misode.packtest.mixin;

import io.github.misode.packtest.PackTestFileToIdConverter;
import net.minecraft.resources.FileToIdConverter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FileToIdConverter.class)
public class FileToIdConverterMixin implements PackTestFileToIdConverter {
    @Shadow @Final private String prefix;

    @Unique
    public String packtest$getPrefix() {
        return prefix;
    }
}
