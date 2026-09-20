package io.github.misode.packtest.dummy;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * Heavily inspired by <a href="https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/patches/EntityPlayerMPFake.java">Carpet</a>
 */
public class Dummy extends ServerPlayer {
    public final Vec3 spawnPosition;
    public final Vec2 spawnRotation;

    public static Dummy createRandom(ServerLevel level, Vec3 pos, Vec2 rot) {
        PlayerList playerList = level.getServer().getPlayerList();
        int tries = 0;
        while (tries++ < 10) {
            String playerName = "Dummy" + level.getRandom().nextInt(100, 1000);
            if (playerList.getPlayerByName(playerName) == null) {
                return create(playerName, level, pos, rot);
            }
        }
        throw new IllegalStateException("Failed to spawn dummy with a random name");
    }

    public static Dummy create(String username, ServerLevel level, Vec3 pos, Vec2 rot) {
        MinecraftServer server = level.getServer();
        UUID id = UUID.randomUUID();
        GameProfile profile = new GameProfile(id, username);
        Vec3 originalSpawn = Vec3.atBottomCenterOf(BlockPos.containing(pos));
        Dummy dummy = new Dummy(server, level, profile, ClientInformation.createDefault(), pos, rot);
        server.getPlayerList().placeNewPlayer(
                new DummyClientConnection(PacketFlow.SERVERBOUND),
                dummy,
                new CommonListenerCookie(profile, 0, dummy.clientInformation(), false));
        dummy.teleportTo(level, originalSpawn.x, originalSpawn.y, originalSpawn.z, Set.of(), rot.y, rot.x, true);
        dummy.setHealth(20);
        dummy.unsetRemoved();
        dummy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(dummy, (byte) (dummy.yHeadRot * 256 / 360)), level.dimension());
        server.getPlayerList().broadcastAll(ClientboundEntityPositionSyncPacket.of(dummy), level.dimension());
        dummy.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
        return dummy;
    }

    public Dummy(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli, Vec3 pos, Vec2 rot) {
        super(server, level, profile, cli);
        this.spawnPosition = pos;
        this.spawnRotation = rot;
    }

    public String getUsername() {
        return this.getGameProfile().name();
    }

    @SuppressWarnings("resource")
    public void leave(Component reason) {
        Objects.requireNonNull(this.level().getServer()).getPlayerList().remove(this);
        this.connection.onDisconnect(new DisconnectionDetails(reason));
    }

    @SuppressWarnings("resource")
    public void respawn() {
        Objects.requireNonNull(this.level().getServer()).getPlayerList().respawn(this, false, Entity.RemovalReason.KILLED);
    }

    @Override
    public @NotNull BlockPos adjustSpawnLocation(@NonNull ServerLevel serverLevel, @NonNull BlockPos blockPos) {
        return BlockPos.containing(this.spawnPosition);
    }

    @SuppressWarnings("resource")
    @Override
    public void tick() {
        if (Objects.requireNonNull(this.level().getServer()).getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.level().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {}
    }

    @SuppressWarnings("resource")
    @Override
    public void die(@NonNull DamageSource cause) {
        super.die(cause);
        if (this.level().getGameRules().get(GameRules.IMMEDIATE_RESPAWN)) {
            MinecraftServer server = Objects.requireNonNull(this.level().getServer());
            server.schedule(new TickTask(server.getTickCount(),
                    () -> this.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN))
            ));
        }
    }

    @Override
    public void onEquipItem(final @NonNull EquipmentSlot slot, final @NonNull ItemStack previous, final @NonNull ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public @NotNull String getIpAddress() {
        return "127.0.0.1";
    }
}
