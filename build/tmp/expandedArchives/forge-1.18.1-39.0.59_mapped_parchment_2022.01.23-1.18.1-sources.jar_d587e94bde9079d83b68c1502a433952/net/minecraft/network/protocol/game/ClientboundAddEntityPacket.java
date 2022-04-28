package net.minecraft.network.protocol.game;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;

public class ClientboundAddEntityPacket implements Packet<ClientGamePacketListener> {
   public static final double MAGICAL_QUANTIZATION = 8000.0D;
   private final int id;
   private final UUID uuid;
   private final double x;
   private final double y;
   private final double z;
   private final int xa;
   private final int ya;
   private final int za;
   private final int xRot;
   private final int yRot;
   private final EntityType<?> type;
   private final int data;
   public static final double LIMIT = 3.9D;

   public ClientboundAddEntityPacket(int pId, UUID pUuid, double pX, double pY, double pZ, float pXRot, float pYRot, EntityType<?> pType, int pData, Vec3 pDeltaMovement) {
      this.id = pId;
      this.uuid = pUuid;
      this.x = pX;
      this.y = pY;
      this.z = pZ;
      this.xRot = Mth.floor(pXRot * 256.0F / 360.0F);
      this.yRot = Mth.floor(pYRot * 256.0F / 360.0F);
      this.type = pType;
      this.data = pData;
      this.xa = (int)(Mth.clamp(pDeltaMovement.x, -3.9D, 3.9D) * 8000.0D);
      this.ya = (int)(Mth.clamp(pDeltaMovement.y, -3.9D, 3.9D) * 8000.0D);
      this.za = (int)(Mth.clamp(pDeltaMovement.z, -3.9D, 3.9D) * 8000.0D);
   }

   public ClientboundAddEntityPacket(Entity pEntity) {
      this(pEntity, 0);
   }

   public ClientboundAddEntityPacket(Entity pEntity, int pData) {
      this(pEntity.getId(), pEntity.getUUID(), pEntity.getX(), pEntity.getY(), pEntity.getZ(), pEntity.getXRot(), pEntity.getYRot(), pEntity.getType(), pData, pEntity.getDeltaMovement());
   }

   public ClientboundAddEntityPacket(Entity pEntity, EntityType<?> pType, int pData, BlockPos pPos) {
      this(pEntity.getId(), pEntity.getUUID(), (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), pEntity.getXRot(), pEntity.getYRot(), pType, pData, pEntity.getDeltaMovement());
   }

   public ClientboundAddEntityPacket(FriendlyByteBuf pBuffer) {
      this.id = pBuffer.readVarInt();
      this.uuid = pBuffer.readUUID();
      this.type = Registry.ENTITY_TYPE.byId(pBuffer.readVarInt());
      this.x = pBuffer.readDouble();
      this.y = pBuffer.readDouble();
      this.z = pBuffer.readDouble();
      this.xRot = pBuffer.readByte();
      this.yRot = pBuffer.readByte();
      this.data = pBuffer.readInt();
      this.xa = pBuffer.readShort();
      this.ya = pBuffer.readShort();
      this.za = pBuffer.readShort();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.id);
      pBuffer.writeUUID(this.uuid);
      pBuffer.writeVarInt(Registry.ENTITY_TYPE.getId(this.type));
      pBuffer.writeDouble(this.x);
      pBuffer.writeDouble(this.y);
      pBuffer.writeDouble(this.z);
      pBuffer.writeByte(this.xRot);
      pBuffer.writeByte(this.yRot);
      pBuffer.writeInt(this.data);
      pBuffer.writeShort(this.xa);
      pBuffer.writeShort(this.ya);
      pBuffer.writeShort(this.za);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleAddEntity(this);
   }

   public int getId() {
      return this.id;
   }

   public UUID getUUID() {
      return this.uuid;
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

   public double getXa() {
      return (double)this.xa / 8000.0D;
   }

   public double getYa() {
      return (double)this.ya / 8000.0D;
   }

   public double getZa() {
      return (double)this.za / 8000.0D;
   }

   public int getxRot() {
      return this.xRot;
   }

   public int getyRot() {
      return this.yRot;
   }

   public EntityType<?> getType() {
      return this.type;
   }

   public int getData() {
      return this.data;
   }
}