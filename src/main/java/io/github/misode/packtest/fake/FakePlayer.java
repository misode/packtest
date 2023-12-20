package io.github.misode.packtest.fake;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Heavily inspired by <a href="https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/patches/EntityPlayerMPFake.java">Carpet</a>
 */
public class FakePlayer extends ServerPlayer {
    public Runnable fixStartingPosition = () -> {};

    public static void create(String username, MinecraftServer server, ResourceKey<Level> dimensionId, Vec3 pos) {
        ServerLevel level = server.getLevel(dimensionId);
        GameProfileCache.setUsesAuthentication(false);
        GameProfile gameProfile;
        try {
            var profileCache = server.getProfileCache();
            gameProfile = profileCache == null ? null : profileCache.get(username).orElse(null);
        }
        finally {
            GameProfileCache.setUsesAuthentication(server.isDedicatedServer() && server.usesAuthentication());
        }
        if (gameProfile == null) {
            gameProfile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(username), username);
        }
        GameProfile finalProfile = gameProfile;
        SkullBlockEntity.fetchGameProfile(gameProfile.getName()).thenAccept(p -> {
            GameProfile profile = p.orElse(finalProfile);
            FakePlayer instance = new FakePlayer(server, level, profile, ClientInformation.createDefault());
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
        });
    }

    private FakePlayer(MinecraftServer server, ServerLevel level, GameProfile profile, ClientInformation cli) {
        super(server, level, profile, cli);
    }
}
