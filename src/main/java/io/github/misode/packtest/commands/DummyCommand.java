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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
                .then(literal("use")
                        .then(dummyName()
                                .then(literal("item")
                                        .executes(DummyCommand::useItem))
                                .then(literal("block")
                                        .then(argument("pos", Vec3Argument.vec3(false))
                                                .executes(ctx -> useBlock(ctx, Direction.UP))
                                                .then(argument("direction", DirectionArgument.direction())
                                                        .executes(ctx -> useBlock(ctx, DirectionArgument.getDirection(ctx, "direction"))))))
                                .then(literal("entity")
                                        .then(argument("entity", EntityArgument.entity())
                                                .executes(ctx -> useEntity(ctx, null))
                                                .then(argument("pos", Vec3Argument.vec3(false))
                                                        .executes(ctx -> useEntity(ctx, Vec3Argument.getVec3(ctx, "pos"))))))))
                .then(literal("attack")
                        .then(dummyName()
                                .then(argument("entity", EntityArgument.entity())
                                        .executes(DummyCommand::attackEntity))))
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

    private static int useItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack handItem = dummy.getItemInHand(hand);
            if (dummy.gameMode.useItem(dummy, dummy.level(), handItem, hand).consumesAction()) {
                return 1;
            }
        }
        ctx.getSource().sendFailure(Component.literal("Dummy cannot use that item"));
        return 0;
    }

    private static int useBlock(CommandContext<CommandSourceStack> ctx, Direction hitDirection) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack handItem = dummy.getItemInHand(hand);
            BlockHitResult blockHit = new BlockHitResult(pos, hitDirection, BlockPos.containing(pos), false);
            InteractionResult result = dummy.gameMode.useItemOn(dummy, dummy.serverLevel(), handItem, hand, blockHit);
            if (result.consumesAction()) {
                if (result.shouldSwing()) dummy.swing(hand);
                return 1;
            }
        }
        ctx.getSource().sendFailure(Component.literal("Dummy cannot interact with that block"));
        return 0;
    }

    private static int useEntity(CommandContext<CommandSourceStack> ctx, Vec3 pos) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        Entity entity = EntityArgument.getEntity(ctx, "entity");
        if (pos == null) {
            pos = entity.position();
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (entity.interactAt(dummy, pos, hand).consumesAction()) {
                return 1;
            }
            if (dummy.interactOn(entity, hand).consumesAction()) {
                return 1;
            }
        }
        ctx.getSource().sendFailure(Component.literal("Dummy cannot interact with that entity"));
        return 0;
    }

    private static int attackEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        Entity entity = EntityArgument.getEntity(ctx, "entity");
        dummy.attack(entity);
        dummy.swing(InteractionHand.MAIN_HAND);
        return 1;
    }
}
