package io.github.misode.packtest.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.SharedConstants;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.TestClassNameArgument;
import net.minecraft.gametest.framework.TestFunctionArgument;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArgumentTypeInfos.class)
public class ArgumentTypeInfosMixin {
    @Shadow
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> ArgumentTypeInfo<A, T> register(Registry<ArgumentTypeInfo<?, ?>> registry, String string, Class<? extends A> clazz, ArgumentTypeInfo<A, T> argumentSerializer) {
        throw new AssertionError("Nope.");
    }

    @Inject(method = "bootstrap", at = @At("RETURN"))
    private static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>> void bootstrap(Registry<ArgumentTypeInfo<?, ?>> registry, CallbackInfoReturnable<ArgumentTypeInfo<A, T>> cir) {
        boolean exists = registry.containsKey(new ResourceLocation("test_argument"));
        if (!exists && !SharedConstants.IS_RUNNING_IN_IDE) {
            register(registry, "test_argument", TestFunctionArgument.class, SingletonArgumentInfo.contextFree(TestFunctionArgument::testFunctionArgument));
            register(registry, "test_class", TestClassNameArgument.class, SingletonArgumentInfo.contextFree(TestClassNameArgument::testClassName));
        }
    }
}
