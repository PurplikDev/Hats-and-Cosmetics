package net.minecraft.network.protocol.login;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ServerboundCustomQueryPacket implements Packet<ServerLoginPacketListener>, net.minecraftforge.network.ICustomPacket<ServerboundCustomQueryPacket>
{
   private static final int MAX_PAYLOAD_SIZE = 1048576;
   private final int transactionId;
   @Nullable
   private final FriendlyByteBuf data;

   public ServerboundCustomQueryPacket(int pTransactionId, @Nullable FriendlyByteBuf pData) {
      this.transactionId = pTransactionId;
      this.data = pData;
   }

   public ServerboundCustomQueryPacket(FriendlyByteBuf pBuffer) {
      this.transactionId = pBuffer.readVarInt();
      if (pBuffer.readBoolean()) {
         int i = pBuffer.readableBytes();
         if (i < 0 || i > 1048576) {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
         }

         this.data = new FriendlyByteBuf(pBuffer.readBytes(i));
      } else {
         this.data = null;
      }

   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.transactionId);
      if (this.data != null) {
         pBuffer.writeBoolean(true);
         pBuffer.writeBytes(this.data.copy());
      } else {
         pBuffer.writeBoolean(false);
      }

   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerLoginPacketListener pHandler) {
      pHandler.handleCustomQueryPacket(this);
   }

   public int getTransactionId() {
      return this.transactionId;
   }

   @Nullable
   public FriendlyByteBuf getData() {
      return this.data;
   }
}
