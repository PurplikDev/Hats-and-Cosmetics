package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.InteractionHand;

public class ServerboundUseItemPacket implements Packet<ServerGamePacketListener> {
   private final InteractionHand hand;

   public ServerboundUseItemPacket(InteractionHand pHand) {
      this.hand = pHand;
   }

   public ServerboundUseItemPacket(FriendlyByteBuf pBuffer) {
      this.hand = pBuffer.readEnum(InteractionHand.class);
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeEnum(this.hand);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ServerGamePacketListener pHandler) {
      pHandler.handleUseItem(this);
   }

   public InteractionHand getHand() {
      return this.hand;
   }
}