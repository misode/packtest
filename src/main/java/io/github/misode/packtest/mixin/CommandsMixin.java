package io.github.misode.packtest.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.gametest.framework.TestCommand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/commands/WorldBorderCommand;register(Lcom/mojang/brigadier/CommandDispatcher;)V", shift = At.Shift.AFTER))
    private void init(Commands.CommandSelection selection, CommandBuildContext buildContext, CallbackInfo info) {
        if (!SharedConstants.IS_RUNNING_IN_IDE) {
            TestCommand.register(this.dispatcher);
        }
    }
}
