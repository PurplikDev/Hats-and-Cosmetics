package net.minecraft.network.protocol.status;

import net.minecraft.network.PacketListener;

/**
 * PacketListener for the server side of the STATUS protocol.
 */
public interface ServerStatusPacketListener extends PacketListener {
   void handlePingRequest(ServerboundPingRequestPacket pPacket);

   void handleStatusRequest(ServerboundStatusRequestPacket pPacket);
}