package io.github.misode.packtest.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class DirectionArgument implements ArgumentType<Direction> {
    private static final DynamicCommandExceptionType ERROR_UNKNOWN_DIRECTION = new DynamicCommandExceptionType(
            dir -> Component.literal("Unknown direction " + dir)
    );

    public static DirectionArgument direction() {
        return new DirectionArgument();
    }

    public static Direction getDirection(CommandContext<CommandSourceStack> ctx, String name) {
        return ctx.getArgument(name, Direction.class);
    }

    @Override
    public Direction parse(StringReader reader) throws CommandSyntaxException {
        String str = reader.readUnquotedString();
        Direction direction = Direction.byName(str);
        if (direction == null) {
            throw ERROR_UNKNOWN_DIRECTION.create(str);
        }
        return direction;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(Arrays.stream(Direction.values()).map(Direction::getName), builder);
    }
}
