package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;

public class ClientboundTeleportEntityPacket implements Packet<ClientGamePacketListener> {
   private final int id;
   private final double x;
   private final double y;
   private final double z;
   private final byte yRot;
   private final byte xRot;
   private final boolean onGround;

   public ClientboundTeleportEntityPacket(Entity pEntity) {
      this.id = pEntity.getId();
      this.x = pEntity.getX();
      this.y = pEntity.getY();
      this.z = pEntity.getZ();
      this.yRot = (byte)((int)(pEntity.getYRot() * 256.0F / 360.0F));
      this.xRot = (byte)((int)(pEntity.getXRot() * 256.0F / 360.0F));
      this.onGround = pEntity.isOnGround();
   }

   public ClientboundTeleportEntityPacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readVarInt();
      this.x = pBuffer.readDouble();
      this.y = pBuffer.readDouble();
      this.z = pBuffer.readDouble();
      this.yRot = pBuffer.readByte();
      this.xRot = pBuffer.readByte();
      this.onGround = pBuffer.readBoolean();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.id);
      pBuffer.writeDouble(this.x);
      pBuffer.writeDouble(this.y);
      pBuffer.writeDouble(this.z);
      pBuffer.writeByte(this.yRot);
      pBuffer.writeByte(this.xRot);
      pBuffer.writeBoolean(this.onGround);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleTeleportEntity(this);
   }

   public int getId() {
      return this.id;
   }

   public double getX() {
      return this.x;
   }

   public double getY() {
      return this.y;
   }

   public double getZ() {
      return this.z;
   }

   public byte getyRot() {
      return this.yRot;
   }

   public byte getxRot() {
      return this.xRot;
   }

   public boolean isOnGround() {
      return this.onGround;
   }
}