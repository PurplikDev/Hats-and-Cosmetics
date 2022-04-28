package net.minecraft.network.protocol.game;

import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.apache.commons.lang3.Validate;

public class ClientboundSoundEntityPacket implements Packet<ClientGamePacketListener> {
   private final SoundEvent sound;
   private final SoundSource source;
   private final int id;
   private final float volume;
   private final float pitch;

   public ClientboundSoundEntityPacket(SoundEvent pSound, SoundSource pSource, Entity pEntity, float pVolume, float pPitch) {
      Validate.notNull(pSound, "sound");
      this.sound = pSound;
      this.source = pSource;
      this.id = pEntity.getId();
      this.volume = pVolume;
      this.pitch = pPitch;
   }

   public ClientboundSoundEntityPacket(FriendlyByteBuf pBuffer) {
      this.sound = Registry.SOUND_EVENT.byId(pBuffer.readVarInt());
      this.source = pBuffer.readEnum(SoundSource.class);
      this.id = pBuffer.readVarInt();
      this.volume = pBuffer.readFloat();
      this.pitch = pBuffer.readFloat();
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeVarInt(Registry.SOUND_EVENT.getId(this.sound));
      pBuffer.writeEnum(this.source);
      pBuffer.writeVarInt(this.id);
      pBuffer.writeFloat(this.volume);
      pBuffer.writeFloat(this.pitch);
   }

   public SoundEvent getSound() {
      return this.sound;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public int getId() {
      return this.id;
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleSoundEntityEvent(this);
   }
}