package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

public class ClientboundChatPacket implements Packet<ClientGamePacketListener> {
   private final Component message;
   private final ChatType type;
   private final UUID sender;

   public ClientboundChatPacket(Component pMessage, ChatType pType, UUID pSender) {
      this.message = pMessage;
      this.type = pType;
      this.sender = pSender;
   }

   public ClientboundChatPacket(FriendlyByteBuf pBuffer) {
      this.message = pBuffer.readComponent();
      this.type = ChatType.getForIndex(pBuffer.readByte());
      this.sender = pBuffer.readUUID();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeComponent(this.message);
      pBuffer.writeByte(this.type.getIndex());
      pBuffer.writeUUID(this.sender);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleChat(this);
   }

   public Component getMessage() {
      return this.message;
   }

   public ChatType getType() {
      return this.type;
   }

   public UUID getSender() {
      return this.sender;
   }

   /**
    * Whether decoding errors will be ignored for this packet.
    */
   public boolean isSkippable() {
      return true;
   }
}