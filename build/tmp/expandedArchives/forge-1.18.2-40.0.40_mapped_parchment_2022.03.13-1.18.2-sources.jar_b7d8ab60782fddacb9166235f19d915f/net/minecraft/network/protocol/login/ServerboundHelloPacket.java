package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundHelloPacket implements Packet<ServerLoginPacketListener> {
   private final GameProfile gameProfile;

   public ServerboundHelloPacket(GameProfile pGameProfile) {
      this.gameProfile = pGameProfile;
   }

   public ServerboundHelloPacket(FriendlyByteBuf pBuffer) {
      this.gameProfile = new GameProfile((UUID)null, pBuffer.readUtf(16));
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeUtf(this.gameProfile.getName());
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleHello(this);
   }

   public GameProfile getGameProfile() {
      return this.gameProfile;
   }
}