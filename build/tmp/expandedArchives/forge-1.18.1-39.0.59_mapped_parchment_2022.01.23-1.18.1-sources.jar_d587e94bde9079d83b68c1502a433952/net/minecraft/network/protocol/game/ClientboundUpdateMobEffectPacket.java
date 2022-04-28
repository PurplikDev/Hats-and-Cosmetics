package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class ClientboundUpdateMobEffectPacket implements Packet<ClientGamePacketListener> {
   private static final int FLAG_AMBIENT = 1;
   private static final int FLAG_VISIBLE = 2;
   private static final int FLAG_SHOW_ICON = 4;
   private final int entityId;
   private final byte effectId;
   private final byte effectAmplifier;
   private final int effectDurationTicks;
   private final byte flags;

   public ClientboundUpdateMobEffectPacket(int pEntityId, MobEffectInstance pEffectInstance) {
      this.entityId = pEntityId;
      this.effectId = (byte)(MobEffect.getId(pEffectInstance.getEffect()) & 255);
      this.effectAmplifier = (byte)(pEffectInstance.getAmplifier() & 255);
      if (pEffectInstance.getDuration() > 32767) {
         this.effectDurationTicks = 32767;
      } else {
         this.effectDurationTicks = pEffectInstance.getDuration();
      }

      byte b0 = 0;
      if (pEffectInstance.isAmbient()) {
         b0 = (byte)(b0 | 1);
      }

      if (pEffectInstance.isVisible()) {
         b0 = (byte)(b0 | 2);
      }

      if (pEffectInstance.showIcon()) {
         b0 = (byte)(b0 | 4);
      }

      this.flags = b0;
   }

   public ClientboundUpdateMobEffectPacket(FriendlyByteBuf pBuffer) {
      this.entityId = pBuffer.readVarInt();
      this.effectId = pBuffer.readByte();
      this.effectAmplifier = pBuffer.readByte();
      this.effectDurationTicks = pBuffer.readVarInt();
      this.flags = pBuffer.readByte();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(this.entityId);
      pBuffer.writeByte(this.effectId);
      pBuffer.writeByte(this.effectAmplifier);
      pBuffer.writeVarInt(this.effectDurationTicks);
      pBuffer.writeByte(this.flags);
   }

   public boolean isSuperLongDuration() {
      return this.effectDurationTicks == 32767;
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleUpdateMobEffect(this);
   }

   public int getEntityId() {
      return this.entityId;
   }

   public byte getEffectId() {
      return this.effectId;
   }

   public byte getEffectAmplifier() {
      return this.effectAmplifier;
   }

   public int getEffectDurationTicks() {
      return this.effectDurationTicks;
   }

   public boolean isEffectVisible() {
      return (this.flags & 2) == 2;
   }

   public boolean isEffectAmbient() {
      return (this.flags & 1) == 1;
   }

   public boolean effectShowsIcon() {
      return (this.flags & 4) == 4;
   }
}