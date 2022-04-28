package net.minecraft.world.entity.item;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public class PrimedTnt extends Entity {
   private static final EntityDataAccessor<Integer> DATA_FUSE_ID = SynchedEntityData.defineId(PrimedTnt.class, EntityDataSerializers.INT);
   private static final int DEFAULT_FUSE_TIME = 80;
   @Nullable
   private LivingEntity owner;

   public PrimedTnt(EntityType<? extends PrimedTnt> p_32076_, Level p_32077_) {
      super(p_32076_, p_32077_);
      this.blocksBuilding = true;
   }

   public PrimedTnt(Level p_32079_, double p_32080_, double p_32081_, double p_32082_, @Nullable LivingEntity p_32083_) {
      this(EntityType.TNT, p_32079_);
      this.setPos(p_32080_, p_32081_, p_32082_);
      double d0 = p_32079_.random.nextDouble() * (double)((float)Math.PI * 2F);
      this.setDeltaMovement(-Math.sin(d0) * 0.02D, (double)0.2F, -Math.cos(d0) * 0.02D);
      this.setFuse(80);
      this.xo = p_32080_;
      this.yo = p_32081_;
      this.zo = p_32082_;
      this.owner = p_32083_;
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_FUSE_ID, 80);
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   /**
    * Returns true if other Entities should be prevented from moving through this Entity.
    */
   public boolean isPickable() {
      return !this.isRemoved();
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      if (!this.isNoGravity()) {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
      }

      this.move(MoverType.SELF, this.getDeltaMovement());
      this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
      if (this.onGround) {
         this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
      }

      int i = this.getFuse() - 1;
      this.setFuse(i);
      if (i <= 0) {
         this.discard();
         if (!this.level.isClientSide) {
            this.explode();
         }
      } else {
         this.updateInWaterStateAndDoFluidPushing();
         if (this.level.isClientSide) {
            this.level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
         }
      }

   }

   protected void explode() {
      float f = 4.0F;
      this.level.explode(this, this.getX(), this.getY(0.0625D), this.getZ(), 4.0F, Explosion.BlockInteraction.BREAK);
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
      pCompound.putShort("Fuse", (short)this.getFuse());
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   protected void readAdditionalSaveData(CompoundTag pCompound) {
      this.setFuse(pCompound.getShort("Fuse"));
   }

   /**
    * Returns null or the entityliving it was ignited by
    */
   @Nullable
   public LivingEntity getOwner() {
      return this.owner;
   }

   protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
      return 0.15F;
   }

   public void setFuse(int pFuse) {
      this.entityData.set(DATA_FUSE_ID, pFuse);
   }

   /**
    * Gets the fuse from the data manager
    */
   public int getFuse() {
      return this.entityData.get(DATA_FUSE_ID);
   }

   public Packet<?> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this);
   }
}