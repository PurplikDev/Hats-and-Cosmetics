package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownEgg extends ThrowableItemProjectile {
   public ThrownEgg(EntityType<? extends ThrownEgg> p_37473_, Level p_37474_) {
      super(p_37473_, p_37474_);
   }

   public ThrownEgg(Level p_37481_, LivingEntity p_37482_) {
      super(EntityType.EGG, p_37482_, p_37481_);
   }

   public ThrownEgg(Level p_37476_, double p_37477_, double p_37478_, double p_37479_) {
      super(EntityType.EGG, p_37477_, p_37478_, p_37479_, p_37476_);
   }

   /**
    * Handler for {@link World#setEntityState}
    */
   public void handleEntityEvent(byte pId) {
      if (pId == 3) {
         double d0 = 0.08D;

         for(int i = 0; i < 8; ++i) {
            this.level.addParticle(new ItemParticleOption(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D);
         }
      }

   }

   /**
    * Called when the arrow hits an entity
    */
   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      pResult.getEntity().hurt(DamageSource.thrown(this, this.getOwner()), 0.0F);
   }

   /**
    * Called when this EntityFireball hits a block or entity.
    */
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level.isClientSide) {
         if (this.random.nextInt(8) == 0) {
            int i = 1;
            if (this.random.nextInt(32) == 0) {
               i = 4;
            }

            for(int j = 0; j < i; ++j) {
               Chicken chicken = EntityType.CHICKEN.create(this.level);
               chicken.setAge(-24000);
               chicken.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
               this.level.addFreshEntity(chicken);
            }
         }

         this.level.broadcastEntityEvent(this, (byte)3);
         this.discard();
      }

   }

   protected Item getDefaultItem() {
      return Items.EGG;
   }
}