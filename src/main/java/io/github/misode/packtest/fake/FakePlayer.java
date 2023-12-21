package io.github.misode.packtest.fake;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Heavily inspired by <a href="https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/patches/EntityPlayerMPFake.java">Carpet</a>
 */
public class FakePlayer extends ServerPlayer {
    public @Nullable BlockPos origin = null;
    public Runnable fixStartingPosition = () -> {};

    public static void create(String username, MinecraftServer server, ResourceKey<Level> dimensionId, Vec3 pos) {
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
        FakePlayer instance = new FakePlayer(server, level, profile, ClientInformation.createDefault());
        instance.origin = BlockPos.containing(pos);
        instance.fixStartingPosition = () -> instance.moveTo(pos.x, pos.y, pos.z, 0, 0);
        server.getPlayerList().placeNewPlayer(
                new FakeClientConnection(PacketFlow.SERVERBOUND),
                instance,
                new CommonListenerCookie(profile, 0, instance.clientInformation()));
        instance.teleportTo(level, pos.x, pos.y, pos.z, 0, 0);
        instance.setHealth(20);
        instance.unsetRemoved();
        instance.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        server.getPlayerList().broadcastAll(new ClientboundRotateHeadPacket(instance, (byte) (instance.yHeadRot * 256 / 360)), dimensionId);
        server.getPlayerList().broadcastAll(new ClientboundTeleportEntityPacket(instance), dimensionId);
        instance.entityData.set(DATA_PLAYER_MODE_CUSTOMISATION, (byte) 0x7f);
    }

    public FakePlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli) {
        super(server, level, profile, cli);
    }

    public void leave(Component reason) {
        server.getPlayerList().remove(this);
        this.connection.onDisconnect(reason);
    }

    public void respawn() {
        server.getPlayerList().respawn(this, false);
    }

    @Override
    public void tick() {
        if (Objects.requireNonNull(this.getServer()).getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.serverLevel().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {}
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
