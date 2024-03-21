package io.github.misode.packtest;

import net.minecraft.commands.arguments.item.ItemPredicateArgument;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public record PackTestItemPredicate(Predicate<ItemStack> predicate, String source) implements ItemPredicateArgument.Result {
    @Override
    public boolean test(ItemStack itemStack) {
        return predicate.test(itemStack);
    }
}
