package net.minecraft.client.resources.sounds;

import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSoundInstance implements SoundInstance {
   protected Sound sound;
   protected final SoundSource source;
   protected final ResourceLocation location;
   protected float volume = 1.0F;
   protected float pitch = 1.0F;
   protected double x;
   protected double y;
   protected double z;
   protected boolean looping;
   /** The number of ticks between repeating the sound */
   protected int delay;
   protected SoundInstance.Attenuation attenuation = SoundInstance.Attenuation.LINEAR;
   protected boolean relative;

   protected AbstractSoundInstance(SoundEvent pSoundEvent, SoundSource pSource) {
      this(pSoundEvent.getLocation(), pSource);
   }

   protected AbstractSoundInstance(ResourceLocation pLocation, SoundSource pSource) {
      this.location = pLocation;
      this.source = pSource;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   public WeighedSoundEvents resolve(SoundManager pHandler) {
      WeighedSoundEvents weighedsoundevents = pHandler.getSoundEvent(this.location);
      if (weighedsoundevents == null) {
         this.sound = SoundManager.EMPTY_SOUND;
      } else {
         this.sound = weighedsoundevents.getSound();
      }

      return weighedsoundevents;
   }

   public Sound getSound() {
      return this.sound;
   }

   public SoundSource getSource() {
      return this.source;
   }

   public boolean isLooping() {
      return this.looping;
   }

   public int getDelay() {
      return this.delay;
   }

   public float getVolume() {
      return this.volume * this.sound.getVolume();
   }

   public float getPitch() {
      return this.pitch * this.sound.getPitch();
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

   public SoundInstance.Attenuation getAttenuation() {
      return this.attenuation;
   }

   /**
    * True if the sound is not tied to a particular position in world (e.g. BGM)
    */
   public boolean isRelative() {
      return this.relative;
   }

   public String toString() {
      return "SoundInstance[" + this.location + "]";
   }
}