package io.github.misode.packtest.dummy;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.PacketFlow;
import org.jetbrains.annotations.NotNull;

public class DummyClientConnection extends Connection {

    public DummyClientConnection(PacketFlow flow) {
        super(flow);
    }

    @Override
    public void setReadOnly() {}

    @Override
    public void handleDisconnection() {}

    @Override
    public void setListenerForServerboundHandshake(@NotNull PacketListener packetListener) {}

    @Override
    public <T extends PacketListener> void setupInboundProtocol(@NotNull ProtocolInfo<@NotNull T> protocolInfo, T packetListener) {}
}
