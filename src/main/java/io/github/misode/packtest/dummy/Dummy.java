package io.github.misode.packtest.dummy;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

/**
 * Heavily inspired by <a href="https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/patches/EntityPlayerMPFake.java">Carpet</a>
 */
public class Dummy extends ServerPlayer {
    public Vec3 originalSpawn;

    public static Dummy createRandom(MinecraftServer server, ResourceKey<Level> dimensionId, Vec3 pos) {
        RandomSource random = server.overworld().getRandom();
        int tries = 0;
        while (tries++ < 10) {
            String playerName = "Dummy" + random.nextInt(100, 1000);
            if (server.getPlayerList().getPlayerByName(playerName) == null) {
                return create(playerName, server, dimensionId, pos);
            }
        }
        throw new IllegalStateException("Failed to spawn dummy with a random name");
    }

    public static Dummy create(String username, MinecraftServer server, ResourceKey<Level> dimensionId, Vec3 pos) {
        ServerLevel level = server.getLevel(dimensionId);
        GameProfileCache.setUsesAuthentication(false);
        GameProfile profile;
        try {
            var profileCache = server.getProfileCache();
            profile = profileCache == null ? null : profileCache.get(username).orElse(null);
        }
        finally {
            GameProfileCache.setUsesAuthentication(server.isDedicatedServer() && server.usesAuthentication());
        }
        if (profile == null) {
            profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(username), username);
        }
        Vec3 originalSpawn = Vec3.atBottomCenterOf(BlockPos.containing(pos));
        Dummy dummy = new Dummy(server, level, profile, ClientInformation.createDefault(), originalSpawn);
        server.getPlayerList().placeNewPlayer(
                new DummyClientConnection(PacketFlow.SERVERBOUND),
                dummy,
                new CommonListenerCookie(profile, 0, dummy.clientInformation(), false));
        dummy.teleportTo(level, originalSpawn.x, originalSpawn.y, originalSpawn.z, Set.of(), 0, 0, true);
        dummy.setHealth(20);
        dummy.unsetRemoved();
        dummy.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(dummy, (byte) (dummy.yHeadRot * 256 / 360)), dimensionId);
        server.getPlayerList().broadcastAll(ClientboundEntityPositionSyncPacket.of(dummy), dimensionId);
        dummy.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
        return dummy;
    }

    public Dummy(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli, Vec3 originalSpawn) {
        super(server, level, profile, cli);
        this.originalSpawn = originalSpawn;
    }

    public String getUsername() {
        return this.getGameProfile().getName();
    }

    public void leave(Component reason) {
        Objects.requireNonNull(this.getServer()).getPlayerList().remove(this);
        this.connection.onDisconnect(new DisconnectionDetails(reason));
    }

    public void respawn() {
        Objects.requireNonNull(this.getServer()).getPlayerList().respawn(this, false, Entity.RemovalReason.KILLED);
    }

    @Override
    public void forceSetRotation(float f, float g) {
        this.setYRot(f);
        this.setXRot(g);
        this.setOldRot();

        this.connection.send(new ServerboundMovePlayerPacket.Rot(f, g, false, false));
    }

    @SuppressWarnings("resource")
    @Override
    public void tick() {
        if (Objects.requireNonNull(this.getServer()).getTickCount() % 10 == 0) {
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
    public void die(DamageSource cause) {
        super.die(cause);
        if (this.level().getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN)) {
            Objects.requireNonNull(this.getServer()).schedule(new TickTask(this.getServer().getTickCount(),
                    () -> this.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN))
            ));
        }
    }

    @Override
    public void onEquipItem(final EquipmentSlot slot, final ItemStack previous, final ItemStack stack) {
        if (!isUsingItem()) super.onEquipItem(slot, previous, stack);
    }

    @Override
    public @NotNull String getIpAddress() {
        return "127.0.0.1";
    }
}
