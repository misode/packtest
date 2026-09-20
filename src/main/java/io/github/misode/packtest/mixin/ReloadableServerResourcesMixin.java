package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds {@link PackTestLibrary} to the /reload listeners and give it the permissionSet
 */
@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {
    @Unique
    private PackTestLibrary testLibrary;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(
            ReloadableServerRegistries.LoadResult loadingContext,
            FeatureFlagSet enabledFeatures,
            Commands.CommandSelection commandSelection,
            List<Registry<Registry.PendingTags<?>>> postponedTags,
            PermissionSet functionCompilationPermissions,
            List<DataComponentInitializers.PendingComponents<?>> newComponents,
            CallbackInfo ci) {
        this.testLibrary = new PackTestLibrary(
                loadingContext.lookupWithUpdatedTags(),
                functionCompilationPermissions);
    }

    @ModifyReturnValue(method = "listeners", at = @At("RETURN"))
    private List<PreparableReloadListener> listeners(List<PreparableReloadListener> list) {
        List<PreparableReloadListener> result = new ArrayList<>(list);
        result.add(testLibrary);
        return result;
    }
}
