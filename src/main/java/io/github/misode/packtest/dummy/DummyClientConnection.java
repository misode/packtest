package io.github.misode.packtest.dummy;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.PacketFlow;

public class DummyClientConnection extends Connection {

    public DummyClientConnection(PacketFlow flow) {
        super(flow);
    }

    @Override
    public void setReadOnly() {}

    @Override
    public void handleDisconnection() {}

    @Override
    public void setListener(PacketListener packetListener) {}
}
