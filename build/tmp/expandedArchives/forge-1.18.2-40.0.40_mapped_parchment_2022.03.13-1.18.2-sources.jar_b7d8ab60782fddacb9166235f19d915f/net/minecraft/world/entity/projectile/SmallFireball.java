package net.minecraft.world.entity.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class SmallFireball extends Fireball {
   public SmallFireball(EntityType<? extends SmallFireball> p_37364_, Level p_37365_) {
      super(p_37364_, p_37365_);
   }

   public SmallFireball(Level p_37375_, LivingEntity p_37376_, double p_37377_, double p_37378_, double p_37379_) {
      super(EntityType.SMALL_FIREBALL, p_37376_, p_37377_, p_37378_, p_37379_, p_37375_);
   }

   public SmallFireball(Level p_37367_, double p_37368_, double p_37369_, double p_37370_, double p_37371_, double p_37372_, double p_37373_) {
      super(EntityType.SMALL_FIREBALL, p_37368_, p_37369_, p_37370_, p_37371_, p_37372_, p_37373_, p_37367_);
   }

   /**
    * Called when the arrow hits an entity
    */
   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      if (!this.level.isClientSide) {
         Entity entity = pResult.getEntity();
         if (!entity.fireImmune()) {
            Entity entity1 = this.getOwner();
            int i = entity.getRemainingFireTicks();
            entity.setSecondsOnFire(5);
            boolean flag = entity.hurt(DamageSource.fireball(this, entity1), 5.0F);
            if (!flag) {
               entity.setRemainingFireTicks(i);
            } else if (entity1 instanceof LivingEntity) {
               this.doEnchantDamageEffects((LivingEntity)entity1, entity);
            }
         }

      }
   }

   protected void onHitBlock(BlockHitResult p_37384_) {
      super.onHitBlock(p_37384_);
      if (!this.level.isClientSide) {
         Entity entity = this.getOwner();
         if (!(entity instanceof Mob) || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this)) {
            BlockPos blockpos = p_37384_.getBlockPos().relative(p_37384_.getDirection());
            if (this.level.isEmptyBlock(blockpos)) {
               this.level.setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level, blockpos));
            }
         }

      }
   }

   /**
    * Called when this EntityFireball hits a block or entity.
    */
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level.isClientSide) {
         this.discard();
      }

   }

   /**
    * Returns true if other Entities should be prevented from moving through this Entity.
    */
   public boolean isPickable() {
      return false;
   }

   /**
    * Called when the entity is attacked.
    */
   public boolean hurt(DamageSource pSource, float pAmount) {
      return false;
   }
}
