package net.minecraft.network.protocol.login;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.SerializableUUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundGameProfilePacket implements Packet<ClientLoginPacketListener> {
   private final GameProfile gameProfile;

   public ClientboundGameProfilePacket(GameProfile pGameProfile) {
      this.gameProfile = pGameProfile;
   }

   public ClientboundGameProfilePacket(FriendlyByteBuf pBuffer) {
      int[] aint = new int[4];

      for(int i = 0; i < aint.length; ++i) {
         aint[i] = pBuffer.readInt();
      }

      UUID uuid = SerializableUUID.uuidFromIntArray(aint);
      String s = pBuffer.readUtf(16);
      this.gameProfile = new GameProfile(uuid, s);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      for(int i : SerializableUUID.uuidToIntArray(this.gameProfile.getId())) {
         pBuffer.writeInt(i);
      }

      pBuffer.writeUtf(this.gameProfile.getName());
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientLoginPacketListener pHandler) {
      pHandler.handleGameProfile(this);
   }

   public GameProfile getGameProfile() {
      return this.gameProfile;
   }
}