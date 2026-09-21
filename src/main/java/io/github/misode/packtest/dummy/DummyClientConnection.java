package io.github.misode.packtest.dummy;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A connection without a channel. Vanilla queues everything sent to a disconnected
 * connection until it comes up, which never happens for a dummy, so outbound traffic is
 * discarded instead of being retained forever.
 */
public class DummyClientConnection extends Connection {

    public DummyClientConnection(PacketFlow flow) {
        super(flow);
    }

    @Override
    public void send(@NotNull Packet<?> packet, @Nullable ChannelFutureListener listener, boolean flush) {}

    @Override
    public void runOnceConnected(@NotNull Consumer<Connection> action) {}

    @Override
    public void flushChannel() {}

    @Override
    public void setReadOnly() {}

    @Override
    public void handleDisconnection() {}

    @Override
    public void setListenerForServerboundHandshake(@NotNull PacketListener packetListener) {}

    @Override
    public <T extends PacketListener> void setupInboundProtocol(@NotNull ProtocolInfo<@NotNull T> protocolInfo, T packetListener) {}
}
