package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.misode.packtest.PackTestSourceStack;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Adds a {@link GameTestHelper} field and accessors
 */
@Mixin(CommandSourceStack.class)
public class CommandSourceStackMixin implements PackTestSourceStack {
    @Unique
    private GameTestHelper helper;

    public GameTestHelper packtest$getHelper() {
        return this.helper;
    }

    public void packtest$setHelper(GameTestHelper helper) {
        this.helper = helper;
    }

    @ModifyReturnValue(method = "withSource", at = @At("RETURN"))
    private CommandSourceStack withSource(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withEntity", at = @At("RETURN"))
    private CommandSourceStack withEntity(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withPosition", at = @At("RETURN"))
    private CommandSourceStack withPosition(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withRotation", at = @At("RETURN"))
    private CommandSourceStack withRotation(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withCallback(Lnet/minecraft/commands/CommandResultCallback;)Lnet/minecraft/commands/CommandSourceStack;", at = @At("RETURN"))
    private CommandSourceStack withCallback(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withSuppressedOutput", at = @At("RETURN"))
    private CommandSourceStack withSuppressedOutput(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withPermission", at = @At("RETURN"))
    private CommandSourceStack withPermission(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withMaximumPermission", at = @At("RETURN"))
    private CommandSourceStack withMaximumPermission(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withAnchor", at = @At("RETURN"))
    private CommandSourceStack withAnchor(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withLevel", at = @At("RETURN"))
    private CommandSourceStack withLevel(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }

    @ModifyReturnValue(method = "withSigningContext", at = @At("RETURN"))
    private CommandSourceStack withSigningContext(CommandSourceStack original) {
        ((CommandSourceStackMixin)(Object)original).helper = this.helper;
        return original;
    }
}
