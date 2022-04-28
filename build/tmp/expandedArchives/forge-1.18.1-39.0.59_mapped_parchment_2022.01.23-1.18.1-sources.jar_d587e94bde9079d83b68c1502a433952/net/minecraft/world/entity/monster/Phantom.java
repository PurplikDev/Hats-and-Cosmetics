package net.minecraft.world.entity.monster;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class Phantom extends FlyingMob implements Enemy {
   public static final float FLAP_DEGREES_PER_TICK = 7.448451F;
   public static final int TICKS_PER_FLAP = Mth.ceil(24.166098F);
   private static final EntityDataAccessor<Integer> ID_SIZE = SynchedEntityData.defineId(Phantom.class, EntityDataSerializers.INT);
   Vec3 moveTargetPoint = Vec3.ZERO;
   BlockPos anchorPoint = BlockPos.ZERO;
   Phantom.AttackPhase attackPhase = Phantom.AttackPhase.CIRCLE;

   public Phantom(EntityType<? extends Phantom> p_33101_, Level p_33102_) {
      super(p_33101_, p_33102_);
      this.xpReward = 5;
      this.moveControl = new Phantom.PhantomMoveControl(this);
      this.lookControl = new Phantom.PhantomLookControl(this);
   }

   public boolean isFlapping() {
      return (this.getUniqueFlapTickOffset() + this.tickCount) % TICKS_PER_FLAP == 0;
   }

   protected BodyRotationControl createBodyControl() {
      return new Phantom.PhantomBodyRotationControl(this);
   }

   protected void registerGoals() {
      this.goalSelector.addGoal(1, new Phantom.PhantomAttackStrategyGoal());
      this.goalSelector.addGoal(2, new Phantom.PhantomSweepAttackGoal());
      this.goalSelector.addGoal(3, new Phantom.PhantomCircleAroundAnchorGoal());
      this.targetSelector.addGoal(1, new Phantom.PhantomAttackPlayerTargetGoal());
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(ID_SIZE, 0);
   }

   public void setPhantomSize(int pSize) {
      this.entityData.set(ID_SIZE, Mth.clamp(pSize, 0, 64));
   }

   private void updatePhantomSizeInfo() {
      this.refreshDimensions();
      this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue((double)(6 + this.getPhantomSize()));
   }

   public int getPhantomSize() {
      return this.entityData.get(ID_SIZE);
   }

   protected float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {
      return pSize.height * 0.35F;
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      if (ID_SIZE.equals(pKey)) {
         this.updatePhantomSizeInfo();
      }

      super.onSyncedDataUpdated(pKey);
   }

   public int getUniqueFlapTickOffset() {
      return this.getId() * 3;
   }

   protected boolean shouldDespawnInPeaceful() {
      return true;
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      super.tick();
      if (this.level.isClientSide) {
         float f = Mth.cos((float)(this.getUniqueFlapTickOffset() + this.tickCount) * 7.448451F * ((float)Math.PI / 180F) + (float)Math.PI);
         float f1 = Mth.cos((float)(this.getUniqueFlapTickOffset() + this.tickCount + 1) * 7.448451F * ((float)Math.PI / 180F) + (float)Math.PI);
         if (f > 0.0F && f1 <= 0.0F) {
            this.level.playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.PHANTOM_FLAP, this.getSoundSource(), 0.95F + this.random.nextFloat() * 0.05F, 0.95F + this.random.nextFloat() * 0.05F, false);
         }

         int i = this.getPhantomSize();
         float f2 = Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * (1.3F + 0.21F * (float)i);
         float f3 = Mth.sin(this.getYRot() * ((float)Math.PI / 180F)) * (1.3F + 0.21F * (float)i);
         float f4 = (0.3F + f * 0.45F) * ((float)i * 0.2F + 1.0F);
         this.level.addParticle(ParticleTypes.MYCELIUM, this.getX() + (double)f2, this.getY() + (double)f4, this.getZ() + (double)f3, 0.0D, 0.0D, 0.0D);
         this.level.addParticle(ParticleTypes.MYCELIUM, this.getX() - (double)f2, this.getY() + (double)f4, this.getZ() - (double)f3, 0.0D, 0.0D, 0.0D);
      }

   }

   /**
    * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
    * use this to react to sunlight and start to burn.
    */
   public void aiStep() {
      if (this.isAlive() && this.isSunBurnTick()) {
         this.setSecondsOnFire(8);
      }

      super.aiStep();
   }

   protected void customServerAiStep() {
      super.customServerAiStep();
   }

   public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
      this.anchorPoint = this.blockPosition().above(5);
      this.setPhantomSize(0);
      return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      if (pCompound.contains("AX")) {
         this.anchorPoint = new BlockPos(pCompound.getInt("AX"), pCompound.getInt("AY"), pCompound.getInt("AZ"));
      }

      this.setPhantomSize(pCompound.getInt("Size"));
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("AX", this.anchorPoint.getX());
      pCompound.putInt("AY", this.anchorPoint.getY());
      pCompound.putInt("AZ", this.anchorPoint.getZ());
      pCompound.putInt("Size", this.getPhantomSize());
   }

   /**
    * Checks if the entity is in range to render.
    */
   public boolean shouldRenderAtSqrDistance(double pDistance) {
      return true;
   }

   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.PHANTOM_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.PHANTOM_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.PHANTOM_DEATH;
   }

   public MobType getMobType() {
      return MobType.UNDEAD;
   }

   /**
    * Returns the volume for the sounds this mob makes.
    */
   protected float getSoundVolume() {
      return 1.0F;
   }

   public boolean canAttackType(EntityType<?> pType) {
      return true;
   }

   public EntityDimensions getDimensions(Pose pPose) {
      int i = this.getPhantomSize();
      EntityDimensions entitydimensions = super.getDimensions(pPose);
      float f = (entitydimensions.width + 0.2F * (float)i) / entitydimensions.width;
      return entitydimensions.scale(f);
   }

   static enum AttackPhase {
      CIRCLE,
      SWOOP;
   }

   class PhantomAttackPlayerTargetGoal extends Goal {
      private final TargetingConditions attackTargeting = TargetingConditions.forCombat().range(64.0D);
      private int nextScanTick = reducedTickDelay(20);

      /**
       * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
       * method as well.
       */
      public boolean canUse() {
         if (this.nextScanTick > 0) {
            --this.nextScanTick;
            return false;
         } else {
            this.nextScanTick = reducedTickDelay(60);
            List<Player> list = Phantom.this.level.getNearbyPlayers(this.attackTargeting, Phantom.this, Phantom.this.getBoundingBox().inflate(16.0D, 64.0D, 16.0D));
            if (!list.isEmpty()) {
               list.sort(Comparator.<Entity, Double>comparing(Entity::getY).reversed());

               for(Player player : list) {
                  if (Phantom.this.canAttack(player, TargetingConditions.DEFAULT)) {
                     Phantom.this.setTarget(player);
                     return true;
                  }
               }
            }

            return false;
         }
      }

      /**
       * Returns whether an in-progress EntityAIBase should continue executing
       */
      public boolean canContinueToUse() {
         LivingEntity livingentity = Phantom.this.getTarget();
         return livingentity != null ? Phantom.this.canAttack(livingentity, TargetingConditions.DEFAULT) : false;
      }
   }

   class PhantomAttackStrategyGoal extends Goal {
      private int nextSweepTick;

      /**
       * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
       * method as well.
       */
      public boolean canUse() {
         LivingEntity livingentity = Phantom.this.getTarget();
         return livingentity != null ? Phantom.this.canAttack(livingentity, TargetingConditions.DEFAULT) : false;
      }

      /**
       * Execute a one shot task or start executing a continuous task
       */
      public void start() {
         this.nextSweepTick = this.adjustedTickDelay(10);
         Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
         this.setAnchorAboveTarget();
      }

      /**
       * Reset the task's internal state. Called when this task is interrupted by another one
       */
      public void stop() {
         Phantom.this.anchorPoint = Phantom.this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, Phantom.this.anchorPoint).above(10 + Phantom.this.random.nextInt(20));
      }

      /**
       * Keep ticking a continuous task that has already been started
       */
      public void tick() {
         if (Phantom.this.attackPhase == Phantom.AttackPhase.CIRCLE) {
            --this.nextSweepTick;
            if (this.nextSweepTick <= 0) {
               Phantom.this.attackPhase = Phantom.AttackPhase.SWOOP;
               this.setAnchorAboveTarget();
               this.nextSweepTick = this.adjustedTickDelay((8 + Phantom.this.random.nextInt(4)) * 20);
               Phantom.this.playSound(SoundEvents.PHANTOM_SWOOP, 10.0F, 0.95F + Phantom.this.random.nextFloat() * 0.1F);
            }
         }

      }

      private void setAnchorAboveTarget() {
         Phantom.this.anchorPoint = Phantom.this.getTarget().blockPosition().above(20 + Phantom.this.random.nextInt(20));
         if (Phantom.this.anchorPoint.getY() < Phantom.this.level.getSeaLevel()) {
            Phantom.this.anchorPoint = new BlockPos(Phantom.this.anchorPoint.getX(), Phantom.this.level.getSeaLevel() + 1, Phantom.this.anchorPoint.getZ());
         }

      }
   }

   class PhantomBodyRotationControl extends BodyRotationControl {
      public PhantomBodyRotationControl(Mob p_33216_) {
         super(p_33216_);
      }

      /**
       * Update the Head and Body rendenring angles
       */
      public void clientTick() {
         Phantom.this.yHeadRot = Phantom.this.yBodyRot;
         Phantom.this.yBodyRot = Phantom.this.getYRot();
      }
   }

   class PhantomCircleAroundAnchorGoal extends Phantom.PhantomMoveTargetGoal {
      private float angle;
      private float distance;
      private float height;
      private float clockwise;

      /**
       * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
       * method as well.
       */
      public boolean canUse() {
         return Phantom.this.getTarget() == null || Phantom.this.attackPhase == Phantom.AttackPhase.CIRCLE;
      }

      /**
       * Execute a one shot task or start executing a continuous task
       */
      public void start() {
         this.distance = 5.0F + Phantom.this.random.nextFloat() * 10.0F;
         this.height = -4.0F + Phantom.this.random.nextFloat() * 9.0F;
         this.clockwise = Phantom.this.random.nextBoolean() ? 1.0F : -1.0F;
         this.selectNext();
      }

      /**
       * Keep ticking a continuous task that has already been started
       */
      public void tick() {
         if (Phantom.this.random.nextInt(this.adjustedTickDelay(350)) == 0) {
            this.height = -4.0F + Phantom.this.random.nextFloat() * 9.0F;
         }

         if (Phantom.this.random.nextInt(this.adjustedTickDelay(250)) == 0) {
            ++this.distance;
            if (this.distance > 15.0F) {
               this.distance = 5.0F;
               this.clockwise = -this.clockwise;
            }
         }

         if (Phantom.this.random.nextInt(this.adjustedTickDelay(450)) == 0) {
            this.angle = Phantom.this.random.nextFloat() * 2.0F * (float)Math.PI;
            this.selectNext();
         }

         if (this.touchingTarget()) {
            this.selectNext();
         }

         if (Phantom.this.moveTargetPoint.y < Phantom.this.getY() && !Phantom.this.level.isEmptyBlock(Phantom.this.blockPosition().below(1))) {
            this.height = Math.max(1.0F, this.height);
            this.selectNext();
         }

         if (Phantom.this.moveTargetPoint.y > Phantom.this.getY() && !Phantom.this.level.isEmptyBlock(Phantom.this.blockPosition().above(1))) {
            this.height = Math.min(-1.0F, this.height);
            this.selectNext();
         }

      }

      private void selectNext() {
         if (BlockPos.ZERO.equals(Phantom.this.anchorPoint)) {
            Phantom.this.anchorPoint = Phantom.this.blockPosition();
         }

         this.angle += this.clockwise * 15.0F * ((float)Math.PI / 180F);
         Phantom.this.moveTargetPoint = Vec3.atLowerCornerOf(Phantom.this.anchorPoint).add((double)(this.distance * Mth.cos(this.angle)), (double)(-4.0F + this.height), (double)(this.distance * Mth.sin(this.angle)));
      }
   }

   class PhantomLookControl extends LookControl {
      public PhantomLookControl(Mob p_33235_) {
         super(p_33235_);
      }

      /**
       * Updates look
       */
      public void tick() {
      }
   }

   class PhantomMoveControl extends MoveControl {
      private float speed = 0.1F;

      public PhantomMoveControl(Mob p_33241_) {
         super(p_33241_);
      }

      public void tick() {
         if (Phantom.this.horizontalCollision) {
            Phantom.this.setYRot(Phantom.this.getYRot() + 180.0F);
            this.speed = 0.1F;
         }

         float f = (float)(Phantom.this.moveTargetPoint.x - Phantom.this.getX());
         float f1 = (float)(Phantom.this.moveTargetPoint.y - Phantom.this.getY());
         float f2 = (float)(Phantom.this.moveTargetPoint.z - Phantom.this.getZ());
         double d0 = (double)Mth.sqrt(f * f + f2 * f2);
         if (Math.abs(d0) > (double)1.0E-5F) {
            double d1 = 1.0D - (double)Mth.abs(f1 * 0.7F) / d0;
            f = (float)((double)f * d1);
            f2 = (float)((double)f2 * d1);
            d0 = (double)Mth.sqrt(f * f + f2 * f2);
            double d2 = (double)Mth.sqrt(f * f + f2 * f2 + f1 * f1);
            float f3 = Phantom.this.getYRot();
            float f4 = (float)Mth.atan2((double)f2, (double)f);
            float f5 = Mth.wrapDegrees(Phantom.this.getYRot() + 90.0F);
            float f6 = Mth.wrapDegrees(f4 * (180F / (float)Math.PI));
            Phantom.this.setYRot(Mth.approachDegrees(f5, f6, 4.0F) - 90.0F);
            Phantom.this.yBodyRot = Phantom.this.getYRot();
            if (Mth.degreesDifferenceAbs(f3, Phantom.this.getYRot()) < 3.0F) {
               this.speed = Mth.approach(this.speed, 1.8F, 0.005F * (1.8F / this.speed));
            } else {
               this.speed = Mth.approach(this.speed, 0.2F, 0.025F);
            }

            float f7 = (float)(-(Mth.atan2((double)(-f1), d0) * (double)(180F / (float)Math.PI)));
            Phantom.this.setXRot(f7);
            float f8 = Phantom.this.getYRot() + 90.0F;
            double d3 = (double)(this.speed * Mth.cos(f8 * ((float)Math.PI / 180F))) * Math.abs((double)f / d2);
            double d4 = (double)(this.speed * Mth.sin(f8 * ((float)Math.PI / 180F))) * Math.abs((double)f2 / d2);
            double d5 = (double)(this.speed * Mth.sin(f7 * ((float)Math.PI / 180F))) * Math.abs((double)f1 / d2);
            Vec3 vec3 = Phantom.this.getDeltaMovement();
            Phantom.this.setDeltaMovement(vec3.add((new Vec3(d3, d5, d4)).subtract(vec3).scale(0.2D)));
         }

      }
   }

   abstract class PhantomMoveTargetGoal extends Goal {
      public PhantomMoveTargetGoal() {
         this.setFlags(EnumSet.of(Goal.Flag.MOVE));
      }

      protected boolean touchingTarget() {
         return Phantom.this.moveTargetPoint.distanceToSqr(Phantom.this.getX(), Phantom.this.getY(), Phantom.this.getZ()) < 4.0D;
      }
   }

   class PhantomSweepAttackGoal extends Phantom.PhantomMoveTargetGoal {
      private static final int CAT_SEARCH_TICK_DELAY = 20;
      private boolean isScaredOfCat;
      private int catSearchTick;

      /**
       * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
       * method as well.
       */
      public boolean canUse() {
         return Phantom.this.getTarget() != null && Phantom.this.attackPhase == Phantom.AttackPhase.SWOOP;
      }

      /**
       * Returns whether an in-progress EntityAIBase should continue executing
       */
      public boolean canContinueToUse() {
         LivingEntity livingentity = Phantom.this.getTarget();
         if (livingentity == null) {
            return false;
         } else if (!livingentity.isAlive()) {
            return false;
         } else {
            if (livingentity instanceof Player) {
               Player player = (Player)livingentity;
               if (livingentity.isSpectator() || player.isCreative()) {
                  return false;
               }
            }

            if (!this.canUse()) {
               return false;
            } else {
               if (Phantom.this.tickCount > this.catSearchTick) {
                  this.catSearchTick = Phantom.this.tickCount + 20;
                  List<Cat> list = Phantom.this.level.getEntitiesOfClass(Cat.class, Phantom.this.getBoundingBox().inflate(16.0D), EntitySelector.ENTITY_STILL_ALIVE);

                  for(Cat cat : list) {
                     cat.hiss();
                  }

                  this.isScaredOfCat = !list.isEmpty();
               }

               return !this.isScaredOfCat;
            }
         }
      }

      /**
       * Execute a one shot task or start executing a continuous task
       */
      public void start() {
      }

      /**
       * Reset the task's internal state. Called when this task is interrupted by another one
       */
      public void stop() {
         Phantom.this.setTarget((LivingEntity)null);
         Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
      }

      /**
       * Keep ticking a continuous task that has already been started
       */
      public void tick() {
         LivingEntity livingentity = Phantom.this.getTarget();
         if (livingentity != null) {
            Phantom.this.moveTargetPoint = new Vec3(livingentity.getX(), livingentity.getY(0.5D), livingentity.getZ());
            if (Phantom.this.getBoundingBox().inflate((double)0.2F).intersects(livingentity.getBoundingBox())) {
               Phantom.this.doHurtTarget(livingentity);
               Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
               if (!Phantom.this.isSilent()) {
                  Phantom.this.level.levelEvent(1039, Phantom.this.blockPosition(), 0);
               }
            } else if (Phantom.this.horizontalCollision || Phantom.this.hurtTime > 0) {
               Phantom.this.attackPhase = Phantom.AttackPhase.CIRCLE;
            }

         }
      }
   }
}