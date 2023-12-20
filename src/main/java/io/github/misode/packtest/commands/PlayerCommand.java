package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.misode.packtest.fake.FakePlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PlayerCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("player")
                .then(literal("spawn")
                        .executes(PlayerCommand::spawnRandomName)
                        .then(argument("player", StringArgumentType.word())
                                .executes(PlayerCommand::spawnWithName)))
        );
    }

    private static Optional<FakePlayer> getPlayer(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "player");
        return getPlayer(playerName, ctx);
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

    private static  int spawnWithName(CommandContext<CommandSourceStack> ctx) {
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
}
