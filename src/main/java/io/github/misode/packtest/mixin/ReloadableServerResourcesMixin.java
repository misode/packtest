package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.server.RegistryLayer;
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
    private void init(LayeredRegistryAccess<RegistryLayer> layeredRegistryAccess, HolderLookup.Provider provider, FeatureFlagSet featureFlagSet, Commands.CommandSelection commandSelection, List<Registry.PendingTags<?>> list, int permissionLevel, CallbackInfo ci) {
        PackTestLibrary.INSTANCE.setRegistries(provider);
        PackTestLibrary.INSTANCE.setPermissionLevel(permissionLevel);
    }

    @ModifyReturnValue(method = "listeners", at = @At("RETURN"))
    private List<PreparableReloadListener> listeners(List<PreparableReloadListener> list) {
        List<PreparableReloadListener> result = new ArrayList<>(list);
        result.add(PackTestLibrary.INSTANCE);
        return result;
    }
}
