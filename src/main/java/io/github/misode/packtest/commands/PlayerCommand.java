package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
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
                        .then(playerArgument()
                                .executes(PlayerCommand::leave)))
                .then(literal("respawn")
                        .then(playerArgument()
                                .executes(PlayerCommand::respawn)))
                .then(literal("jump")
                        .then(playerArgument()
                                .executes(PlayerCommand::jump)))
                .then(literal("sneak")
                        .then(playerArgument()
                                .then(argument("active", BoolArgumentType.bool())
                                    .executes(PlayerCommand::sneak))))
                .then(literal("sprint")
                    .then(playerArgument()
                            .then(argument("active", BoolArgumentType.bool())
                                    .executes(PlayerCommand::sprint))))
        );
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> playerArgument() {
        return argument("player", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    PlayerList playerList = ctx.getSource().getServer().getPlayerList();
                    playerList.getPlayers().forEach(player -> {
                        if (player instanceof FakePlayer) {
                            builder.suggest(player.getName().getString());
                        }
                    });
                    return builder.buildFuture();
                });
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
        player.respawn();
        return 1;
    }

    private static int jump(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FakePlayer player = getPlayer(ctx);
        if (player.onGround()) {
            player.jumpFromGround();
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Player is not on the ground"));
        return 0;
    }

    private static int sneak(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FakePlayer player = getPlayer(ctx);
        boolean toggle = BoolArgumentType.getBool(ctx, "active");
        if (player.isShiftKeyDown() != toggle) {
            player.setShiftKeyDown(toggle);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal(toggle ? "Player is already sneaking" : "Player is already not sneaking"));
        return 0;
    }

    private static int sprint(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FakePlayer player = getPlayer(ctx);
        boolean toggle = BoolArgumentType.getBool(ctx, "active");
        if (player.isSprinting() != toggle) {
            player.setSprinting(toggle);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal(toggle ? "Player is already sprinting" : "Player is already not sprinting"));
        return 0;
    }
}
