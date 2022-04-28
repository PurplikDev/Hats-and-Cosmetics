package net.minecraft.network.protocol.login;

import net.minecraft.network.PacketListener;

/**
 * PacketListener for the server side of the LOGIN protocol.
 */
public interface ServerLoginPacketListener extends PacketListener {
   void handleHello(ServerboundHelloPacket pPacket);

   void handleKey(ServerboundKeyPacket pPacket);

   void handleCustomQueryPacket(ServerboundCustomQueryPacket pPacket);
}