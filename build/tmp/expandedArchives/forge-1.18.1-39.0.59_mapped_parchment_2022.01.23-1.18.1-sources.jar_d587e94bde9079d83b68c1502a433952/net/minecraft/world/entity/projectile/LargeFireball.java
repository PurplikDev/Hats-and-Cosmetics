package net.minecraft.world.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class LargeFireball extends Fireball {
   private int explosionPower = 1;

   public LargeFireball(EntityType<? extends LargeFireball> p_37199_, Level p_37200_) {
      super(p_37199_, p_37200_);
   }

   public LargeFireball(Level p_181151_, LivingEntity p_181152_, double p_181153_, double p_181154_, double p_181155_, int p_181156_) {
      super(EntityType.FIREBALL, p_181152_, p_181153_, p_181154_, p_181155_, p_181151_);
      this.explosionPower = p_181156_;
   }

   /**
    * Called when this EntityFireball hits a block or entity.
    */
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level.isClientSide) {
         boolean flag = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this.getOwner());
         this.level.explode((Entity)null, this.getX(), this.getY(), this.getZ(), (float)this.explosionPower, flag, flag ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE);
         this.discard();
      }

   }

   /**
    * Called when the arrow hits an entity
    */
   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      if (!this.level.isClientSide) {
         Entity entity = pResult.getEntity();
         Entity entity1 = this.getOwner();
         entity.hurt(DamageSource.fireball(this, entity1), 6.0F);
         if (entity1 instanceof LivingEntity) {
            this.doEnchantDamageEffects((LivingEntity)entity1, entity);
         }

      }
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putByte("ExplosionPower", (byte)this.explosionPower);
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      if (pCompound.contains("ExplosionPower", 99)) {
         this.explosionPower = pCompound.getByte("ExplosionPower");
      }

   }
}
