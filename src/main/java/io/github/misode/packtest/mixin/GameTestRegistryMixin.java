package io.github.misode.packtest.mixin;

import com.google.common.collect.Sets;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.misode.packtest.PackTestLibrary;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(GameTestRegistry.class)
public class GameTestRegistryMixin {

	@ModifyExpressionValue(method = "getAllTestFunctions", at = @At(value = "FIELD", target = "Lnet/minecraft/gametest/framework/GameTestRegistry;TEST_FUNCTIONS:Ljava/util/Collection;"))
	private static Collection<TestFunction> getAllTestFunctions(Collection<TestFunction> original) {
		return Stream.concat(original.stream(), PackTestLibrary.INSTANCE.getAllTestFunctions().stream())
				.collect(Collectors.toList());
	}

	@ModifyExpressionValue(method = "getAllTestClassNames", at = @At(value = "FIELD", target = "Lnet/minecraft/gametest/framework/GameTestRegistry;TEST_CLASS_NAMES:Ljava/util/Set;"))
	private static Set<String> getAllTestClassNames(Set<String> original) {
		Set<String> allNames = Sets.newHashSet();
		allNames.addAll(original);
		allNames.addAll(PackTestLibrary.INSTANCE.getAllTestClassNames());
		return allNames;
	}

	@Inject(method = "isTestClass", at = @At("HEAD"), cancellable = true)
	private static void isTestClass(String string, CallbackInfoReturnable<Boolean> ci) {
		if (PackTestLibrary.INSTANCE.getAllTestClassNames().contains(string)) {
			ci.setReturnValue(true);
		}
	}

	@ModifyExpressionValue(method = "getTestFunctionsForClassName", at = @At(value = "FIELD", target = "Lnet/minecraft/gametest/framework/GameTestRegistry;TEST_FUNCTIONS:Ljava/util/Collection;"))
	private static Collection<TestFunction> getTestFunctionsForClassName(Collection<TestFunction> original) {
		return Stream.concat(original.stream(), PackTestLibrary.INSTANCE.getAllTestFunctions().stream())
				.collect(Collectors.toList());
	}
}
