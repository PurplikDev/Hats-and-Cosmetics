package net.minecraft.world.entity.projectile;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Snowball extends ThrowableItemProjectile {
   public Snowball(EntityType<? extends Snowball> p_37391_, Level p_37392_) {
      super(p_37391_, p_37392_);
   }

   public Snowball(Level p_37399_, LivingEntity p_37400_) {
      super(EntityType.SNOWBALL, p_37400_, p_37399_);
   }

   public Snowball(Level p_37394_, double p_37395_, double p_37396_, double p_37397_) {
      super(EntityType.SNOWBALL, p_37395_, p_37396_, p_37397_, p_37394_);
   }

   protected Item getDefaultItem() {
      return Items.SNOWBALL;
   }

   private ParticleOptions getParticle() {
      ItemStack itemstack = this.getItemRaw();
      return (ParticleOptions)(itemstack.isEmpty() ? ParticleTypes.ITEM_SNOWBALL : new ItemParticleOption(ParticleTypes.ITEM, itemstack));
   }

   /**
    * Handler for {@link World#setEntityState}
    */
   public void handleEntityEvent(byte pId) {
      if (pId == 3) {
         ParticleOptions particleoptions = this.getParticle();

         for(int i = 0; i < 8; ++i) {
            this.level.addParticle(particleoptions, this.getX(), this.getY(), this.getZ(), 0.0D, 0.0D, 0.0D);
         }
      }

   }

   /**
    * Called when the arrow hits an entity
    */
   protected void onHitEntity(EntityHitResult pResult) {
      super.onHitEntity(pResult);
      Entity entity = pResult.getEntity();
      int i = entity instanceof Blaze ? 3 : 0;
      entity.hurt(DamageSource.thrown(this, this.getOwner()), (float)i);
   }

   /**
    * Called when this EntityFireball hits a block or entity.
    */
   protected void onHit(HitResult pResult) {
      super.onHit(pResult);
      if (!this.level.isClientSide) {
         this.level.broadcastEntityEvent(this, (byte)3);
         this.discard();
      }

   }
}