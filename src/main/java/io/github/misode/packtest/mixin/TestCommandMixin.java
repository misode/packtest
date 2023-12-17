package io.github.misode.packtest.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.TestCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Modifies the /test command such that it requires a permission level of 2
 */
@Mixin(TestCommand.class)
public class TestCommandMixin {
    @ModifyExpressionValue(
            method = "register",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;literal(Ljava/lang/String;)Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;", ordinal = 0)
    )
    private static LiteralArgumentBuilder<CommandSourceStack> register(LiteralArgumentBuilder<CommandSourceStack> original) {
        return original.requires(ctx -> ctx.hasPermission(2));
    }
}
