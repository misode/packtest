package io.github.misode.packtest.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.misode.packtest.PackTestPlayerName;
import io.github.misode.packtest.dummy.Dummy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class DummyCommand {

    public static final SuggestionProvider<CommandSourceStack> SUGGEST_DIRECTION = (ctx, builder) -> {
        for (var d : Direction.values()) {
            builder.suggest(d.getName());
        }
        return builder.buildFuture();
    };
    public static final SuggestionProvider<CommandSourceStack> SUGGEST_DUMMY_NAME = (ctx, builder) -> {
        builder.suggest("@s");
        PlayerList playerList = ctx.getSource().getServer().getPlayerList();
        playerList.getPlayers().forEach(player -> {
            if (player instanceof Dummy dummy) {
                builder.suggest(dummy.getUsername());
            }
        });
        return builder.buildFuture();
    };
    private static final SimpleCommandExceptionType ERROR_DUMMY_NOT_FOUND = new SimpleCommandExceptionType(
            Component.literal("No dummy was found")
    );
    private static final SimpleCommandExceptionType ERROR_NO_NAME = new SimpleCommandExceptionType(
            Component.literal("Cannot spawn dummy without a name")
    );
    private static final SimpleCommandExceptionType ERROR_INVALID_DIRECTION = new SimpleCommandExceptionType(
            Component.literal("Not a valid direction")
    );
    private static final DynamicCommandExceptionType ERROR_DUMMY_EXISTS = createError("is already logged on");
    private static final DynamicCommandExceptionType ERROR_PLAYER_EXISTS = createError("is already a player");
    private static final DynamicCommandExceptionType ERROR_NOT_ON_GROUND = createError("is not on the ground");
    private static final DynamicCommandExceptionType ERROR_SNEAKING = createError("is already sneaking");
    private static final DynamicCommandExceptionType ERROR_NOT_SNEAKING = createError("is already not sneaking");
    private static final DynamicCommandExceptionType ERROR_SPRINTING = createError("is already sprinting");
    private static final DynamicCommandExceptionType ERROR_NOT_SPRINTING = createError("is already not sprinting");
    private static final DynamicCommandExceptionType ERROR_NOT_HOLDING_ITEM = createError("is not holding an item in their mainhand");
    private static final Dynamic2CommandExceptionType ERROR_SLOT_SELECTED = new Dynamic2CommandExceptionType(
            (name, slot) -> Component.literal("Dummy " + name + " already has slot " + slot + " selected")
    );
    private static final DynamicCommandExceptionType ERROR_USE_ITEM = createError("cannot use that item");
    private static final DynamicCommandExceptionType ERROR_INTERACT_BLOCK = createError("cannot interact with that block");
    private static final DynamicCommandExceptionType ERROR_INTERACT_ENTITY = createError("cannot interact with that entity");
    private static final DynamicCommandExceptionType ERROR_MINE_BLOCK = createError("failed to mine block");

    private static DynamicCommandExceptionType createError(String message) {
        return new DynamicCommandExceptionType(name -> Component.literal("Dummy " + name + " " + message));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("dummy")
                .then(argument("dummy", EntityArgument.entity())
                        .suggests(SUGGEST_DUMMY_NAME)
                        .then(literal("spawn")
                                .executes(DummyCommand::spawn))
                        .then(literal("leave")
                                .executes(DummyCommand::leave))
                        .then(literal("respawn")
                                .executes(DummyCommand::respawn))
                        .then(literal("jump")
                                .executes(DummyCommand::jump))
                        .then(literal("sneak")
                                .then(argument("active", BoolArgumentType.bool())
                                    .executes(DummyCommand::sneak)))
                        .then(literal("sprint")
                                .then(argument("active", BoolArgumentType.bool())
                                        .executes(DummyCommand::sprint)))
                        .then(literal("drop")
                                .executes(ctx -> dropMainhand(ctx, false))
                                .then(literal("all")
                                        .executes(ctx -> dropMainhand(ctx, true))))
                        .then(literal("swap")
                                .executes(DummyCommand::swap))
                        .then(literal("selectslot")
                                .then(argument("slot", IntegerArgumentType.integer(1, 9))
                                    .executes(DummyCommand::selectSlot)))
                        .then(literal("use")
                                .then(literal("item")
                                        .executes(DummyCommand::useItem))
                                .then(literal("block")
                                        .then(argument("pos", Vec3Argument.vec3(false))
                                                .executes(ctx -> useBlock(ctx, Direction.UP))
                                                .then(argument("direction", StringArgumentType.word())
                                                        .suggests(SUGGEST_DIRECTION)
                                                        .executes(ctx -> useBlock(ctx, Direction.byName(StringArgumentType.getString(ctx, "direction")))))))
                                .then(literal("entity")
                                        .then(argument("entity", EntityArgument.entity())
                                                .executes(ctx -> useEntity(ctx, null))
                                                .then(argument("pos", Vec3Argument.vec3(false))
                                                        .executes(ctx -> useEntity(ctx, Vec3Argument.getVec3(ctx, "pos")))))))
                        .then(literal("attack")
                                .then(argument("entity", EntityArgument.entity())
                                        .executes(DummyCommand::attackEntity)))
                        .then(literal("mine")
                                .then(argument("pos", BlockPosArgument.blockPos())
                                        .executes(DummyCommand::mineBlock)))
        ));
    }

    private static Dummy getDummy(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        EntitySelector selector = ctx.getArgument("dummy", EntitySelector.class);
        ServerPlayer player;
        try {
            player = selector.findSinglePlayer(ctx.getSource());
        } catch (CommandSyntaxException e) {
            throw ERROR_DUMMY_NOT_FOUND.create();
        }
        if (player instanceof Dummy dummy) {
            return dummy;
        }
        throw ERROR_DUMMY_NOT_FOUND.create();
    }

    private static int spawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        EntitySelector selector = ctx.getArgument("dummy", EntitySelector.class);
        String name = ((PackTestPlayerName)selector).packtest$getPlayerName();
        if (name == null) {
            throw ERROR_NO_NAME.create();
        }
        CommandSourceStack source = ctx.getSource();
        MinecraftServer server = source.getServer();
        ServerPlayer player = server.getPlayerList().getPlayerByName(name);
        if (player instanceof Dummy) {
            throw ERROR_DUMMY_EXISTS.create(name);
        }
        if (player != null) {
            throw ERROR_PLAYER_EXISTS.create(name);
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
        if (!dummy.onGround()) {
            throw ERROR_NOT_ON_GROUND.create(dummy.getUsername());
        }
        dummy.jumpFromGround();
        return 1;
    }

    private static int sneak(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        boolean active = BoolArgumentType.getBool(ctx, "active");
        if (dummy.isShiftKeyDown() == active) {
            throw (active ? ERROR_SNEAKING : ERROR_NOT_SNEAKING).create(dummy.getUsername());
        }
        dummy.setShiftKeyDown(active);
        return 1;
    }

    private static int sprint(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        boolean active = BoolArgumentType.getBool(ctx, "active");
        if (dummy.isSprinting() == active) {
            throw (active ? ERROR_SPRINTING : ERROR_NOT_SPRINTING).create(dummy.getUsername());
        }
        dummy.setSprinting(active);
        return 1;
    }

    private static int dropMainhand(CommandContext<CommandSourceStack> ctx, boolean stack) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        if (dummy.getInventory().getSelectedItem().isEmpty()) {
            throw ERROR_NOT_HOLDING_ITEM.create(dummy.getUsername());
        }
        dummy.drop(stack);
        return 1;
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
        if (dummy.getInventory().getSelectedSlot() == slot - 1) {
            throw ERROR_SLOT_SELECTED.create(dummy.getUsername(), slot);
        }
        dummy.getInventory().setSelectedSlot(slot - 1);
        return 1;
    }

    private static int useItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack handItem = dummy.getItemInHand(hand);
            if (dummy.gameMode.useItem(dummy, dummy.level(), handItem, hand).consumesAction()) {
                return 1;
            }
        }
        throw ERROR_USE_ITEM.create(dummy.getUsername());
    }

    private static int useBlock(CommandContext<CommandSourceStack> ctx, Direction hitDirection) throws CommandSyntaxException {
        if (hitDirection == null) {
            throw ERROR_INVALID_DIRECTION.create();
        }
        Dummy dummy = getDummy(ctx);
        Vec3 pos = Vec3Argument.getVec3(ctx, "pos");
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack handItem = dummy.getItemInHand(hand);
            BlockHitResult blockHit = new BlockHitResult(pos, hitDirection, BlockPos.containing(pos), false);
            InteractionResult result = dummy.gameMode.useItemOn(dummy, dummy.level(), handItem, hand, blockHit);
            if (result.consumesAction()) {
                dummy.swing(hand);
                return 1;
            }
        }
        throw ERROR_INTERACT_BLOCK.create(dummy.getUsername());
    }

    @SuppressWarnings("SameReturnValue")
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
        throw ERROR_INTERACT_ENTITY.create(dummy.getUsername());
    }

    private static int attackEntity(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        Entity entity = EntityArgument.getEntity(ctx, "entity");
        dummy.attack(entity);
        dummy.swing(InteractionHand.MAIN_HAND);
        return 1;
    }

    private static int mineBlock(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Dummy dummy = getDummy(ctx);
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        if (!dummy.gameMode.destroyBlock(pos)) {
            throw ERROR_MINE_BLOCK.create(dummy.getUsername());
        }
        return 1;
    }
}
