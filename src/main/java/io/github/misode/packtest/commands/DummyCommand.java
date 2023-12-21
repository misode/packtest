package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DummyCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("dummy")
                .then(literal("spawn")
                        .executes(DummyCommand::spawnRandomName)
                        .then(argument("name", StringArgumentType.word())
                                .executes(DummyCommand::spawnFixedName)))
                .then(literal("leave")
                        .then(dummyName()
                                .executes(DummyCommand::leave)))
                .then(literal("respawn")
                        .then(dummyName()
                                .executes(DummyCommand::respawn)))
                .then(literal("jump")
                        .then(dummyName()
                                .executes(DummyCommand::jump)))
                .then(literal("sneak")
                        .then(dummyName()
                                .then(argument("active", BoolArgumentType.bool())
                                    .executes(DummyCommand::sneak))))
                .then(literal("sprint")
                        .then(dummyName()
                                .then(argument("active", BoolArgumentType.bool())
                                        .executes(DummyCommand::sprint))))
                .then(literal("drop")
                        .then(dummyName()
                                .executes(ctx -> dropMainhand(ctx, false))
                                .then(literal("all")
                                        .executes(ctx -> dropMainhand(ctx, true)))))
                .then(literal("swap")
                        .then(dummyName()
                                .executes(DummyCommand::swap)))
                .then(literal("selectslot")
                        .then(dummyName()
                                .then(argument("slot", IntegerArgumentType.integer(1, 9))
                                    .executes(DummyCommand::selectSlot))))
        );
    }

    private static RequiredArgumentBuilder<CommandSourceStack, String> dummyName() {
        return argument("name", StringArgumentType.word())
                .suggests((ctx, builder) -> {
                    PlayerList playerList = ctx.getSource().getServer().getPlayerList();
                    playerList.getPlayers().forEach(player -> {
                        if (player instanceof Dummy) {
                            builder.suggest(player.getName().getString());
                        }
                    });
                    return builder.buildFuture();
                });
    }

    private static Dummy getDummy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String playerName = StringArgumentType.getString(ctx, "name");
        return getDummy(playerName, ctx).orElseThrow(() -> new SimpleCommandExceptionType(() -> "Dummy " + playerName + " does not exist").create());
    }

    private static Optional<Dummy> getDummy(String playerName, CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        ServerPlayer player = server.getPlayerList().getPlayerByName(playerName);
        if (player instanceof Dummy testPlayer) {
            return Optional.of(testPlayer);
        }
        return Optional.empty();
    }

    private static int spawnRandomName(CommandContext<CommandSourceStack> ctx) {
        int tries = 0;
        while (tries++ < 10) {
            RandomSource random = ctx.getSource().getLevel().getRandom();
            String playerName = "Dummy" + random.nextInt(100, 1000);
            if (getDummy(playerName, ctx).isEmpty()) {
                return spawn(playerName, ctx);
            }
        }
        ctx.getSource().sendFailure(Component.literal("Failed to spawn dummy with a random name"));
        return 0;
    }

    private static  int spawnFixedName(CommandContext<CommandSourceStack> ctx) {
        String playerName = StringArgumentType.getString(ctx, "name");
        return spawn(playerName, ctx);
    }

    private static int spawn(String name, CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        if (getDummy(name, ctx).isPresent()) {
            source.sendFailure(Component.literal("Dummy " + name + " is already logged on"));
            return 0;
        }
        ResourceKey<Level> dimension = source.getLevel().dimension();
        Dummy.create(name, server, dimension, source.getPosition());
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        dummy.leave(Component.literal("Forced to leave"));
        return 1;
    }

    private static int respawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        dummy.respawn();
        return 1;
    }

    private static int jump(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        if (dummy.onGround()) {
            dummy.jumpFromGround();
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Dummy is not on the ground"));
        return 0;
    }

    private static int sneak(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        boolean toggle = BoolArgumentType.getBool(ctx, "active");
        if (dummy.isShiftKeyDown() != toggle) {
            dummy.setShiftKeyDown(toggle);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal(toggle ? "Dummy is already sneaking" : "Dummy is already not sneaking"));
        return 0;
    }

    private static int sprint(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        boolean toggle = BoolArgumentType.getBool(ctx, "active");
        if (dummy.isSprinting() != toggle) {
            dummy.setSprinting(toggle);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal(toggle ? "Dummy is already sprinting" : "Dummy is already not sprinting"));
        return 0;
    }

    private static int dropMainhand(CommandContext<CommandSourceStack> ctx, boolean stack) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        if (!dummy.getInventory().getSelected().is(Items.AIR)) {
            dummy.drop(stack);
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Dummy is not holding an item in their mainhand"));
        return 0;
    }

    private static int swap(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        ItemStack offhandItem = dummy.getItemInHand(InteractionHand.OFF_HAND);
        dummy.setItemInHand(InteractionHand.OFF_HAND, dummy.getItemInHand(InteractionHand.MAIN_HAND));
        dummy.setItemInHand(InteractionHand.MAIN_HAND, offhandItem);
        return 1;
    }

    private static int selectSlot(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        int slot = IntegerArgumentType.getInteger(ctx, "slot");
        if (dummy.getInventory().selected != slot - 1) {
            dummy.getInventory().selected = slot - 1;
            return 1;
        }
        ctx.getSource().sendFailure(Component.literal("Dummy already has slot " + slot + " selected"));
        return 0;
    }
}
