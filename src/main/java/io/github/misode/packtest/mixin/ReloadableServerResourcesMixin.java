package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(LayeredRegistryAccess<RegistryLayer> registries, HolderLookup.Provider provider, FeatureFlagSet enabledFeatures, Commands.CommandSelection commands, List<?> postponedTags, PermissionSet permissionSet, List<?> newComponents, CallbackInfo ci) {
        PackTestLibrary.INSTANCE.setRegistries(provider);
        PackTestLibrary.INSTANCE.setPermissionSet(permissionSet);
    }

    @ModifyReturnValue(method = "listeners", at = @At("RETURN"))
    private List<PreparableReloadListener> listeners(List<PreparableReloadListener> list) {
        List<PreparableReloadListener> result = new ArrayList<>(list);
        result.add(PackTestLibrary.INSTANCE);
        return result;
    }
}
