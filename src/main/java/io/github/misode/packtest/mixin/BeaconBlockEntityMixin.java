package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Prevents dummies from receiving the "Bring Home the Beacon" advancement
 * due to the beacons used as part of the test status indicator
 */
@Mixin(BeaconBlockEntity.class)
public class BeaconBlockEntityMixin {

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private static <T> List<T> preventBeaconAdvancement(List<T> entities, @Local(argsOnly = true) Level level, @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
        BlockPos structurePos = pos.east().south().above(2);
        if (level.getBlockState(structurePos).is(Blocks.STRUCTURE_BLOCK)) {
            return entities.stream().filter(e -> !(e instanceof Dummy)).toList();
        }
        return entities;
    }
}
