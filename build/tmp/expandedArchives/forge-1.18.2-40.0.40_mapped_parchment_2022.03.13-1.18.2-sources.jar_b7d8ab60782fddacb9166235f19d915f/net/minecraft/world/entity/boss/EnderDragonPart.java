package net.minecraft.world.entity.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

public class EnderDragonPart extends net.minecraftforge.entity.PartEntity<EnderDragon> {
   public final EnderDragon parentMob;
   public final String name;
   private final EntityDimensions size;

   public EnderDragonPart(EnderDragon p_31014_, String p_31015_, float p_31016_, float p_31017_) {
      super(p_31014_);
      this.size = EntityDimensions.scalable(p_31016_, p_31017_);
      this.refreshDimensions();
      this.parentMob = p_31014_;
      this.name = p_31015_;
   }

   protected void defineSynchedData() {
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   protected void readAdditionalSaveData(CompoundTag pCompound) {
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
   }

   /**
    * Returns true if other Entities should be prevented from moving through this Entity.
    */
   public boolean isPickable() {
      return true;
   }

   /**
    * Called when the entity is attacked.
    */
   public boolean hurt(DamageSource pSource, float pAmount) {
      return this.isInvulnerableTo(pSource) ? false : this.parentMob.hurt(this, pSource, pAmount);
   }

   /**
    * Returns true if Entity argument is equal to this Entity
    */
   public boolean is(Entity pEntity) {
      return this == pEntity || this.parentMob == pEntity;
   }

   public Packet<?> getAddEntityPacket() {
      throw new UnsupportedOperationException();
   }

   public EntityDimensions getDimensions(Pose pPose) {
      return this.size;
   }

   public boolean shouldBeSaved() {
      return false;
   }
}
