package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddMobPacket implements Packet<ClientGamePacketListener> {
   private final int id;
   private final UUID uuid;
   private final int type;
   private final double x;
   private final double y;
   private final double z;
   private final int xd;
   private final int yd;
   private final int zd;
   private final byte yRot;
   private final byte xRot;
   private final byte yHeadRot;

   public ClientboundAddMobPacket(LivingEntity pEntity) {
      this.id = pEntity.getId();
      this.uuid = pEntity.getUUID();
      this.type = Registry.ENTITY_TYPE.getId(pEntity.getType());
      this.x = pEntity.getX();
      this.y = pEntity.getY();
      this.z = pEntity.getZ();
      this.yRot = (byte)((int)(pEntity.getYRot() * 256.0F / 360.0F));
      this.xRot = (byte)((int)(pEntity.getXRot() * 256.0F / 360.0F));
      this.yHeadRot = (byte)((int)(pEntity.yHeadRot * 256.0F / 360.0F));
      double d0 = 3.9D;
      Vec3 vec3 = pEntity.getDeltaMovement();
      double d1 = Mth.clamp(vec3.x, -3.9D, 3.9D);
      double d2 = Mth.clamp(vec3.y, -3.9D, 3.9D);
      double d3 = Mth.clamp(vec3.z, -3.9D, 3.9D);
      this.xd = (int)(d1 * 8000.0D);
      this.yd = (int)(d2 * 8000.0D);
      this.zd = (int)(d3 * 8000.0D);
   }

   public ClientboundAddMobPacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readVarInt();
      this.uuid = pBuffer.readUUID();
      this.type = pBuffer.readVarInt();
      this.x = pBuffer.readDouble();
      this.y = pBuffer.readDouble();
      this.z = pBuffer.readDouble();
      this.yRot = pBuffer.readByte();
      this.xRot = pBuffer.readByte();
      this.yHeadRot = pBuffer.readByte();
      this.xd = pBuffer.readShort();
      this.yd = pBuffer.readShort();
      this.zd = pBuffer.readShort();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.id);
      pBuffer.writeUUID(this.uuid);
      pBuffer.writeVarInt(this.type);
      pBuffer.writeDouble(this.x);
      pBuffer.writeDouble(this.y);
      pBuffer.writeDouble(this.z);
      pBuffer.writeByte(this.yRot);
      pBuffer.writeByte(this.xRot);
      pBuffer.writeByte(this.yHeadRot);
      pBuffer.writeShort(this.xd);
      pBuffer.writeShort(this.yd);
      pBuffer.writeShort(this.zd);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleAddMob(this);
   }

   public int getId() {
      return this.id;
   }

   public UUID getUUID() {
      return this.uuid;
   }

   public int getType() {
      return this.type;
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

   public int getXd() {
      return this.xd;
   }

   public int getYd() {
      return this.yd;
   }

   public int getZd() {
      return this.zd;
   }

   public byte getyRot() {
      return this.yRot;
   }

   public byte getxRot() {
      return this.xRot;
   }

   public byte getyHeadRot() {
      return this.yHeadRot;
   }
}