package net.minecraft.client.resources.sounds;

import javax.annotation.Nullable;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.Weighted;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class Sound implements Weighted<Sound> {
   private final ResourceLocation location;
   private final float volume;
   private final float pitch;
   private final int weight;
   private final Sound.Type type;
   private final boolean stream;
   private final boolean preload;
   private final int attenuationDistance;

   public Sound(String pPath, float pVolume, float pPitch, int pWeight, Sound.Type pType, boolean pStream, boolean pPreload, int pAttenuationDistance) {
      this.location = new ResourceLocation(pPath);
      this.volume = pVolume;
      this.pitch = pPitch;
      this.weight = pWeight;
      this.type = pType;
      this.stream = pStream;
      this.preload = pPreload;
      this.attenuationDistance = pAttenuationDistance;
   }

   public ResourceLocation getLocation() {
      return this.location;
   }

   public ResourceLocation getPath() {
      return new ResourceLocation(this.location.getNamespace(), "sounds/" + this.location.getPath() + ".ogg");
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   public int getWeight() {
      return this.weight;
   }

   public Sound getSound() {
      return this;
   }

   public void preloadIfRequired(SoundEngine pEngine) {
      if (this.preload) {
         pEngine.requestPreload(this);
      }

   }

   public Sound.Type getType() {
      return this.type;
   }

   public boolean shouldStream() {
      return this.stream;
   }

   public boolean shouldPreload() {
      return this.preload;
   }

   public int getAttenuationDistance() {
      return this.attenuationDistance;
   }

   public String toString() {
      return "Sound[" + this.location + "]";
   }

   @OnlyIn(Dist.CLIENT)
   public static enum Type {
      FILE("file"),
      SOUND_EVENT("event");

      private final String name;

      private Type(String p_119809_) {
         this.name = p_119809_;
      }

      @Nullable
      public static Sound.Type getByName(String pName) {
         for(Sound.Type sound$type : values()) {
            if (sound$type.name.equals(pName)) {
               return sound$type;
            }
         }

         return null;
      }
   }
}