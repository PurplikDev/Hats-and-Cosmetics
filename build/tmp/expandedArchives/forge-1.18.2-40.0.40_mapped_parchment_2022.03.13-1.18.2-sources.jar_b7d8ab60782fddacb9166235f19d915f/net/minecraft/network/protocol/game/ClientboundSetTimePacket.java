package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundSetTimePacket implements Packet<ClientGamePacketListener> {
   private final long gameTime;
   private final long dayTime;

   public ClientboundSetTimePacket(long pGameTime, long pDayTime, boolean pDaylightCycleEnabled) {
      this.gameTime = pGameTime;
      long i = pDayTime;
      if (!pDaylightCycleEnabled) {
         i = -pDayTime;
         if (i == 0L) {
            i = -1L;
         }
      }

      this.dayTime = i;
   }

   public ClientboundSetTimePacket(FriendlyByteBuf p_179387_) {
      this.gameTime = p_179387_.readLong();
      this.dayTime = p_179387_.readLong();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeLong(this.gameTime);
      pBuffer.writeLong(this.dayTime);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleSetTime(this);
   }

   public long getGameTime() {
      return this.gameTime;
   }

   public long getDayTime() {
      return this.dayTime;
   }
}