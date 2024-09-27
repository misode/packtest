package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.misode.packtest.LoadDiagnostics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootDataType;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Catches loot table, predicate and item modifier errors.
 * Improves error message.
 */
@Mixin(LootDataType.class)
public class LootDataTypeMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @WrapOperation(method = "method_53267", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;[Ljava/lang/Object;)V", remap = false))
    private void deserialize(Logger logger, String message, Object[] args, Operation<Void> original) {
        String type = ((ResourceLocation)args[0]).getPath().replaceAll("_", " ").replaceFirst("s$", "");
        LoadDiagnostics.error(LOGGER, type, ((ResourceLocation)args[1]).toString(), (String)args[2]);
    }
}
