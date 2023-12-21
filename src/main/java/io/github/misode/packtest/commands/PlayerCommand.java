package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.misode.packtest.fake.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PlayerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("player")
                .then(literal("spawn")
                        .executes(PlayerCommand::spawnRandomName)
                        .then(argument("player", StringArgumentType.word())
                                .executes(PlayerCommand::spawnFixedName)))
                .then(literal("leave")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(PlayerCommand::listFakePlayers)
                                .executes(PlayerCommand::leave)))
                .then(literal("respawn")
                        .then(argument("player", StringArgumentType.word())
                                .suggests(PlayerCommand::listFakePlayers)
                                .executes(PlayerCommand::respawn)))
        );
    }

    private static CompletableFuture<Suggestions> listFakePlayers(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        PlayerList playerList = ctx.getSource().getServer().getPlayerList();
        playerList.getPlayers().forEach(player -> {
            if (player instanceof FakePlayer) {
                builder.suggest(player.getName().getString());
            }
        });
        return builder.buildFuture();
    }

    private static FakePlayer getPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(ctx, "player");
        return getPlayer(playerName, ctx).orElseThrow(() -> new SimpleCommandExceptionType(() -> "Fake player " + playerName + " does not exist").create());
    }

    private static Optional<FakePlayer> getPlayer(String playerName, CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
        if (player instanceof FakePlayer fakePlayer) {
            return Optional.of(fakePlayer);
        }
        return Optional.empty();
    }

    private static int spawnRandomName(CommandContext<CommandSourceStack> ctx) {
        int tries = 0;
        while (tries++ < 10) {
            RandomSource random = ctx.getSource().getLevel().getRandom();
            String playerName = "Tester" + random.nextInt(100, 1000);
            if (getPlayer(playerName, ctx).isEmpty()) {
                return spawn(playerName, ctx);
            }
        }
        ctx.getSource().sendFailure(Component.literal("Failed to spawn player with a random name"));
        return 0;
    }

    private static  int spawnFixedName(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        return spawn(playerName, ctx);
    }

    private static int spawn(String playerName, CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        if (getPlayer(playerName, ctx).isPresent()) {
            source.sendFailure(Component.literal("Player " + playerName + " is already logged on"));
            return 0;
        }
        ResourceKey<Level> dimension = source.getLevel().dimension();
        FakePlayer.create(playerName, server, dimension, source.getPosition());
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FakePlayer player = getPlayer(ctx);
        player.leave(Component.literal("Forced to leave"));
        return 1;
    }

    private static int respawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FakePlayer player = getPlayer(ctx);
        player.respawn(ctx.getSource().getPosition());
        return 1;
    }
}
