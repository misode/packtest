package io.github.misode.packtest.mixin.server;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.misode.packtest.PackTest;
import net.minecraft.server.Main;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Start the test server instead of the normal dedicated server. Based on Fabric API.
 */
@Mixin(Main.class)
public class MainMixin {
    @Inject(method = "main", cancellable = true, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/server/packs/repository/ServerPacksSource;createPackRepository(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;)Lnet/minecraft/server/packs/repository/PackRepository;", shift = At.Shift.AFTER))
    private static void mainStartServer(String[] args, CallbackInfo ci, @Local LevelStorageSource.LevelStorageAccess storage, @Local PackRepository packRepository) {
        if (PackTest.isAutoEnabled()) {
            PackTest.runHeadlessServer(storage, packRepository);
            ci.cancel();
        }
    }

    @Inject(method = "main", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Lorg/slf4j/Marker;Ljava/lang/String;Ljava/lang/Throwable;)V", shift = At.Shift.AFTER, remap = false), remap = false)
    private static void exitOnError(CallbackInfo info) {
        if (PackTest.isAutoEnabled()) {
            System.exit(-1);
        }
    }
}
