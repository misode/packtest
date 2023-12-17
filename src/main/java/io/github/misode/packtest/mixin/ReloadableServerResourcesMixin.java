package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds {@link PackTestLibrary} to the /reload listeners and give it the permissionLevel and dispatcher
 */
@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {

    @Shadow
    @Final
    private Commands commands;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(RegistryAccess.Frozen frozen, FeatureFlagSet featureFlagSet, Commands.CommandSelection commandSelection, int permissionLevel, CallbackInfo ci) {
        PackTestLibrary.INSTANCE.setPermissionLevel(permissionLevel);
        PackTestLibrary.INSTANCE.setDispatcher(commands.getDispatcher());
    }

    @ModifyReturnValue(method = "listeners", at = @At("RETURN"))
    private static List<PreparableReloadListener> listeners(List<PreparableReloadListener> list) {
        List<PreparableReloadListener> result = new ArrayList<>(list);
        result.add(PackTestLibrary.INSTANCE);
        return result;
    }
}
