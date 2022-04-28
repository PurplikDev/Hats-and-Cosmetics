package net.minecraft.world.entity.player;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.JigsawBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public abstract class Player extends LivingEntity {
   public static final String PERSISTED_NBT_TAG = "PlayerPersisted";
   public static final String UUID_PREFIX_OFFLINE_PLAYER = "OfflinePlayer:";
   public static final int MAX_NAME_LENGTH = 16;
   public static final int MAX_HEALTH = 20;
   public static final int SLEEP_DURATION = 100;
   public static final int WAKE_UP_DURATION = 10;
   public static final int ENDER_SLOT_OFFSET = 200;
   public static final float CROUCH_BB_HEIGHT = 1.5F;
   public static final float SWIMMING_BB_WIDTH = 0.6F;
   public static final float SWIMMING_BB_HEIGHT = 0.6F;
   public static final float DEFAULT_EYE_HEIGHT = 1.62F;
   public static final EntityDimensions STANDING_DIMENSIONS = EntityDimensions.scalable(0.6F, 1.8F);
   private static final Map<Pose, EntityDimensions> POSES = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.STANDING, STANDING_DIMENSIONS).put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F)).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F)).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
   private static final int FLY_ACHIEVEMENT_SPEED = 25;
   private static final EntityDataAccessor<Float> DATA_PLAYER_ABSORPTION_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Integer> DATA_SCORE_ID = SynchedEntityData.defineId(Player.class, EntityDataSerializers.INT);
   protected static final EntityDataAccessor<Byte> DATA_PLAYER_MODE_CUSTOMISATION = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<Byte> DATA_PLAYER_MAIN_HAND = SynchedEntityData.defineId(Player.class, EntityDataSerializers.BYTE);
   protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_LEFT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   protected static final EntityDataAccessor<CompoundTag> DATA_SHOULDER_RIGHT = SynchedEntityData.defineId(Player.class, EntityDataSerializers.COMPOUND_TAG);
   private long timeEntitySatOnShoulder;
   private final Inventory inventory = new Inventory(this);
   protected PlayerEnderChestContainer enderChestInventory = new PlayerEnderChestContainer();
   public final InventoryMenu inventoryMenu;
   public AbstractContainerMenu containerMenu;
   protected FoodData foodData = new FoodData();
   protected int jumpTriggerTime;
   public float oBob;
   public float bob;
   public int takeXpDelay;
   public double xCloakO;
   public double yCloakO;
   public double zCloakO;
   public double xCloak;
   public double yCloak;
   public double zCloak;
   private int sleepCounter;
   protected boolean wasUnderwater;
   private final Abilities abilities = new Abilities();
   public int experienceLevel;
   public int totalExperience;
   public float experienceProgress;
   protected int enchantmentSeed;
   protected final float defaultFlySpeed = 0.02F;
   private int lastLevelUpTime;
   /** The player's unique game profile */
   private final GameProfile gameProfile;
   private boolean reducedDebugInfo;
   private ItemStack lastItemInMainHand = ItemStack.EMPTY;
   private final ItemCooldowns cooldowns = this.createItemCooldowns();
   @Nullable
   public FishingHook fishing;
   private final java.util.Collection<MutableComponent> prefixes = new java.util.LinkedList<>();
   private final java.util.Collection<MutableComponent> suffixes = new java.util.LinkedList<>();
   @Nullable private Pose forcedPose;

   public Player(Level p_36114_, BlockPos p_36115_, float p_36116_, GameProfile p_36117_) {
      super(EntityType.PLAYER, p_36114_);
      this.setUUID(createPlayerUUID(p_36117_));
      this.gameProfile = p_36117_;
      this.inventoryMenu = new InventoryMenu(this.inventory, !p_36114_.isClientSide, this);
      this.containerMenu = this.inventoryMenu;
      this.moveTo((double)p_36115_.getX() + 0.5D, (double)(p_36115_.getY() + 1), (double)p_36115_.getZ() + 0.5D, p_36116_, 0.0F);
      this.rotOffs = 180.0F;
   }

   public boolean blockActionRestricted(Level pLevel, BlockPos pPos, GameType pGameMode) {
      if (!pGameMode.isBlockPlacingRestricted()) {
         return false;
      } else if (pGameMode == GameType.SPECTATOR) {
         return true;
      } else if (this.mayBuild()) {
         return false;
      } else {
         ItemStack itemstack = this.getMainHandItem();
         return itemstack.isEmpty() || !itemstack.hasAdventureModeBreakTagForBlock(pLevel.getTagManager(), new BlockInWorld(pLevel, pPos, false));
      }
   }

   public static AttributeSupplier.Builder createAttributes() {
       return LivingEntity.createLivingAttributes().add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, (double) 0.1F).add(Attributes.ATTACK_SPEED).add(Attributes.LUCK).add(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).add(Attributes.ATTACK_KNOCKBACK);
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_PLAYER_ABSORPTION_ID, 0.0F);
      this.entityData.define(DATA_SCORE_ID, 0);
      this.entityData.define(DATA_PLAYER_MODE_CUSTOMISATION, (byte)0);
      this.entityData.define(DATA_PLAYER_MAIN_HAND, (byte)1);
      this.entityData.define(DATA_SHOULDER_LEFT, new CompoundTag());
      this.entityData.define(DATA_SHOULDER_RIGHT, new CompoundTag());
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      net.minecraftforge.event.ForgeEventFactory.onPlayerPreTick(this);
      this.noPhysics = this.isSpectator();
      if (this.isSpectator()) {
         this.onGround = false;
      }

      if (this.takeXpDelay > 0) {
         --this.takeXpDelay;
      }

      if (this.isSleeping()) {
         ++this.sleepCounter;
         if (this.sleepCounter > 100) {
            this.sleepCounter = 100;
         }

         if (!this.level.isClientSide && !net.minecraftforge.event.ForgeEventFactory.fireSleepingTimeCheck(this, getSleepingPos())) {
            this.stopSleepInBed(false, true);
         }
      } else if (this.sleepCounter > 0) {
         ++this.sleepCounter;
         if (this.sleepCounter >= 110) {
            this.sleepCounter = 0;
         }
      }

      this.updateIsUnderwater();
      super.tick();
      if (!this.level.isClientSide && this.containerMenu != null && !this.containerMenu.stillValid(this)) {
         this.closeContainer();
         this.containerMenu = this.inventoryMenu;
      }

      this.moveCloak();
      if (!this.level.isClientSide) {
         this.foodData.tick(this);
         this.awardStat(Stats.PLAY_TIME);
         this.awardStat(Stats.TOTAL_WORLD_TIME);
         if (this.isAlive()) {
            this.awardStat(Stats.TIME_SINCE_DEATH);
         }

         if (this.isDiscrete()) {
            this.awardStat(Stats.CROUCH_TIME);
         }

         if (!this.isSleeping()) {
            this.awardStat(Stats.TIME_SINCE_REST);
         }
      }

      int i = 29999999;
      double d0 = Mth.clamp(this.getX(), -2.9999999E7D, 2.9999999E7D);
      double d1 = Mth.clamp(this.getZ(), -2.9999999E7D, 2.9999999E7D);
      if (d0 != this.getX() || d1 != this.getZ()) {
         this.setPos(d0, this.getY(), d1);
      }

      ++this.attackStrengthTicker;
      ItemStack itemstack = this.getMainHandItem();
      if (!ItemStack.matches(this.lastItemInMainHand, itemstack)) {
         if (!ItemStack.isSameIgnoreDurability(this.lastItemInMainHand, itemstack)) {
            this.resetAttackStrengthTicker();
         }

         this.lastItemInMainHand = itemstack.copy();
      }

      this.turtleHelmetTick();
      this.cooldowns.tick();
      this.updatePlayerPose();
      net.minecraftforge.event.ForgeEventFactory.onPlayerPostTick(this);
   }

   public boolean isSecondaryUseActive() {
      return this.isShiftKeyDown();
   }

   protected boolean wantsToStopRiding() {
      return this.isShiftKeyDown();
   }

   protected boolean isStayingOnGroundSurface() {
      return this.isShiftKeyDown();
   }

   protected boolean updateIsUnderwater() {
      this.wasUnderwater = this.isEyeInFluid(FluidTags.WATER);
      return this.wasUnderwater;
   }

   private void turtleHelmetTick() {
      ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
      if (itemstack.is(Items.TURTLE_HELMET) && !this.isEyeInFluid(FluidTags.WATER)) {
         this.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0, false, false, true));
      }

   }

   protected ItemCooldowns createItemCooldowns() {
      return new ItemCooldowns();
   }

   private void moveCloak() {
      this.xCloakO = this.xCloak;
      this.yCloakO = this.yCloak;
      this.zCloakO = this.zCloak;
      double d0 = this.getX() - this.xCloak;
      double d1 = this.getY() - this.yCloak;
      double d2 = this.getZ() - this.zCloak;
      double d3 = 10.0D;
      if (d0 > 10.0D) {
         this.xCloak = this.getX();
         this.xCloakO = this.xCloak;
      }

      if (d2 > 10.0D) {
         this.zCloak = this.getZ();
         this.zCloakO = this.zCloak;
      }

      if (d1 > 10.0D) {
         this.yCloak = this.getY();
         this.yCloakO = this.yCloak;
      }

      if (d0 < -10.0D) {
         this.xCloak = this.getX();
         this.xCloakO = this.xCloak;
      }

      if (d2 < -10.0D) {
         this.zCloak = this.getZ();
         this.zCloakO = this.zCloak;
      }

      if (d1 < -10.0D) {
         this.yCloak = this.getY();
         this.yCloakO = this.yCloak;
      }

      this.xCloak += d0 * 0.25D;
      this.zCloak += d2 * 0.25D;
      this.yCloak += d1 * 0.25D;
   }

   protected void updatePlayerPose() {
      if(forcedPose != null) {
         this.setPose(forcedPose);
         return;
      }
      if (this.canEnterPose(Pose.SWIMMING)) {
         Pose pose;
         if (this.isFallFlying()) {
            pose = Pose.FALL_FLYING;
         } else if (this.isSleeping()) {
            pose = Pose.SLEEPING;
         } else if (this.isSwimming()) {
            pose = Pose.SWIMMING;
         } else if (this.isAutoSpinAttack()) {
            pose = Pose.SPIN_ATTACK;
         } else if (this.isShiftKeyDown() && !this.abilities.flying) {
            pose = Pose.CROUCHING;
         } else {
            pose = Pose.STANDING;
         }

         Pose pose1;
         if (!this.isSpectator() && !this.isPassenger() && !this.canEnterPose(pose)) {
            if (this.canEnterPose(Pose.CROUCHING)) {
               pose1 = Pose.CROUCHING;
            } else {
               pose1 = Pose.SWIMMING;
            }
         } else {
            pose1 = pose;
         }

         this.setPose(pose1);
      }
   }

   /**
    * Return the amount of time this entity should stay in a portal before being transported.
    */
   public int getPortalWaitTime() {
      return this.abilities.invulnerable ? 1 : 80;
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.PLAYER_SWIM;
   }

   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.PLAYER_SPLASH;
   }

   protected SoundEvent getSwimHighSpeedSplashSound() {
      return SoundEvents.PLAYER_SPLASH_HIGH_SPEED;
   }

   /**
    * Return the amount of cooldown before this entity can use a portal again.
    */
   public int getDimensionChangingDelay() {
      return 10;
   }

   public void playSound(SoundEvent pSound, float pVolume, float pPitch) {
      this.level.playSound(this, this.getX(), this.getY(), this.getZ(), pSound, this.getSoundSource(), pVolume, pPitch);
   }

   public void playNotifySound(SoundEvent p_36140_, SoundSource p_36141_, float p_36142_, float p_36143_) {
   }

   public SoundSource getSoundSource() {
      return SoundSource.PLAYERS;
   }

   protected int getFireImmuneTicks() {
      return 20;
   }

   /**
    * Handler for {@link World#setEntityState}
    */
   public void handleEntityEvent(byte pId) {
      if (pId == 9) {
         this.completeUsingItem();
      } else if (pId == 23) {
         this.reducedDebugInfo = false;
      } else if (pId == 22) {
         this.reducedDebugInfo = true;
      } else if (pId == 43) {
         this.addParticlesAroundSelf(ParticleTypes.CLOUD);
      } else {
         super.handleEntityEvent(pId);
      }

   }

   private void addParticlesAroundSelf(ParticleOptions p_36209_) {
      for(int i = 0; i < 5; ++i) {
         double d0 = this.random.nextGaussian() * 0.02D;
         double d1 = this.random.nextGaussian() * 0.02D;
         double d2 = this.random.nextGaussian() * 0.02D;
         this.level.addParticle(p_36209_, this.getRandomX(1.0D), this.getRandomY() + 1.0D, this.getRandomZ(1.0D), d0, d1, d2);
      }

   }

   /**
    * set current crafting inventory back to the 2x2 square
    */
   public void closeContainer() {
      this.containerMenu = this.inventoryMenu;
   }

   /**
    * Handles updating while riding another entity
    */
   public void rideTick() {
      if (!this.level.isClientSide && this.wantsToStopRiding() && this.isPassenger()) {
         this.stopRiding();
         this.setShiftKeyDown(false);
      } else {
         double d0 = this.getX();
         double d1 = this.getY();
         double d2 = this.getZ();
         super.rideTick();
         this.oBob = this.bob;
         this.bob = 0.0F;
         this.checkRidingStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
      }
   }

   protected void serverAiStep() {
      super.serverAiStep();
      this.updateSwingTime();
      this.yHeadRot = this.getYRot();
   }

   /**
    * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
    * use this to react to sunlight and start to burn.
    */
   public void aiStep() {
      if (this.jumpTriggerTime > 0) {
         --this.jumpTriggerTime;
      }

      if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.level.getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION)) {
         if (this.getHealth() < this.getMaxHealth() && this.tickCount % 20 == 0) {
            this.heal(1.0F);
         }

         if (this.foodData.needsFood() && this.tickCount % 10 == 0) {
            this.foodData.setFoodLevel(this.foodData.getFoodLevel() + 1);
         }
      }

      this.inventory.tick();
      this.oBob = this.bob;
      super.aiStep();
      this.flyingSpeed = 0.02F;
      if (this.isSprinting()) {
         this.flyingSpeed = (float)((double)this.flyingSpeed + 0.005999999865889549D);
      }

      this.setSpeed((float)this.getAttributeValue(Attributes.MOVEMENT_SPEED));
      float f;
      if (this.onGround && !this.isDeadOrDying() && !this.isSwimming()) {
         f = Math.min(0.1F, (float)this.getDeltaMovement().horizontalDistance());
      } else {
         f = 0.0F;
      }

      this.bob += (f - this.bob) * 0.4F;
      if (this.getHealth() > 0.0F && !this.isSpectator()) {
         AABB aabb;
         if (this.isPassenger() && !this.getVehicle().isRemoved()) {
            aabb = this.getBoundingBox().minmax(this.getVehicle().getBoundingBox()).inflate(1.0D, 0.0D, 1.0D);
         } else {
            aabb = this.getBoundingBox().inflate(1.0D, 0.5D, 1.0D);
         }

         List<Entity> list = this.level.getEntities(this, aabb);
         List<Entity> list1 = Lists.newArrayList();

         for(int i = 0; i < list.size(); ++i) {
            Entity entity = list.get(i);
            if (entity.getType() == EntityType.EXPERIENCE_ORB) {
               list1.add(entity);
            } else if (!entity.isRemoved()) {
               this.touch(entity);
            }
         }

         if (!list1.isEmpty()) {
            this.touch(Util.getRandom(list1, this.random));
         }
      }

      this.playShoulderEntityAmbientSound(this.getShoulderEntityLeft());
      this.playShoulderEntityAmbientSound(this.getShoulderEntityRight());
      if (!this.level.isClientSide && (this.fallDistance > 0.5F || this.isInWater()) || this.abilities.flying || this.isSleeping() || this.isInPowderSnow) {
         this.removeEntitiesOnShoulder();
      }

   }

   private void playShoulderEntityAmbientSound(@Nullable CompoundTag p_36368_) {
      if (p_36368_ != null && (!p_36368_.contains("Silent") || !p_36368_.getBoolean("Silent")) && this.level.random.nextInt(200) == 0) {
         String s = p_36368_.getString("id");
         EntityType.byString(s).filter((p_36280_) -> {
            return p_36280_ == EntityType.PARROT;
         }).ifPresent((p_36255_) -> {
            if (!Parrot.imitateNearbyMobs(this.level, this)) {
               this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), Parrot.getAmbient(this.level, this.level.random), this.getSoundSource(), 1.0F, Parrot.getPitch(this.level.random));
            }

         });
      }

   }

   private void touch(Entity pEntity) {
      pEntity.playerTouch(this);
   }

   public int getScore() {
      return this.entityData.get(DATA_SCORE_ID);
   }

   /**
    * Set player's score
    */
   public void setScore(int pScore) {
      this.entityData.set(DATA_SCORE_ID, pScore);
   }

   /**
    * Add to player's score
    */
   public void increaseScore(int pScore) {
      int i = this.getScore();
      this.entityData.set(DATA_SCORE_ID, i + pScore);
   }

   /**
    * Called when the mob's health reaches 0.
    */
   public void die(DamageSource pCause) {
      if (net.minecraftforge.common.ForgeHooks.onLivingDeath(this, pCause)) return;
      super.die(pCause);
      this.reapplyPosition();
      if (!this.isSpectator()) {
         this.dropAllDeathLoot(pCause);
      }

      if (pCause != null) {
         this.setDeltaMovement((double)(-Mth.cos((this.hurtDir + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F), (double)0.1F, (double)(-Mth.sin((this.hurtDir + this.getYRot()) * ((float)Math.PI / 180F)) * 0.1F));
      } else {
         this.setDeltaMovement(0.0D, 0.1D, 0.0D);
      }

      this.awardStat(Stats.DEATHS);
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      this.clearFire();
      this.setSharedFlagOnFire(false);
   }

   protected void dropEquipment() {
      super.dropEquipment();
      if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
         this.destroyVanishingCursedItems();
         this.inventory.dropAll();
      }

   }

   protected void destroyVanishingCursedItems() {
      for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
         ItemStack itemstack = this.inventory.getItem(i);
         if (!itemstack.isEmpty() && EnchantmentHelper.hasVanishingCurse(itemstack)) {
            this.inventory.removeItemNoUpdate(i);
         }
      }

   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      if (pDamageSource == DamageSource.ON_FIRE) {
         return SoundEvents.PLAYER_HURT_ON_FIRE;
      } else if (pDamageSource == DamageSource.DROWN) {
         return SoundEvents.PLAYER_HURT_DROWN;
      } else if (pDamageSource == DamageSource.SWEET_BERRY_BUSH) {
         return SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH;
      } else {
         return pDamageSource == DamageSource.FREEZE ? SoundEvents.PLAYER_HURT_FREEZE : SoundEvents.PLAYER_HURT;
      }
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.PLAYER_DEATH;
   }

   /**
    * Drops an item into the world.
    */
   @Nullable
   public ItemEntity drop(ItemStack pItemStack, boolean pUnused) {
      return net.minecraftforge.common.ForgeHooks.onPlayerTossEvent(this, pItemStack, pUnused);
   }

   /**
    * Creates and drops the provided item. Depending on the dropAround, it will drop teh item around the player, instead
    * of dropping the item from where the player is pointing at. Likewise, if traceItem is true, the dropped item entity
    * will have the thrower set as the player.
    */
   @Nullable
   public ItemEntity drop(ItemStack pDroppedItem, boolean pDropAround, boolean pTraceItem) {
      if (pDroppedItem.isEmpty()) {
         return null;
      } else {
         if (this.level.isClientSide) {
            this.swing(InteractionHand.MAIN_HAND);
         }

         double d0 = this.getEyeY() - (double)0.3F;
         ItemEntity itementity = new ItemEntity(this.level, this.getX(), d0, this.getZ(), pDroppedItem);
         itementity.setPickUpDelay(40);
         if (pTraceItem) {
            itementity.setThrower(this.getUUID());
         }

         if (pDropAround) {
            float f = this.random.nextFloat() * 0.5F;
            float f1 = this.random.nextFloat() * ((float)Math.PI * 2F);
            itementity.setDeltaMovement((double)(-Mth.sin(f1) * f), (double)0.2F, (double)(Mth.cos(f1) * f));
         } else {
            float f7 = 0.3F;
            float f8 = Mth.sin(this.getXRot() * ((float)Math.PI / 180F));
            float f2 = Mth.cos(this.getXRot() * ((float)Math.PI / 180F));
            float f3 = Mth.sin(this.getYRot() * ((float)Math.PI / 180F));
            float f4 = Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
            float f5 = this.random.nextFloat() * ((float)Math.PI * 2F);
            float f6 = 0.02F * this.random.nextFloat();
            itementity.setDeltaMovement((double)(-f3 * f2 * 0.3F) + Math.cos((double)f5) * (double)f6, (double)(-f8 * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(f4 * f2 * 0.3F) + Math.sin((double)f5) * (double)f6);
         }

         return itementity;
      }
   }

   @Deprecated //Use location sensitive version below
   public float getDestroySpeed(BlockState pState) {
      return getDigSpeed(pState, null);
   }

   public float getDigSpeed(BlockState pState, @Nullable BlockPos pos) {
      float f = this.inventory.getDestroySpeed(pState);
      if (f > 1.0F) {
         int i = EnchantmentHelper.getBlockEfficiency(this);
         ItemStack itemstack = this.getMainHandItem();
         if (i > 0 && !itemstack.isEmpty()) {
            f += (float)(i * i + 1);
         }
      }

      if (MobEffectUtil.hasDigSpeed(this)) {
         f *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
      }

      if (this.hasEffect(MobEffects.DIG_SLOWDOWN)) {
         float f1;
         switch(this.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier()) {
         case 0:
            f1 = 0.3F;
            break;
         case 1:
            f1 = 0.09F;
            break;
         case 2:
            f1 = 0.0027F;
            break;
         case 3:
         default:
            f1 = 8.1E-4F;
         }

         f *= f1;
      }

      if (this.isEyeInFluid(FluidTags.WATER) && !EnchantmentHelper.hasAquaAffinity(this)) {
         f /= 5.0F;
      }

      if (!this.onGround) {
         f /= 5.0F;
      }

      f = net.minecraftforge.event.ForgeEventFactory.getBreakSpeed(this, pState, f, pos);
      return f;
   }

   public boolean hasCorrectToolForDrops(BlockState p_36299_) {
      return net.minecraftforge.event.ForgeEventFactory.doPlayerHarvestCheck(this, p_36299_, !p_36299_.requiresCorrectToolForDrops() || this.inventory.getSelected().isCorrectToolForDrops(p_36299_));
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setUUID(createPlayerUUID(this.gameProfile));
      ListTag listtag = pCompound.getList("Inventory", 10);
      this.inventory.load(listtag);
      this.inventory.selected = pCompound.getInt("SelectedItemSlot");
      this.sleepCounter = pCompound.getShort("SleepTimer");
      this.experienceProgress = pCompound.getFloat("XpP");
      this.experienceLevel = pCompound.getInt("XpLevel");
      this.totalExperience = pCompound.getInt("XpTotal");
      this.enchantmentSeed = pCompound.getInt("XpSeed");
      if (this.enchantmentSeed == 0) {
         this.enchantmentSeed = this.random.nextInt();
      }

      this.setScore(pCompound.getInt("Score"));
      this.foodData.readAdditionalSaveData(pCompound);
      this.abilities.loadSaveData(pCompound);
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue((double)this.abilities.getWalkingSpeed());
      if (pCompound.contains("EnderItems", 9)) {
         this.enderChestInventory.fromTag(pCompound.getList("EnderItems", 10));
      }

      if (pCompound.contains("ShoulderEntityLeft", 10)) {
         this.setShoulderEntityLeft(pCompound.getCompound("ShoulderEntityLeft"));
      }

      if (pCompound.contains("ShoulderEntityRight", 10)) {
         this.setShoulderEntityRight(pCompound.getCompound("ShoulderEntityRight"));
      }

   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
      pCompound.put("Inventory", this.inventory.save(new ListTag()));
      pCompound.putInt("SelectedItemSlot", this.inventory.selected);
      pCompound.putShort("SleepTimer", (short)this.sleepCounter);
      pCompound.putFloat("XpP", this.experienceProgress);
      pCompound.putInt("XpLevel", this.experienceLevel);
      pCompound.putInt("XpTotal", this.totalExperience);
      pCompound.putInt("XpSeed", this.enchantmentSeed);
      pCompound.putInt("Score", this.getScore());
      this.foodData.addAdditionalSaveData(pCompound);
      this.abilities.addSaveData(pCompound);
      pCompound.put("EnderItems", this.enderChestInventory.createTag());
      if (!this.getShoulderEntityLeft().isEmpty()) {
         pCompound.put("ShoulderEntityLeft", this.getShoulderEntityLeft());
      }

      if (!this.getShoulderEntityRight().isEmpty()) {
         pCompound.put("ShoulderEntityRight", this.getShoulderEntityRight());
      }

   }

   /**
    * Returns whether this Entity is invulnerable to the given DamageSource.
    */
   public boolean isInvulnerableTo(DamageSource pSource) {
      if (super.isInvulnerableTo(pSource)) {
         return true;
      } else if (pSource == DamageSource.DROWN) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_DROWNING_DAMAGE);
      } else if (pSource.isFall()) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_FALL_DAMAGE);
      } else if (pSource.isFire()) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_FIRE_DAMAGE);
      } else if (pSource == DamageSource.FREEZE) {
         return !this.level.getGameRules().getBoolean(GameRules.RULE_FREEZE_DAMAGE);
      } else {
         return false;
      }
   }

   /**
    * Called when the entity is attacked.
    */
   public boolean hurt(DamageSource pSource, float pAmount) {
      if (!net.minecraftforge.common.ForgeHooks.onPlayerAttack(this, pSource, pAmount)) return false;
      if (this.isInvulnerableTo(pSource)) {
         return false;
      } else if (this.abilities.invulnerable && !pSource.isBypassInvul()) {
         return false;
      } else {
         this.noActionTime = 0;
         if (this.isDeadOrDying()) {
            return false;
         } else {
            this.removeEntitiesOnShoulder();
            if (pSource.scalesWithDifficulty()) {
               if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
                  pAmount = 0.0F;
               }

               if (this.level.getDifficulty() == Difficulty.EASY) {
                  pAmount = Math.min(pAmount / 2.0F + 1.0F, pAmount);
               }

               if (this.level.getDifficulty() == Difficulty.HARD) {
                  pAmount = pAmount * 3.0F / 2.0F;
               }
            }

            return pAmount == 0.0F ? false : super.hurt(pSource, pAmount);
         }
      }
   }

   protected void blockUsingShield(LivingEntity pEntity) {
      super.blockUsingShield(pEntity);
      if (pEntity.getMainHandItem().canDisableShield(this.useItem, this, pEntity)) {
         this.disableShield(true);
      }

   }

   public boolean canBeSeenAsEnemy() {
      return !this.getAbilities().invulnerable && super.canBeSeenAsEnemy();
   }

   public boolean canHarmPlayer(Player pOther) {
      Team team = this.getTeam();
      Team team1 = pOther.getTeam();
      if (team == null) {
         return true;
      } else {
         return !team.isAlliedTo(team1) ? true : team.isAllowFriendlyFire();
      }
   }

   protected void hurtArmor(DamageSource pDamageSource, float pDamage) {
      this.inventory.hurtArmor(pDamageSource, pDamage, Inventory.ALL_ARMOR_SLOTS);
   }

   protected void hurtHelmet(DamageSource pDamageSource, float pDamage) {
      this.inventory.hurtArmor(pDamageSource, pDamage, Inventory.HELMET_SLOT_ONLY);
   }

   protected void hurtCurrentlyUsedShield(float pDamage) {
      if (this.useItem.canPerformAction(net.minecraftforge.common.ToolActions.SHIELD_BLOCK)) {
         if (!this.level.isClientSide) {
            this.awardStat(Stats.ITEM_USED.get(this.useItem.getItem()));
         }

         if (pDamage >= 3.0F) {
            int i = 1 + Mth.floor(pDamage);
            InteractionHand interactionhand = this.getUsedItemHand();
            this.useItem.hurtAndBreak(i, this, (p_36149_) -> {
               p_36149_.broadcastBreakEvent(interactionhand);
               net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(this, this.useItem, interactionhand);
            });
            if (this.useItem.isEmpty()) {
               if (interactionhand == InteractionHand.MAIN_HAND) {
                  this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
               } else {
                  this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
               }

               this.useItem = ItemStack.EMPTY;
               this.playSound(SoundEvents.SHIELD_BREAK, 0.8F, 0.8F + this.level.random.nextFloat() * 0.4F);
            }
         }

      }
   }

   /**
    * Deals damage to the entity. This will take the armor of the entity into consideration before damaging the health
    * bar.
    */
   protected void actuallyHurt(DamageSource pDamageSrc, float pDamageAmount) {
      if (!this.isInvulnerableTo(pDamageSrc)) {
         pDamageAmount = net.minecraftforge.common.ForgeHooks.onLivingHurt(this, pDamageSrc, pDamageAmount);
         if (pDamageAmount <= 0) return;
         pDamageAmount = this.getDamageAfterArmorAbsorb(pDamageSrc, pDamageAmount);
         pDamageAmount = this.getDamageAfterMagicAbsorb(pDamageSrc, pDamageAmount);
         float f2 = Math.max(pDamageAmount - this.getAbsorptionAmount(), 0.0F);
         this.setAbsorptionAmount(this.getAbsorptionAmount() - (pDamageAmount - f2));
         f2 = net.minecraftforge.common.ForgeHooks.onLivingDamage(this, pDamageSrc, f2);
         float f = pDamageAmount - f2;
         if (f > 0.0F && f < 3.4028235E37F) {
            this.awardStat(Stats.DAMAGE_ABSORBED, Math.round(f * 10.0F));
         }

         if (f2 != 0.0F) {
            this.causeFoodExhaustion(pDamageSrc.getFoodExhaustion());
            float f1 = this.getHealth();
            this.getCombatTracker().recordDamage(pDamageSrc, f1, f2);
            this.setHealth(f1 - f2); // Forge: moved to fix MC-121048
            if (f2 < 3.4028235E37F) {
               this.awardStat(Stats.DAMAGE_TAKEN, Math.round(f2 * 10.0F));
            }

         }
      }
   }

   protected boolean onSoulSpeedBlock() {
      return !this.abilities.flying && super.onSoulSpeedBlock();
   }

   public void openTextEdit(SignBlockEntity pSignEntity) {
   }

   public void openMinecartCommandBlock(BaseCommandBlock pCommandEntity) {
   }

   public void openCommandBlock(CommandBlockEntity pCommandBlock) {
   }

   public void openStructureBlock(StructureBlockEntity pStructureEntity) {
   }

   public void openJigsawBlock(JigsawBlockEntity pJigsawEntity) {
   }

   public void openHorseInventory(AbstractHorse pHorse, Container pInventory) {
   }

   public OptionalInt openMenu(@Nullable MenuProvider p_36150_) {
      return OptionalInt.empty();
   }

   public void sendMerchantOffers(int p_36121_, MerchantOffers p_36122_, int p_36123_, int p_36124_, boolean p_36125_, boolean p_36126_) {
   }

   public void openItemGui(ItemStack pStack, InteractionHand pHand) {
   }

   public InteractionResult interactOn(Entity pEntityToInteractOn, InteractionHand pHand) {
      if (this.isSpectator()) {
         if (pEntityToInteractOn instanceof MenuProvider) {
            this.openMenu((MenuProvider)pEntityToInteractOn);
         }

         return InteractionResult.PASS;
      } else {
         InteractionResult cancelResult = net.minecraftforge.common.ForgeHooks.onInteractEntity(this, pEntityToInteractOn, pHand);
         if (cancelResult != null) return cancelResult;
         ItemStack itemstack = this.getItemInHand(pHand);
         ItemStack itemstack1 = itemstack.copy();
         InteractionResult interactionresult = pEntityToInteractOn.interact(this, pHand);
         if (interactionresult.consumesAction()) {
            if (this.abilities.instabuild && itemstack == this.getItemInHand(pHand) && itemstack.getCount() < itemstack1.getCount()) {
               itemstack.setCount(itemstack1.getCount());
            }

            if (!this.abilities.instabuild && itemstack.isEmpty()) {
               net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(this, itemstack1, pHand);
            }
            return interactionresult;
         } else {
            if (!itemstack.isEmpty() && pEntityToInteractOn instanceof LivingEntity) {
               if (this.abilities.instabuild) {
                  itemstack = itemstack1;
               }

               InteractionResult interactionresult1 = itemstack.interactLivingEntity(this, (LivingEntity)pEntityToInteractOn, pHand);
               if (interactionresult1.consumesAction()) {
                  if (itemstack.isEmpty() && !this.abilities.instabuild) {
                     net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(this, itemstack1, pHand);
                     this.setItemInHand(pHand, ItemStack.EMPTY);
                  }

                  return interactionresult1;
               }
            }

            return InteractionResult.PASS;
         }
      }
   }

   /**
    * Returns the Y Offset of this entity.
    */
   public double getMyRidingOffset() {
      return -0.35D;
   }

   public void removeVehicle() {
      super.removeVehicle();
      this.boardingCooldown = 0;
   }

   /**
    * Dead and sleeping entities cannot move
    */
   protected boolean isImmobile() {
      return super.isImmobile() || this.isSleeping();
   }

   public boolean isAffectedByFluids() {
      return !this.abilities.flying;
   }

   protected Vec3 maybeBackOffFromEdge(Vec3 pVec, MoverType pMover) {
      if (!this.abilities.flying && (pMover == MoverType.SELF || pMover == MoverType.PLAYER) && this.isStayingOnGroundSurface() && this.isAboveGround()) {
         double d0 = pVec.x;
         double d1 = pVec.z;
         double d2 = 0.05D;

         while(d0 != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(d0, (double)(-this.maxUpStep), 0.0D))) {
            if (d0 < 0.05D && d0 >= -0.05D) {
               d0 = 0.0D;
            } else if (d0 > 0.0D) {
               d0 -= 0.05D;
            } else {
               d0 += 0.05D;
            }
         }

         while(d1 != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(0.0D, (double)(-this.maxUpStep), d1))) {
            if (d1 < 0.05D && d1 >= -0.05D) {
               d1 = 0.0D;
            } else if (d1 > 0.0D) {
               d1 -= 0.05D;
            } else {
               d1 += 0.05D;
            }
         }

         while(d0 != 0.0D && d1 != 0.0D && this.level.noCollision(this, this.getBoundingBox().move(d0, (double)(-this.maxUpStep), d1))) {
            if (d0 < 0.05D && d0 >= -0.05D) {
               d0 = 0.0D;
            } else if (d0 > 0.0D) {
               d0 -= 0.05D;
            } else {
               d0 += 0.05D;
            }

            if (d1 < 0.05D && d1 >= -0.05D) {
               d1 = 0.0D;
            } else if (d1 > 0.0D) {
               d1 -= 0.05D;
            } else {
               d1 += 0.05D;
            }
         }

         pVec = new Vec3(d0, pVec.y, d1);
      }

      return pVec;
   }

   private boolean isAboveGround() {
      return this.onGround || this.fallDistance < this.maxUpStep && !this.level.noCollision(this, this.getBoundingBox().move(0.0D, (double)(this.fallDistance - this.maxUpStep), 0.0D));
   }

   /**
    * Attacks for the player the targeted entity with the currently equipped item.  The equipped item has hitEntity
    * called on it. Args: targetEntity
    */
   public void attack(Entity pTargetEntity) {
      if (!net.minecraftforge.common.ForgeHooks.onPlayerAttackTarget(this, pTargetEntity)) return;
      if (pTargetEntity.isAttackable()) {
         if (!pTargetEntity.skipAttackInteraction(this)) {
            float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float f1;
            if (pTargetEntity instanceof LivingEntity) {
               f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)pTargetEntity).getMobType());
            } else {
               f1 = EnchantmentHelper.getDamageBonus(this.getMainHandItem(), MobType.UNDEFINED);
            }

            float f2 = this.getAttackStrengthScale(0.5F);
            f *= 0.2F + f2 * f2 * 0.8F;
            f1 *= f2;
            this.resetAttackStrengthTicker();
            if (f > 0.0F || f1 > 0.0F) {
               boolean flag = f2 > 0.9F;
               boolean flag1 = false;
               float i = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK); // Forge: Initialize this value to the attack knockback attribute of the player, which is by default 0
               i += EnchantmentHelper.getKnockbackBonus(this);
               if (this.isSprinting() && flag) {
                  this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, this.getSoundSource(), 1.0F, 1.0F);
                  ++i;
                  flag1 = true;
               }

               boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround && !this.onClimbable() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && pTargetEntity instanceof LivingEntity;
               flag2 = flag2 && !this.isSprinting();
               net.minecraftforge.event.entity.player.CriticalHitEvent hitResult = net.minecraftforge.common.ForgeHooks.getCriticalHit(this, pTargetEntity, flag2, flag2 ? 1.5F : 1.0F);
               flag2 = hitResult != null;
               if (flag2) {
                  f *= hitResult.getDamageModifier();
               }

               f += f1;
               boolean flag3 = false;
               double d0 = (double)(this.walkDist - this.walkDistO);
               if (flag && !flag2 && !flag1 && this.onGround && d0 < (double)this.getSpeed()) {
                  ItemStack itemstack = this.getItemInHand(InteractionHand.MAIN_HAND);
                  flag3 = itemstack.canPerformAction(net.minecraftforge.common.ToolActions.SWORD_SWEEP);
               }

               float f4 = 0.0F;
               boolean flag4 = false;
               int j = EnchantmentHelper.getFireAspect(this);
               if (pTargetEntity instanceof LivingEntity) {
                  f4 = ((LivingEntity)pTargetEntity).getHealth();
                  if (j > 0 && !pTargetEntity.isOnFire()) {
                     flag4 = true;
                     pTargetEntity.setSecondsOnFire(1);
                  }
               }

               Vec3 vec3 = pTargetEntity.getDeltaMovement();
               boolean flag5 = pTargetEntity.hurt(DamageSource.playerAttack(this), f);
               if (flag5) {
                  if (i > 0) {
                     if (pTargetEntity instanceof LivingEntity) {
                        ((LivingEntity)pTargetEntity).knockback((double)((float)i * 0.5F), (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                     } else {
                        pTargetEntity.push((double)(-Mth.sin(this.getYRot() * ((float)Math.PI / 180F)) * (float)i * 0.5F), 0.1D, (double)(Mth.cos(this.getYRot() * ((float)Math.PI / 180F)) * (float)i * 0.5F));
                     }

                     this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
                     this.setSprinting(false);
                  }

                  if (flag3) {
                     float f3 = 1.0F + EnchantmentHelper.getSweepingDamageRatio(this) * f;

                     for(LivingEntity livingentity : this.level.getEntitiesOfClass(LivingEntity.class, this.getItemInHand(InteractionHand.MAIN_HAND).getSweepHitBox(this, pTargetEntity))) {
                        if (livingentity != this && livingentity != pTargetEntity && !this.isAlliedTo(livingentity) && (!(livingentity instanceof ArmorStand) || !((ArmorStand)livingentity).isMarker()) && this.distanceToSqr(livingentity) < 9.0D) {
                           livingentity.knockback((double)0.4F, (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
                           livingentity.hurt(DamageSource.playerAttack(this), f3);
                        }
                     }

                     this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, this.getSoundSource(), 1.0F, 1.0F);
                     this.sweepAttack();
                  }

                  if (pTargetEntity instanceof ServerPlayer && pTargetEntity.hurtMarked) {
                     ((ServerPlayer)pTargetEntity).connection.send(new ClientboundSetEntityMotionPacket(pTargetEntity));
                     pTargetEntity.hurtMarked = false;
                     pTargetEntity.setDeltaMovement(vec3);
                  }

                  if (flag2) {
                     this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, this.getSoundSource(), 1.0F, 1.0F);
                     this.crit(pTargetEntity);
                  }

                  if (!flag2 && !flag3) {
                     if (flag) {
                        this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_STRONG, this.getSoundSource(), 1.0F, 1.0F);
                     } else {
                        this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_WEAK, this.getSoundSource(), 1.0F, 1.0F);
                     }
                  }

                  if (f1 > 0.0F) {
                     this.magicCrit(pTargetEntity);
                  }

                  this.setLastHurtMob(pTargetEntity);
                  if (pTargetEntity instanceof LivingEntity) {
                     EnchantmentHelper.doPostHurtEffects((LivingEntity)pTargetEntity, this);
                  }

                  EnchantmentHelper.doPostDamageEffects(this, pTargetEntity);
                  ItemStack itemstack1 = this.getMainHandItem();
                  Entity entity = pTargetEntity;
                  if (pTargetEntity instanceof net.minecraftforge.entity.PartEntity) {
                     entity = ((net.minecraftforge.entity.PartEntity<?>) pTargetEntity).getParent();
                  }

                  if (!this.level.isClientSide && !itemstack1.isEmpty() && entity instanceof LivingEntity) {
                     ItemStack copy = itemstack1.copy();
                     itemstack1.hurtEnemy((LivingEntity)entity, this);
                     if (itemstack1.isEmpty()) {
                        net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(this, copy, InteractionHand.MAIN_HAND);
                        this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                     }
                  }

                  if (pTargetEntity instanceof LivingEntity) {
                     float f5 = f4 - ((LivingEntity)pTargetEntity).getHealth();
                     this.awardStat(Stats.DAMAGE_DEALT, Math.round(f5 * 10.0F));
                     if (j > 0) {
                        pTargetEntity.setSecondsOnFire(j * 4);
                     }

                     if (this.level instanceof ServerLevel && f5 > 2.0F) {
                        int k = (int)((double)f5 * 0.5D);
                        ((ServerLevel)this.level).sendParticles(ParticleTypes.DAMAGE_INDICATOR, pTargetEntity.getX(), pTargetEntity.getY(0.5D), pTargetEntity.getZ(), k, 0.1D, 0.0D, 0.1D, 0.2D);
                     }
                  }

                  this.causeFoodExhaustion(0.1F);
               } else {
                  this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, this.getSoundSource(), 1.0F, 1.0F);
                  if (flag4) {
                     pTargetEntity.clearFire();
                  }
               }
            }

         }
      }
   }

   protected void doAutoAttackOnTouch(LivingEntity p_36355_) {
      this.attack(p_36355_);
   }

   public void disableShield(boolean p_36385_) {
      float f = 0.25F + (float)EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
      if (p_36385_) {
         f += 0.75F;
      }

      if (this.random.nextFloat() < f) {
         this.getCooldowns().addCooldown(this.getUseItem().getItem(), 100);
         this.stopUsingItem();
         this.level.broadcastEntityEvent(this, (byte)30);
      }

   }

   /**
    * Called when the entity is dealt a critical hit.
    */
   public void crit(Entity pEntityHit) {
   }

   public void magicCrit(Entity pEntityHit) {
   }

   public void sweepAttack() {
      double d0 = (double)(-Mth.sin(this.getYRot() * ((float)Math.PI / 180F)));
      double d1 = (double)Mth.cos(this.getYRot() * ((float)Math.PI / 180F));
      if (this.level instanceof ServerLevel) {
         ((ServerLevel)this.level).sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX() + d0, this.getY(0.5D), this.getZ() + d1, 0, d0, 0.0D, d1, 0.0D);
      }

   }

   public void respawn() {
   }

   public void remove(Entity.RemovalReason pReason) {
      super.remove(pReason);
      this.inventoryMenu.removed(this);
      if (this.containerMenu != null) {
         this.containerMenu.removed(this);
      }

   }

   /**
    * returns true if this is an EntityPlayerSP, or the logged in player.
    */
   public boolean isLocalPlayer() {
      return false;
   }

   /**
    * Returns the GameProfile for this player
    */
   public GameProfile getGameProfile() {
      return this.gameProfile;
   }

   public Inventory getInventory() {
      return this.inventory;
   }

   public Abilities getAbilities() {
      return this.abilities;
   }

   public void updateTutorialInventoryAction(ItemStack p_150098_, ItemStack p_150099_, ClickAction p_150100_) {
   }

   public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos pAt) {
      this.startSleeping(pAt);
      this.sleepCounter = 0;
      return Either.right(Unit.INSTANCE);
   }

   public void stopSleepInBed(boolean p_36226_, boolean p_36227_) {
      net.minecraftforge.event.ForgeEventFactory.onPlayerWakeup(this, p_36226_, p_36227_);
      super.stopSleeping();
      if (this.level instanceof ServerLevel && p_36227_) {
         ((ServerLevel)this.level).updateSleepingPlayerList();
      }

      this.sleepCounter = p_36226_ ? 0 : 100;
   }

   public void stopSleeping() {
      this.stopSleepInBed(true, true);
   }

   public static Optional<Vec3> findRespawnPositionAndUseSpawnBlock(ServerLevel p_36131_, BlockPos p_36132_, float p_36133_, boolean p_36134_, boolean p_36135_) {
      BlockState blockstate = p_36131_.getBlockState(p_36132_);
      Block block = blockstate.getBlock();
      if (block instanceof RespawnAnchorBlock && blockstate.getValue(RespawnAnchorBlock.CHARGE) > 0 && RespawnAnchorBlock.canSetSpawn(p_36131_)) {
         Optional<Vec3> optional = RespawnAnchorBlock.findStandUpPosition(EntityType.PLAYER, p_36131_, p_36132_);
         if (!p_36135_ && optional.isPresent()) {
            p_36131_.setBlock(p_36132_, blockstate.setValue(RespawnAnchorBlock.CHARGE, Integer.valueOf(blockstate.getValue(RespawnAnchorBlock.CHARGE) - 1)), 3);
         }

         return optional;
      } else if (block instanceof BedBlock && BedBlock.canSetSpawn(p_36131_)) {
         return BedBlock.findStandUpPosition(EntityType.PLAYER, p_36131_, p_36132_, p_36133_);
      } else if (!p_36134_) {
         return blockstate.getRespawnPosition(EntityType.PLAYER, p_36131_, p_36132_, p_36133_, null);
      } else {
         boolean flag = block.isPossibleToRespawnInThis();
         boolean flag1 = p_36131_.getBlockState(p_36132_.above()).getBlock().isPossibleToRespawnInThis();
         return flag && flag1 ? Optional.of(new Vec3((double)p_36132_.getX() + 0.5D, (double)p_36132_.getY() + 0.1D, (double)p_36132_.getZ() + 0.5D)) : Optional.empty();
      }
   }

   /**
    * Returns whether or not the player is asleep and the screen has fully faded.
    */
   public boolean isSleepingLongEnough() {
      return this.isSleeping() && this.sleepCounter >= 100;
   }

   public int getSleepTimer() {
      return this.sleepCounter;
   }

   public void displayClientMessage(Component pChatComponent, boolean pActionBar) {
   }

   public void awardStat(ResourceLocation pStat) {
      this.awardStat(Stats.CUSTOM.get(pStat));
   }

   public void awardStat(ResourceLocation p_36223_, int p_36224_) {
      this.awardStat(Stats.CUSTOM.get(p_36223_), p_36224_);
   }

   /**
    * Add a stat once
    */
   public void awardStat(Stat<?> pStat) {
      this.awardStat(pStat, 1);
   }

   /**
    * Adds a value to a statistic field.
    */
   public void awardStat(Stat<?> pStat, int pAmount) {
   }

   public void resetStat(Stat<?> pStat) {
   }

   public int awardRecipes(Collection<Recipe<?>> p_36213_) {
      return 0;
   }

   public void awardRecipesByKey(ResourceLocation[] p_36228_) {
   }

   public int resetRecipes(Collection<Recipe<?>> p_36263_) {
      return 0;
   }

   /**
    * Causes this entity to do an upwards motion (jumping).
    */
   public void jumpFromGround() {
      super.jumpFromGround();
      this.awardStat(Stats.JUMP);
      if (this.isSprinting()) {
         this.causeFoodExhaustion(0.2F);
      } else {
         this.causeFoodExhaustion(0.05F);
      }

   }

   public void travel(Vec3 pTravelVector) {
      double d0 = this.getX();
      double d1 = this.getY();
      double d2 = this.getZ();
      if (this.isSwimming() && !this.isPassenger()) {
         double d3 = this.getLookAngle().y;
         double d4 = d3 < -0.2D ? 0.085D : 0.06D;
         if (d3 <= 0.0D || this.jumping || !this.level.getBlockState(new BlockPos(this.getX(), this.getY() + 1.0D - 0.1D, this.getZ())).getFluidState().isEmpty()) {
            Vec3 vec31 = this.getDeltaMovement();
            this.setDeltaMovement(vec31.add(0.0D, (d3 - vec31.y) * d4, 0.0D));
         }
      }

      if (this.abilities.flying && !this.isPassenger()) {
         double d5 = this.getDeltaMovement().y;
         float f = this.flyingSpeed;
         this.flyingSpeed = this.abilities.getFlyingSpeed() * (float)(this.isSprinting() ? 2 : 1);
         super.travel(pTravelVector);
         Vec3 vec3 = this.getDeltaMovement();
         this.setDeltaMovement(vec3.x, d5 * 0.6D, vec3.z);
         this.flyingSpeed = f;
         this.resetFallDistance();
         this.setSharedFlag(7, false);
      } else {
         super.travel(pTravelVector);
      }

      this.checkMovementStatistics(this.getX() - d0, this.getY() - d1, this.getZ() - d2);
   }

   public void updateSwimming() {
      if (this.abilities.flying) {
         this.setSwimming(false);
      } else {
         super.updateSwimming();
      }

   }

   protected boolean freeAt(BlockPos pPos) {
      return !this.level.getBlockState(pPos).isSuffocating(this.level, pPos);
   }

   /**
    * the movespeed used for the new AI system
    */
   public float getSpeed() {
      return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
   }

   /**
    * Adds a value to a movement statistic field - like run, walk, swin or climb.
    */
   public void checkMovementStatistics(double p_36379_, double p_36380_, double p_36381_) {
      if (!this.isPassenger()) {
         if (this.isSwimming()) {
            int i = Math.round((float)Math.sqrt(p_36379_ * p_36379_ + p_36380_ * p_36380_ + p_36381_ * p_36381_) * 100.0F);
            if (i > 0) {
               this.awardStat(Stats.SWIM_ONE_CM, i);
               this.causeFoodExhaustion(0.01F * (float)i * 0.01F);
            }
         } else if (this.isEyeInFluid(FluidTags.WATER)) {
            int j = Math.round((float)Math.sqrt(p_36379_ * p_36379_ + p_36380_ * p_36380_ + p_36381_ * p_36381_) * 100.0F);
            if (j > 0) {
               this.awardStat(Stats.WALK_UNDER_WATER_ONE_CM, j);
               this.causeFoodExhaustion(0.01F * (float)j * 0.01F);
            }
         } else if (this.isInWater()) {
            int k = Math.round((float)Math.sqrt(p_36379_ * p_36379_ + p_36381_ * p_36381_) * 100.0F);
            if (k > 0) {
               this.awardStat(Stats.WALK_ON_WATER_ONE_CM, k);
               this.causeFoodExhaustion(0.01F * (float)k * 0.01F);
            }
         } else if (this.onClimbable()) {
            if (p_36380_ > 0.0D) {
               this.awardStat(Stats.CLIMB_ONE_CM, (int)Math.round(p_36380_ * 100.0D));
            }
         } else if (this.onGround) {
            int l = Math.round((float)Math.sqrt(p_36379_ * p_36379_ + p_36381_ * p_36381_) * 100.0F);
            if (l > 0) {
               if (this.isSprinting()) {
                  this.awardStat(Stats.SPRINT_ONE_CM, l);
                  this.causeFoodExhaustion(0.1F * (float)l * 0.01F);
               } else if (this.isCrouching()) {
                  this.awardStat(Stats.CROUCH_ONE_CM, l);
                  this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
               } else {
                  this.awardStat(Stats.WALK_ONE_CM, l);
                  this.causeFoodExhaustion(0.0F * (float)l * 0.01F);
               }
            }
         } else if (this.isFallFlying()) {
            int i1 = Math.round((float)Math.sqrt(p_36379_ * p_36379_ + p_36380_ * p_36380_ + p_36381_ * p_36381_) * 100.0F);
            this.awardStat(Stats.AVIATE_ONE_CM, i1);
         } else {
            int j1 = Math.round((float)Math.sqrt(p_36379_ * p_36379_ + p_36381_ * p_36381_) * 100.0F);
            if (j1 > 25) {
               this.awardStat(Stats.FLY_ONE_CM, j1);
            }
         }

      }
   }

   /**
    * Adds a value to a mounted movement statistic field - by minecart, boat, or pig.
    */
   private void checkRidingStatistics(double p_36388_, double p_36389_, double p_36390_) {
      if (this.isPassenger()) {
         int i = Math.round((float)Math.sqrt(p_36388_ * p_36388_ + p_36389_ * p_36389_ + p_36390_ * p_36390_) * 100.0F);
         if (i > 0) {
            Entity entity = this.getVehicle();
            if (entity instanceof AbstractMinecart) {
               this.awardStat(Stats.MINECART_ONE_CM, i);
            } else if (entity instanceof Boat) {
               this.awardStat(Stats.BOAT_ONE_CM, i);
            } else if (entity instanceof Pig) {
               this.awardStat(Stats.PIG_ONE_CM, i);
            } else if (entity instanceof AbstractHorse) {
               this.awardStat(Stats.HORSE_ONE_CM, i);
            } else if (entity instanceof Strider) {
               this.awardStat(Stats.STRIDER_ONE_CM, i);
            }
         }
      }

   }

   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      if (this.abilities.mayfly) {
         net.minecraftforge.event.ForgeEventFactory.onPlayerFall(this, pFallDistance, pFallDistance);
         return false;
      } else {
         if (pFallDistance >= 2.0F) {
            this.awardStat(Stats.FALL_ONE_CM, (int)Math.round((double)pFallDistance * 100.0D));
         }

         return super.causeFallDamage(pFallDistance, pMultiplier, pSource);
      }
   }

   public boolean tryToStartFallFlying() {
      if (!this.onGround && !this.isFallFlying() && !this.isInWater() && !this.hasEffect(MobEffects.LEVITATION)) {
         ItemStack itemstack = this.getItemBySlot(EquipmentSlot.CHEST);
         if (itemstack.canElytraFly(this)) {
            this.startFallFlying();
            return true;
         }
      }

      return false;
   }

   public void startFallFlying() {
      this.setSharedFlag(7, true);
   }

   public void stopFallFlying() {
      this.setSharedFlag(7, true);
      this.setSharedFlag(7, false);
   }

   /**
    * Plays the {@link #getSplashSound() splash sound}, and the {@link ParticleType#WATER_BUBBLE} and {@link
    * ParticleType#WATER_SPLASH} particles.
    */
   protected void doWaterSplashEffect() {
      if (!this.isSpectator()) {
         super.doWaterSplashEffect();
      }

   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.PLAYER_SMALL_FALL, SoundEvents.PLAYER_BIG_FALL);
   }

   public void killed(ServerLevel pLevel, LivingEntity pKilledEntity) {
      this.awardStat(Stats.ENTITY_KILLED.get(pKilledEntity.getType()));
   }

   public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {
      if (!this.abilities.flying) {
         super.makeStuckInBlock(pState, pMotionMultiplier);
      }

   }

   public void giveExperiencePoints(int p_36291_) {
      net.minecraftforge.event.entity.player.PlayerXpEvent.XpChange event = new net.minecraftforge.event.entity.player.PlayerXpEvent.XpChange(this, p_36291_);
      if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) return;
      p_36291_ = event.getAmount();

      this.increaseScore(p_36291_);
      this.experienceProgress += (float)p_36291_ / (float)this.getXpNeededForNextLevel();
      this.totalExperience = Mth.clamp(this.totalExperience + p_36291_, 0, Integer.MAX_VALUE);

      while(this.experienceProgress < 0.0F) {
         float f = this.experienceProgress * (float)this.getXpNeededForNextLevel();
         if (this.experienceLevel > 0) {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 1.0F + f / (float)this.getXpNeededForNextLevel();
         } else {
            this.giveExperienceLevels(-1);
            this.experienceProgress = 0.0F;
         }
      }

      while(this.experienceProgress >= 1.0F) {
         this.experienceProgress = (this.experienceProgress - 1.0F) * (float)this.getXpNeededForNextLevel();
         this.giveExperienceLevels(1);
         this.experienceProgress /= (float)this.getXpNeededForNextLevel();
      }

   }

   public int getEnchantmentSeed() {
      return this.enchantmentSeed;
   }

   public void onEnchantmentPerformed(ItemStack pEnchantedItem, int pCost) {
      giveExperienceLevels(-pCost);
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      this.enchantmentSeed = this.random.nextInt();
   }

   /**
    * Add experience levels to this player.
    */
   public void giveExperienceLevels(int pLevels) {
      net.minecraftforge.event.entity.player.PlayerXpEvent.LevelChange event = new net.minecraftforge.event.entity.player.PlayerXpEvent.LevelChange(this, pLevels);
      if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event)) return;
      pLevels = event.getLevels();

      this.experienceLevel += pLevels;
      if (this.experienceLevel < 0) {
         this.experienceLevel = 0;
         this.experienceProgress = 0.0F;
         this.totalExperience = 0;
      }

      if (pLevels > 0 && this.experienceLevel % 5 == 0 && (float)this.lastLevelUpTime < (float)this.tickCount - 100.0F) {
         float f = this.experienceLevel > 30 ? 1.0F : (float)this.experienceLevel / 30.0F;
         this.level.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_LEVELUP, this.getSoundSource(), f * 0.75F, 1.0F);
         this.lastLevelUpTime = this.tickCount;
      }

   }

   /**
    * This method returns the cap amount of experience that the experience bar can hold. With each level, the experience
    * cap on the player's experience bar is raised by 10.
    */
   public int getXpNeededForNextLevel() {
      if (this.experienceLevel >= 30) {
         return 112 + (this.experienceLevel - 30) * 9;
      } else {
         return this.experienceLevel >= 15 ? 37 + (this.experienceLevel - 15) * 5 : 7 + this.experienceLevel * 2;
      }
   }

   /**
    * increases exhaustion level by supplied amount
    */
   public void causeFoodExhaustion(float pExhaustion) {
      if (!this.abilities.invulnerable) {
         if (!this.level.isClientSide) {
            this.foodData.addExhaustion(pExhaustion);
         }

      }
   }

   /**
    * Returns the player's FoodStats object.
    */
   public FoodData getFoodData() {
      return this.foodData;
   }

   public boolean canEat(boolean pIgnoreHunger) {
      return this.abilities.invulnerable || pIgnoreHunger || this.foodData.needsFood();
   }

   /**
    * Checks if the player's health is not full and not zero.
    */
   public boolean isHurt() {
      return this.getHealth() > 0.0F && this.getHealth() < this.getMaxHealth();
   }

   public boolean mayBuild() {
      return this.abilities.mayBuild;
   }

   /**
    * Returns whether this player can modify the block at a certain location with the given stack.
    * <p>
    * The position being queried is {@code pos.offset(facing.getOpposite()))}.
    * 
    * @return Whether this player may modify the queried location in the current world
    * @see ItemStack#canPlaceOn(Block)
    * @see ItemStack#canEditBlocks()
    * @see PlayerCapabilities#allowEdit
    */
   public boolean mayUseItemAt(BlockPos pPos, Direction pFacing, ItemStack pStack) {
      if (this.abilities.mayBuild) {
         return true;
      } else {
         BlockPos blockpos = pPos.relative(pFacing.getOpposite());
         BlockInWorld blockinworld = new BlockInWorld(this.level, blockpos, false);
         return pStack.hasAdventureModePlaceTagForBlock(this.level.getTagManager(), blockinworld);
      }
   }

   /**
    * Get the experience points the entity currently has.
    */
   protected int getExperienceReward(Player pPlayer) {
      if (!this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) && !this.isSpectator()) {
         int i = this.experienceLevel * 7;
         return i > 100 ? 100 : i;
      } else {
         return 0;
      }
   }

   /**
    * Only use is to identify if class is an instance of player for experience dropping
    */
   protected boolean isAlwaysExperienceDropper() {
      return true;
   }

   public boolean shouldShowName() {
      return true;
   }

   protected Entity.MovementEmission getMovementEmission() {
      return this.abilities.flying || this.onGround && this.isDiscrete() ? Entity.MovementEmission.NONE : Entity.MovementEmission.ALL;
   }

   /**
    * Sends the player's abilities to the server (if there is one).
    */
   public void onUpdateAbilities() {
   }

   public Component getName() {
      return new TextComponent(this.gameProfile.getName());
   }

   /**
    * Returns the InventoryEnderChest of this player.
    */
   public PlayerEnderChestContainer getEnderChestInventory() {
      return this.enderChestInventory;
   }

   public ItemStack getItemBySlot(EquipmentSlot pSlot) {
      if (pSlot == EquipmentSlot.MAINHAND) {
         return this.inventory.getSelected();
      } else if (pSlot == EquipmentSlot.OFFHAND) {
         return this.inventory.offhand.get(0);
      } else {
         return pSlot.getType() == EquipmentSlot.Type.ARMOR ? this.inventory.armor.get(pSlot.getIndex()) : ItemStack.EMPTY;
      }
   }

   public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
      this.verifyEquippedItem(pStack);
      if (pSlot == EquipmentSlot.MAINHAND) {
         this.equipEventAndSound(pStack);
         this.inventory.items.set(this.inventory.selected, pStack);
      } else if (pSlot == EquipmentSlot.OFFHAND) {
         this.equipEventAndSound(pStack);
         this.inventory.offhand.set(0, pStack);
      } else if (pSlot.getType() == EquipmentSlot.Type.ARMOR) {
         this.equipEventAndSound(pStack);
         this.inventory.armor.set(pSlot.getIndex(), pStack);
      }

   }

   public boolean addItem(ItemStack p_36357_) {
      this.equipEventAndSound(p_36357_);
      return this.inventory.add(p_36357_);
   }

   public Iterable<ItemStack> getHandSlots() {
      return Lists.newArrayList(this.getMainHandItem(), this.getOffhandItem());
   }

   public Iterable<ItemStack> getArmorSlots() {
      return this.inventory.armor;
   }

   public boolean setEntityOnShoulder(CompoundTag p_36361_) {
      if (!this.isPassenger() && this.onGround && !this.isInWater() && !this.isInPowderSnow) {
         if (this.getShoulderEntityLeft().isEmpty()) {
            this.setShoulderEntityLeft(p_36361_);
            this.timeEntitySatOnShoulder = this.level.getGameTime();
            return true;
         } else if (this.getShoulderEntityRight().isEmpty()) {
            this.setShoulderEntityRight(p_36361_);
            this.timeEntitySatOnShoulder = this.level.getGameTime();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   protected void removeEntitiesOnShoulder() {
      if (this.timeEntitySatOnShoulder + 20L < this.level.getGameTime()) {
         this.respawnEntityOnShoulder(this.getShoulderEntityLeft());
         this.setShoulderEntityLeft(new CompoundTag());
         this.respawnEntityOnShoulder(this.getShoulderEntityRight());
         this.setShoulderEntityRight(new CompoundTag());
      }

   }

   private void respawnEntityOnShoulder(CompoundTag p_36371_) {
      if (!this.level.isClientSide && !p_36371_.isEmpty()) {
         EntityType.create(p_36371_, this.level).ifPresent((p_36293_) -> {
            if (p_36293_ instanceof TamableAnimal) {
               ((TamableAnimal)p_36293_).setOwnerUUID(this.uuid);
            }

            p_36293_.setPos(this.getX(), this.getY() + (double)0.7F, this.getZ());
            ((ServerLevel)this.level).addWithUUID(p_36293_);
         });
      }

   }

   /**
    * Returns true if the player is in spectator mode.
    */
   public abstract boolean isSpectator();

   public boolean isSwimming() {
      return !this.abilities.flying && !this.isSpectator() && super.isSwimming();
   }

   public abstract boolean isCreative();

   public boolean isPushedByFluid() {
      return !this.abilities.flying;
   }

   public Scoreboard getScoreboard() {
      return this.level.getScoreboard();
   }

   public Component getDisplayName() {
      if (this.displayname == null) this.displayname = net.minecraftforge.event.ForgeEventFactory.getPlayerDisplayName(this, this.getName());
      MutableComponent mutablecomponent = new TextComponent("");
      mutablecomponent = prefixes.stream().reduce(mutablecomponent, MutableComponent::append);
      mutablecomponent = mutablecomponent.append(PlayerTeam.formatNameForTeam(this.getTeam(), this.displayname));
      mutablecomponent = suffixes.stream().reduce(mutablecomponent, MutableComponent::append);
      return this.decorateDisplayNameComponent(mutablecomponent);
   }

   private MutableComponent decorateDisplayNameComponent(MutableComponent p_36219_) {
      String s = this.getGameProfile().getName();
      return p_36219_.withStyle((p_36212_) -> {
         return p_36212_.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + s + " ")).withHoverEvent(this.createHoverEvent()).withInsertion(s);
      });
   }

   /**
    * Returns a String to use as this entity's name in the scoreboard/entity selector systems
    */
   public String getScoreboardName() {
      return this.getGameProfile().getName();
   }

   public float getStandingEyeHeight(Pose pPose, EntityDimensions pSize) {
      switch(pPose) {
      case SWIMMING:
      case FALL_FLYING:
      case SPIN_ATTACK:
         return 0.4F;
      case CROUCHING:
         return 1.27F;
      default:
         return 1.62F;
      }
   }

   public void setAbsorptionAmount(float pAmount) {
      if (pAmount < 0.0F) {
         pAmount = 0.0F;
      }

      this.getEntityData().set(DATA_PLAYER_ABSORPTION_ID, pAmount);
   }

   /**
    * Returns the amount of health added by the Absorption effect.
    */
   public float getAbsorptionAmount() {
      return this.getEntityData().get(DATA_PLAYER_ABSORPTION_ID);
   }

   /**
    * Gets a players UUID given their GameProfie
    */
   public static UUID createPlayerUUID(GameProfile pProfile) {
      UUID uuid = pProfile.getId();
      if (uuid == null) {
         uuid = createPlayerUUID(pProfile.getName());
      }

      return uuid;
   }

   public static UUID createPlayerUUID(String pUsername) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + pUsername).getBytes(StandardCharsets.UTF_8));
   }

   public boolean isModelPartShown(PlayerModelPart pPart) {
      return (this.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION) & pPart.getMask()) == pPart.getMask();
   }

   public SlotAccess getSlot(int pSlot) {
      if (pSlot >= 0 && pSlot < this.inventory.items.size()) {
         return SlotAccess.forContainer(this.inventory, pSlot);
      } else {
         int i = pSlot - 200;
         return i >= 0 && i < this.enderChestInventory.getContainerSize() ? SlotAccess.forContainer(this.enderChestInventory, i) : super.getSlot(pSlot);
      }
   }

   /**
    * Whether the "reducedDebugInfo" option is active for this player.
    */
   public boolean isReducedDebugInfo() {
      return this.reducedDebugInfo;
   }

   public void setReducedDebugInfo(boolean pReducedDebug) {
      this.reducedDebugInfo = pReducedDebug;
   }

   public void setRemainingFireTicks(int pTicks) {
      super.setRemainingFireTicks(this.abilities.invulnerable ? Math.min(pTicks, 1) : pTicks);
   }

   public HumanoidArm getMainArm() {
      return this.entityData.get(DATA_PLAYER_MAIN_HAND) == 0 ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public void setMainArm(HumanoidArm pHand) {
      this.entityData.set(DATA_PLAYER_MAIN_HAND, (byte)(pHand == HumanoidArm.LEFT ? 0 : 1));
   }

   public CompoundTag getShoulderEntityLeft() {
      return this.entityData.get(DATA_SHOULDER_LEFT);
   }

   protected void setShoulderEntityLeft(CompoundTag pTag) {
      this.entityData.set(DATA_SHOULDER_LEFT, pTag);
   }

   public CompoundTag getShoulderEntityRight() {
      return this.entityData.get(DATA_SHOULDER_RIGHT);
   }

   protected void setShoulderEntityRight(CompoundTag pTag) {
      this.entityData.set(DATA_SHOULDER_RIGHT, pTag);
   }

   public float getCurrentItemAttackStrengthDelay() {
      return (float)(1.0D / this.getAttributeValue(Attributes.ATTACK_SPEED) * 20.0D);
   }

   /**
    * Returns the percentage of attack power available based on the cooldown (zero to one).
    */
   public float getAttackStrengthScale(float pAdjustTicks) {
      return Mth.clamp(((float)this.attackStrengthTicker + pAdjustTicks) / this.getCurrentItemAttackStrengthDelay(), 0.0F, 1.0F);
   }

   public void resetAttackStrengthTicker() {
      this.attackStrengthTicker = 0;
   }

   public ItemCooldowns getCooldowns() {
      return this.cooldowns;
   }

   protected float getBlockSpeedFactor() {
      return !this.abilities.flying && !this.isFallFlying() ? super.getBlockSpeedFactor() : 1.0F;
   }

   public float getLuck() {
      return (float)this.getAttributeValue(Attributes.LUCK);
   }

   public boolean canUseGameMasterBlocks() {
      return this.abilities.instabuild && this.getPermissionLevel() >= 2;
   }

   public boolean canTakeItem(ItemStack pItemstack) {
      EquipmentSlot equipmentslot = Mob.getEquipmentSlotForItem(pItemstack);
      return this.getItemBySlot(equipmentslot).isEmpty();
   }

   public EntityDimensions getDimensions(Pose pPose) {
      return POSES.getOrDefault(pPose, STANDING_DIMENSIONS);
   }

   public ImmutableList<Pose> getDismountPoses() {
      return ImmutableList.of(Pose.STANDING, Pose.CROUCHING, Pose.SWIMMING);
   }

   public ItemStack getProjectile(ItemStack pShootable) {
      if (!(pShootable.getItem() instanceof ProjectileWeaponItem)) {
         return ItemStack.EMPTY;
      } else {
         Predicate<ItemStack> predicate = ((ProjectileWeaponItem)pShootable.getItem()).getSupportedHeldProjectiles();
         ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
         if (!itemstack.isEmpty()) {
            return itemstack;
         } else {
            predicate = ((ProjectileWeaponItem)pShootable.getItem()).getAllSupportedProjectiles();

            for(int i = 0; i < this.inventory.getContainerSize(); ++i) {
               ItemStack itemstack1 = this.inventory.getItem(i);
               if (predicate.test(itemstack1)) {
                  return itemstack1;
               }
            }

            return this.abilities.instabuild ? new ItemStack(Items.ARROW) : ItemStack.EMPTY;
         }
      }
   }

   public ItemStack eat(Level p_36185_, ItemStack p_36186_) {
      this.getFoodData().eat(p_36186_.getItem(), p_36186_);
      this.awardStat(Stats.ITEM_USED.get(p_36186_.getItem()));
      p_36185_.playSound((Player)null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, p_36185_.random.nextFloat() * 0.1F + 0.9F);
      if (this instanceof ServerPlayer) {
         CriteriaTriggers.CONSUME_ITEM.trigger((ServerPlayer)this, p_36186_);
      }

      return super.eat(p_36185_, p_36186_);
   }

   protected boolean shouldRemoveSoulSpeed(BlockState p_36262_) {
      return this.abilities.flying || super.shouldRemoveSoulSpeed(p_36262_);
   }

   public Vec3 getRopeHoldPosition(float pPartialTicks) {
      double d0 = 0.22D * (this.getMainArm() == HumanoidArm.RIGHT ? -1.0D : 1.0D);
      float f = Mth.lerp(pPartialTicks * 0.5F, this.getXRot(), this.xRotO) * ((float)Math.PI / 180F);
      float f1 = Mth.lerp(pPartialTicks, this.yBodyRotO, this.yBodyRot) * ((float)Math.PI / 180F);
      if (!this.isFallFlying() && !this.isAutoSpinAttack()) {
         if (this.isVisuallySwimming()) {
            return this.getPosition(pPartialTicks).add((new Vec3(d0, 0.2D, -0.15D)).xRot(-f).yRot(-f1));
         } else {
            double d5 = this.getBoundingBox().getYsize() - 1.0D;
            double d6 = this.isCrouching() ? -0.2D : 0.07D;
            return this.getPosition(pPartialTicks).add((new Vec3(d0, d5, d6)).yRot(-f1));
         }
      } else {
         Vec3 vec3 = this.getViewVector(pPartialTicks);
         Vec3 vec31 = this.getDeltaMovement();
         double d1 = vec31.horizontalDistanceSqr();
         double d2 = vec3.horizontalDistanceSqr();
         float f2;
         if (d1 > 0.0D && d2 > 0.0D) {
            double d3 = (vec31.x * vec3.x + vec31.z * vec3.z) / Math.sqrt(d1 * d2);
            double d4 = vec31.x * vec3.z - vec31.z * vec3.x;
            f2 = (float)(Math.signum(d4) * Math.acos(d3));
         } else {
            f2 = 0.0F;
         }

         return this.getPosition(pPartialTicks).add((new Vec3(d0, -0.11D, 0.85D)).zRot(-f2).xRot(-f).yRot(-f1));
      }
   }

   public boolean isAlwaysTicking() {
      return true;
   }

   public boolean isScoping() {
      return this.isUsingItem() && this.getUseItem().is(Items.SPYGLASS);
   }

   public boolean shouldBeSaved() {
      return false;
   }

   public static enum BedSleepingProblem {
      NOT_POSSIBLE_HERE,
      NOT_POSSIBLE_NOW(new TranslatableComponent("block.minecraft.bed.no_sleep")),
      TOO_FAR_AWAY(new TranslatableComponent("block.minecraft.bed.too_far_away")),
      OBSTRUCTED(new TranslatableComponent("block.minecraft.bed.obstructed")),
      OTHER_PROBLEM,
      NOT_SAFE(new TranslatableComponent("block.minecraft.bed.not_safe"));

      @Nullable
      private final Component message;

      private BedSleepingProblem() {
         this.message = null;
      }

      private BedSleepingProblem(Component p_36422_) {
         this.message = p_36422_;
      }

      @Nullable
      public Component getMessage() {
         return this.message;
      }
   }

   // =========== FORGE START ==============//
   public Collection<MutableComponent> getPrefixes() {
       return this.prefixes;
   }

   public Collection<MutableComponent> getSuffixes() {
       return this.suffixes;
   }

   private Component displayname = null;
   /**
    * Force the displayed name to refresh, by firing {@link net.minecraftforge.event.entity.player.PlayerEvent.NameFormat}, using the real player name as event parameter.
    */
   public void refreshDisplayName() {
      this.displayname = net.minecraftforge.event.ForgeEventFactory.getPlayerDisplayName(this, this.getName());
   }

   private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>
         playerMainHandler = net.minecraftforge.common.util.LazyOptional.of(
               () -> new net.minecraftforge.items.wrapper.PlayerMainInvWrapper(inventory));

   private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>
         playerEquipmentHandler = net.minecraftforge.common.util.LazyOptional.of(
               () -> new net.minecraftforge.items.wrapper.CombinedInvWrapper(
                     new net.minecraftforge.items.wrapper.PlayerArmorInvWrapper(inventory),
                     new net.minecraftforge.items.wrapper.PlayerOffhandInvWrapper(inventory)));

   private final net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>
         playerJoinedHandler = net.minecraftforge.common.util.LazyOptional.of(
               () -> new net.minecraftforge.items.wrapper.PlayerInvWrapper(inventory));

   @Override
   public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
      if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
         if (facing == null) return playerJoinedHandler.cast();
         else if (facing.getAxis().isVertical()) return playerMainHandler.cast();
         else if (facing.getAxis().isHorizontal()) return playerEquipmentHandler.cast();
      }
      return super.getCapability(capability, facing);
   }

   /**
    * Force a pose for the player. If set, the vanilla pose determination and clearance check is skipped. Make sure the pose is clear yourself (e.g. in PlayerTick).
    * This has to be set just once, do not set it every tick.
    * Make sure to clear (null) the pose if not required anymore and only use if necessary.
    */
   public void setForcedPose(@Nullable Pose pose) {
      this.forcedPose = pose;
   }

   /**
    * @return The forced pose if set, null otherwise
    */
   @Nullable
   public Pose getForcedPose() {
      return this.forcedPose;
   }
}
