package net.minecraft.client.resources.sounds;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EntityBoundSoundInstance extends AbstractTickableSoundInstance {
   private final Entity entity;

   public EntityBoundSoundInstance(SoundEvent pSoundEvent, SoundSource pSource, float pVolume, float pPitch, Entity pEntity) {
      super(pSoundEvent, pSource);
      this.volume = pVolume;
      this.pitch = pPitch;
      this.entity = pEntity;
      this.x = (double)((float)this.entity.getX());
      this.y = (double)((float)this.entity.getY());
      this.z = (double)((float)this.entity.getZ());
   }

   public boolean canPlaySound() {
      return !this.entity.isSilent();
   }

   public void tick() {
      if (this.entity.isRemoved()) {
         this.stop();
      } else {
         this.x = (double)((float)this.entity.getX());
         this.y = (double)((float)this.entity.getY());
         this.z = (double)((float)this.entity.getZ());
      }
   }
}