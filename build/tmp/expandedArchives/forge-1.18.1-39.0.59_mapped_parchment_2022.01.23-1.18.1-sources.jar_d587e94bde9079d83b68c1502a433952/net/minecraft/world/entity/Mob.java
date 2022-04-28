package net.minecraft.world.entity;

import com.google.common.collect.Maps;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.JumpControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.sensing.Sensing;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.LootContext;

public abstract class Mob extends LivingEntity {
   private static final EntityDataAccessor<Byte> DATA_MOB_FLAGS_ID = SynchedEntityData.defineId(Mob.class, EntityDataSerializers.BYTE);
   private static final int MOB_FLAG_NO_AI = 1;
   private static final int MOB_FLAG_LEFTHANDED = 2;
   private static final int MOB_FLAG_AGGRESSIVE = 4;
   public static final float MAX_WEARING_ARMOR_CHANCE = 0.15F;
   public static final float MAX_PICKUP_LOOT_CHANCE = 0.55F;
   public static final float MAX_ENCHANTED_ARMOR_CHANCE = 0.5F;
   public static final float MAX_ENCHANTED_WEAPON_CHANCE = 0.25F;
   public static final String LEASH_TAG = "Leash";
   private static final int PICKUP_REACH = 1;
   public static final float DEFAULT_EQUIPMENT_DROP_CHANCE = 0.085F;
   public static final int UPDATE_GOAL_SELECTOR_EVERY_N_TICKS = 2;
   public int ambientSoundTime;
   protected int xpReward;
   protected LookControl lookControl;
   protected MoveControl moveControl;
   protected JumpControl jumpControl;
   private final BodyRotationControl bodyRotationControl;
   protected PathNavigation navigation;
   public final GoalSelector goalSelector;
   public final GoalSelector targetSelector;
   @Nullable
   private LivingEntity target;
   private final Sensing sensing;
   private final NonNullList<ItemStack> handItems = NonNullList.withSize(2, ItemStack.EMPTY);
   protected final float[] handDropChances = new float[2];
   private final NonNullList<ItemStack> armorItems = NonNullList.withSize(4, ItemStack.EMPTY);
   protected final float[] armorDropChances = new float[4];
   private boolean canPickUpLoot;
   private boolean persistenceRequired;
   private final Map<BlockPathTypes, Float> pathfindingMalus = Maps.newEnumMap(BlockPathTypes.class);
   @Nullable
   private ResourceLocation lootTable;
   private long lootTableSeed;
   @Nullable
   private Entity leashHolder;
   private int delayedLeashHolderId;
   @Nullable
   private CompoundTag leashInfoTag;
   private BlockPos restrictCenter = BlockPos.ZERO;
   private float restrictRadius = -1.0F;

   protected Mob(EntityType<? extends Mob> p_21368_, Level p_21369_) {
      super(p_21368_, p_21369_);
      this.goalSelector = new GoalSelector(p_21369_.getProfilerSupplier());
      this.targetSelector = new GoalSelector(p_21369_.getProfilerSupplier());
      this.lookControl = new LookControl(this);
      this.moveControl = new MoveControl(this);
      this.jumpControl = new JumpControl(this);
      this.bodyRotationControl = this.createBodyControl();
      this.navigation = this.createNavigation(p_21369_);
      this.sensing = new Sensing(this);
      Arrays.fill(this.armorDropChances, 0.085F);
      Arrays.fill(this.handDropChances, 0.085F);
      if (p_21369_ != null && !p_21369_.isClientSide) {
         this.registerGoals();
      }

   }

   protected void registerGoals() {
   }

   public static AttributeSupplier.Builder createMobAttributes() {
      return LivingEntity.createLivingAttributes().add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.ATTACK_KNOCKBACK);
   }

   protected PathNavigation createNavigation(Level pLevel) {
      return new GroundPathNavigation(this, pLevel);
   }

   protected boolean shouldPassengersInheritMalus() {
      return false;
   }

   public float getPathfindingMalus(BlockPathTypes pNodeType) {
      Mob mob;
      if (this.getVehicle() instanceof Mob && ((Mob)this.getVehicle()).shouldPassengersInheritMalus()) {
         mob = (Mob)this.getVehicle();
      } else {
         mob = this;
      }

      Float f = mob.pathfindingMalus.get(pNodeType);
      return f == null ? pNodeType.getMalus() : f;
   }

   public void setPathfindingMalus(BlockPathTypes pNodeType, float pPriority) {
      this.pathfindingMalus.put(pNodeType, pPriority);
   }

   public boolean canCutCorner(BlockPathTypes p_21482_) {
      return p_21482_ != BlockPathTypes.DANGER_FIRE && p_21482_ != BlockPathTypes.DANGER_CACTUS && p_21482_ != BlockPathTypes.DANGER_OTHER && p_21482_ != BlockPathTypes.WALKABLE_DOOR;
   }

   protected BodyRotationControl createBodyControl() {
      return new BodyRotationControl(this);
   }

   public LookControl getLookControl() {
      return this.lookControl;
   }

   public MoveControl getMoveControl() {
      if (this.isPassenger() && this.getVehicle() instanceof Mob) {
         Mob mob = (Mob)this.getVehicle();
         return mob.getMoveControl();
      } else {
         return this.moveControl;
      }
   }

   public JumpControl getJumpControl() {
      return this.jumpControl;
   }

   public PathNavigation getNavigation() {
      if (this.isPassenger() && this.getVehicle() instanceof Mob) {
         Mob mob = (Mob)this.getVehicle();
         return mob.getNavigation();
      } else {
         return this.navigation;
      }
   }

   /**
    * returns the EntitySenses Object for the EntityLiving
    */
   public Sensing getSensing() {
      return this.sensing;
   }

   /**
    * Gets the active target the Task system uses for tracking
    */
   @Nullable
   public LivingEntity getTarget() {
      return this.target;
   }

   /**
    * Sets the active target the Task system uses for tracking
    */
   public void setTarget(@Nullable LivingEntity pLivingEntity) {
      this.target = pLivingEntity;
      net.minecraftforge.common.ForgeHooks.onLivingSetAttackTarget(this, pLivingEntity);
   }

   public boolean canAttackType(EntityType<?> pType) {
      return pType != EntityType.GHAST;
   }

   public boolean canFireProjectileWeapon(ProjectileWeaponItem p_21430_) {
      return false;
   }

   /**
    * This function applies the benefits of growing back wool and faster growing up to the acting entity. (This function
    * is used in the AIEatGrass)
    */
   public void ate() {
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_MOB_FLAGS_ID, (byte)0);
   }

   /**
    * Get number of ticks, at least during which the living entity will be silent.
    */
   public int getAmbientSoundInterval() {
      return 80;
   }

   /**
    * Plays living's sound at its position
    */
   public void playAmbientSound() {
      SoundEvent soundevent = this.getAmbientSound();
      if (soundevent != null) {
         this.playSound(soundevent, this.getSoundVolume(), this.getVoicePitch());
      }

   }

   /**
    * Gets called every tick from main Entity class
    */
   public void baseTick() {
      super.baseTick();
      this.level.getProfiler().push("mobBaseTick");
      if (this.isAlive() && this.random.nextInt(1000) < this.ambientSoundTime++) {
         this.resetAmbientSoundTime();
         this.playAmbientSound();
      }

      this.level.getProfiler().pop();
   }

   protected void playHurtSound(DamageSource pSource) {
      this.resetAmbientSoundTime();
      super.playHurtSound(pSource);
   }

   private void resetAmbientSoundTime() {
      this.ambientSoundTime = -this.getAmbientSoundInterval();
   }

   /**
    * Get the experience points the entity currently has.
    */
   protected int getExperienceReward(Player pPlayer) {
      if (this.xpReward > 0) {
         int i = this.xpReward;

         for(int j = 0; j < this.armorItems.size(); ++j) {
            if (!this.armorItems.get(j).isEmpty() && this.armorDropChances[j] <= 1.0F) {
               i += 1 + this.random.nextInt(3);
            }
         }

         for(int k = 0; k < this.handItems.size(); ++k) {
            if (!this.handItems.get(k).isEmpty() && this.handDropChances[k] <= 1.0F) {
               i += 1 + this.random.nextInt(3);
            }
         }

         return i;
      } else {
         return this.xpReward;
      }
   }

   /**
    * Spawns an explosion particle around the Entity's location
    */
   public void spawnAnim() {
      if (this.level.isClientSide) {
         for(int i = 0; i < 20; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            double d3 = 10.0D;
            this.level.addParticle(ParticleTypes.POOF, this.getX(1.0D) - d0 * 10.0D, this.getRandomY() - d1 * 10.0D, this.getRandomZ(1.0D) - d2 * 10.0D, d0, d1, d2);
         }
      } else {
         this.level.broadcastEntityEvent(this, (byte)20);
      }

   }

   /**
    * Handler for {@link World#setEntityState}
    */
   public void handleEntityEvent(byte pId) {
      if (pId == 20) {
         this.spawnAnim();
      } else {
         super.handleEntityEvent(pId);
      }

   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      super.tick();
      if (!this.level.isClientSide) {
         this.tickLeash();
         if (this.tickCount % 5 == 0) {
            this.updateControlFlags();
         }
      }

   }

   /**
    * Sets MOVE, JUMP, LOOK Goal.Flags depending if entity is riding or been controlled
    */
   protected void updateControlFlags() {
      boolean flag = !(this.getControllingPassenger() instanceof Mob);
      boolean flag1 = !(this.getVehicle() instanceof Boat);
      this.goalSelector.setControlFlag(Goal.Flag.MOVE, flag);
      this.goalSelector.setControlFlag(Goal.Flag.JUMP, flag && flag1);
      this.goalSelector.setControlFlag(Goal.Flag.LOOK, flag);
   }

   protected float tickHeadTurn(float p_21538_, float p_21539_) {
      this.bodyRotationControl.clientTick();
      return p_21539_;
   }

   @Nullable
   protected SoundEvent getAmbientSound() {
      return null;
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putBoolean("CanPickUpLoot", this.canPickUpLoot());
      pCompound.putBoolean("PersistenceRequired", this.persistenceRequired);
      ListTag listtag = new ListTag();

      for(ItemStack itemstack : this.armorItems) {
         CompoundTag compoundtag = new CompoundTag();
         if (!itemstack.isEmpty()) {
            itemstack.save(compoundtag);
         }

         listtag.add(compoundtag);
      }

      pCompound.put("ArmorItems", listtag);
      ListTag listtag1 = new ListTag();

      for(ItemStack itemstack1 : this.handItems) {
         CompoundTag compoundtag1 = new CompoundTag();
         if (!itemstack1.isEmpty()) {
            itemstack1.save(compoundtag1);
         }

         listtag1.add(compoundtag1);
      }

      pCompound.put("HandItems", listtag1);
      ListTag listtag2 = new ListTag();

      for(float f : this.armorDropChances) {
         listtag2.add(FloatTag.valueOf(f));
      }

      pCompound.put("ArmorDropChances", listtag2);
      ListTag listtag3 = new ListTag();

      for(float f1 : this.handDropChances) {
         listtag3.add(FloatTag.valueOf(f1));
      }

      pCompound.put("HandDropChances", listtag3);
      if (this.leashHolder != null) {
         CompoundTag compoundtag2 = new CompoundTag();
         if (this.leashHolder instanceof LivingEntity) {
            UUID uuid = this.leashHolder.getUUID();
            compoundtag2.putUUID("UUID", uuid);
         } else if (this.leashHolder instanceof HangingEntity) {
            BlockPos blockpos = ((HangingEntity)this.leashHolder).getPos();
            compoundtag2.putInt("X", blockpos.getX());
            compoundtag2.putInt("Y", blockpos.getY());
            compoundtag2.putInt("Z", blockpos.getZ());
         }

         pCompound.put("Leash", compoundtag2);
      } else if (this.leashInfoTag != null) {
         pCompound.put("Leash", this.leashInfoTag.copy());
      }

      pCompound.putBoolean("LeftHanded", this.isLeftHanded());
      if (this.lootTable != null) {
         pCompound.putString("DeathLootTable", this.lootTable.toString());
         if (this.lootTableSeed != 0L) {
            pCompound.putLong("DeathLootTableSeed", this.lootTableSeed);
         }
      }

      if (this.isNoAi()) {
         pCompound.putBoolean("NoAI", this.isNoAi());
      }

   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      if (pCompound.contains("CanPickUpLoot", 1)) {
         this.setCanPickUpLoot(pCompound.getBoolean("CanPickUpLoot"));
      }

      this.persistenceRequired = pCompound.getBoolean("PersistenceRequired");
      if (pCompound.contains("ArmorItems", 9)) {
         ListTag listtag = pCompound.getList("ArmorItems", 10);

         for(int i = 0; i < this.armorItems.size(); ++i) {
            this.armorItems.set(i, ItemStack.of(listtag.getCompound(i)));
         }
      }

      if (pCompound.contains("HandItems", 9)) {
         ListTag listtag1 = pCompound.getList("HandItems", 10);

         for(int j = 0; j < this.handItems.size(); ++j) {
            this.handItems.set(j, ItemStack.of(listtag1.getCompound(j)));
         }
      }

      if (pCompound.contains("ArmorDropChances", 9)) {
         ListTag listtag2 = pCompound.getList("ArmorDropChances", 5);

         for(int k = 0; k < listtag2.size(); ++k) {
            this.armorDropChances[k] = listtag2.getFloat(k);
         }
      }

      if (pCompound.contains("HandDropChances", 9)) {
         ListTag listtag3 = pCompound.getList("HandDropChances", 5);

         for(int l = 0; l < listtag3.size(); ++l) {
            this.handDropChances[l] = listtag3.getFloat(l);
         }
      }

      if (pCompound.contains("Leash", 10)) {
         this.leashInfoTag = pCompound.getCompound("Leash");
      }

      this.setLeftHanded(pCompound.getBoolean("LeftHanded"));
      if (pCompound.contains("DeathLootTable", 8)) {
         this.lootTable = new ResourceLocation(pCompound.getString("DeathLootTable"));
         this.lootTableSeed = pCompound.getLong("DeathLootTableSeed");
      }

      this.setNoAi(pCompound.getBoolean("NoAI"));
   }

   protected void dropFromLootTable(DamageSource pDamageSource, boolean pAttackedRecently) {
      super.dropFromLootTable(pDamageSource, pAttackedRecently);
      this.lootTable = null;
   }

   protected LootContext.Builder createLootContext(boolean pAttackedRecently, DamageSource pDamageSource) {
      return super.createLootContext(pAttackedRecently, pDamageSource).withOptionalRandomSeed(this.lootTableSeed, this.random);
   }

   public final ResourceLocation getLootTable() {
      return this.lootTable == null ? this.getDefaultLootTable() : this.lootTable;
   }

   protected ResourceLocation getDefaultLootTable() {
      return super.getLootTable();
   }

   public void setZza(float pAmount) {
      this.zza = pAmount;
   }

   public void setYya(float pAmount) {
      this.yya = pAmount;
   }

   public void setXxa(float pAmount) {
      this.xxa = pAmount;
   }

   /**
    * set the movespeed used for the new AI system
    */
   public void setSpeed(float pSpeed) {
      super.setSpeed(pSpeed);
      this.setZza(pSpeed);
   }

   /**
    * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
    * use this to react to sunlight and start to burn.
    */
   public void aiStep() {
      super.aiStep();
      this.level.getProfiler().push("looting");
      if (!this.level.isClientSide && this.canPickUpLoot() && this.isAlive() && !this.dead && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this)) {
         for(ItemEntity itementity : this.level.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0D, 0.0D, 1.0D))) {
            if (!itementity.isRemoved() && !itementity.getItem().isEmpty() && !itementity.hasPickUpDelay() && this.wantsToPickUp(itementity.getItem())) {
               this.pickUpItem(itementity);
            }
         }
      }

      this.level.getProfiler().pop();
   }

   /**
    * Tests if this entity should pickup a weapon or an armor. Entity drops current weapon or armor if the new one is
    * better.
    */
   protected void pickUpItem(ItemEntity pItemEntity) {
      ItemStack itemstack = pItemEntity.getItem();
      if (this.equipItemIfPossible(itemstack)) {
         this.onItemPickup(pItemEntity);
         this.take(pItemEntity, itemstack.getCount());
         pItemEntity.discard();
      }

   }

   public boolean equipItemIfPossible(ItemStack p_21541_) {
      EquipmentSlot equipmentslot = getEquipmentSlotForItem(p_21541_);
      ItemStack itemstack = this.getItemBySlot(equipmentslot);
      boolean flag = this.canReplaceCurrentItem(p_21541_, itemstack);
      if (flag && this.canHoldItem(p_21541_)) {
         double d0 = (double)this.getEquipmentDropChance(equipmentslot);
         if (!itemstack.isEmpty() && (double)Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
            this.spawnAtLocation(itemstack);
         }

         this.setItemSlotAndDropWhenKilled(equipmentslot, p_21541_);
         this.equipEventAndSound(p_21541_);
         return true;
      } else {
         return false;
      }
   }

   protected void setItemSlotAndDropWhenKilled(EquipmentSlot p_21469_, ItemStack p_21470_) {
      this.setItemSlot(p_21469_, p_21470_);
      this.setGuaranteedDrop(p_21469_);
      this.persistenceRequired = true;
   }

   public void setGuaranteedDrop(EquipmentSlot p_21509_) {
      switch(p_21509_.getType()) {
      case HAND:
         this.handDropChances[p_21509_.getIndex()] = 2.0F;
         break;
      case ARMOR:
         this.armorDropChances[p_21509_.getIndex()] = 2.0F;
      }

   }

   protected boolean canReplaceCurrentItem(ItemStack pCandidate, ItemStack pExisting) {
      if (pExisting.isEmpty()) {
         return true;
      } else if (pCandidate.getItem() instanceof SwordItem) {
         if (!(pExisting.getItem() instanceof SwordItem)) {
            return true;
         } else {
            SwordItem sworditem = (SwordItem)pCandidate.getItem();
            SwordItem sworditem1 = (SwordItem)pExisting.getItem();
            if (sworditem.getDamage() != sworditem1.getDamage()) {
               return sworditem.getDamage() > sworditem1.getDamage();
            } else {
               return this.canReplaceEqualItem(pCandidate, pExisting);
            }
         }
      } else if (pCandidate.getItem() instanceof BowItem && pExisting.getItem() instanceof BowItem) {
         return this.canReplaceEqualItem(pCandidate, pExisting);
      } else if (pCandidate.getItem() instanceof CrossbowItem && pExisting.getItem() instanceof CrossbowItem) {
         return this.canReplaceEqualItem(pCandidate, pExisting);
      } else if (pCandidate.getItem() instanceof ArmorItem) {
         if (EnchantmentHelper.hasBindingCurse(pExisting)) {
            return false;
         } else if (!(pExisting.getItem() instanceof ArmorItem)) {
            return true;
         } else {
            ArmorItem armoritem = (ArmorItem)pCandidate.getItem();
            ArmorItem armoritem1 = (ArmorItem)pExisting.getItem();
            if (armoritem.getDefense() != armoritem1.getDefense()) {
               return armoritem.getDefense() > armoritem1.getDefense();
            } else if (armoritem.getToughness() != armoritem1.getToughness()) {
               return armoritem.getToughness() > armoritem1.getToughness();
            } else {
               return this.canReplaceEqualItem(pCandidate, pExisting);
            }
         }
      } else {
         if (pCandidate.getItem() instanceof DiggerItem) {
            if (pExisting.getItem() instanceof BlockItem) {
               return true;
            }

            if (pExisting.getItem() instanceof DiggerItem) {
               DiggerItem diggeritem = (DiggerItem)pCandidate.getItem();
               DiggerItem diggeritem1 = (DiggerItem)pExisting.getItem();
               if (diggeritem.getAttackDamage() != diggeritem1.getAttackDamage()) {
                  return diggeritem.getAttackDamage() > diggeritem1.getAttackDamage();
               }

               return this.canReplaceEqualItem(pCandidate, pExisting);
            }
         }

         return false;
      }
   }

   public boolean canReplaceEqualItem(ItemStack p_21478_, ItemStack p_21479_) {
      if (p_21478_.getDamageValue() >= p_21479_.getDamageValue() && (!p_21478_.hasTag() || p_21479_.hasTag())) {
         if (p_21478_.hasTag() && p_21479_.hasTag()) {
            return p_21478_.getTag().getAllKeys().stream().anyMatch((p_21513_) -> {
               return !p_21513_.equals("Damage");
            }) && !p_21479_.getTag().getAllKeys().stream().anyMatch((p_21503_) -> {
               return !p_21503_.equals("Damage");
            });
         } else {
            return false;
         }
      } else {
         return true;
      }
   }

   public boolean canHoldItem(ItemStack pStack) {
      return true;
   }

   public boolean wantsToPickUp(ItemStack p_21546_) {
      return this.canHoldItem(p_21546_);
   }

   public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
      return true;
   }

   public boolean requiresCustomPersistence() {
      return this.isPassenger();
   }

   protected boolean shouldDespawnInPeaceful() {
      return false;
   }

   /**
    * Makes the entity despawn if requirements are reached
    */
   public void checkDespawn() {
      if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
         this.discard();
      } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
         Entity entity = this.level.getNearestPlayer(this, -1.0D);
         net.minecraftforge.eventbus.api.Event.Result result = net.minecraftforge.event.ForgeEventFactory.canEntityDespawn(this);
         if (result == net.minecraftforge.eventbus.api.Event.Result.DENY) {
            noActionTime = 0;
            entity = null;
         } else if (result == net.minecraftforge.eventbus.api.Event.Result.ALLOW) {
            this.discard();
            entity = null;
         }
         if (entity != null) {
            double d0 = entity.distanceToSqr(this);
            int i = this.getType().getCategory().getDespawnDistance();
            int j = i * i;
            if (d0 > (double)j && this.removeWhenFarAway(d0)) {
               this.discard();
            }

            int k = this.getType().getCategory().getNoDespawnDistance();
            int l = k * k;
            if (this.noActionTime > 600 && this.random.nextInt(800) == 0 && d0 > (double)l && this.removeWhenFarAway(d0)) {
               this.discard();
            } else if (d0 < (double)l) {
               this.noActionTime = 0;
            }
         }

      } else {
         this.noActionTime = 0;
      }
   }

   protected final void serverAiStep() {
      ++this.noActionTime;
      this.level.getProfiler().push("sensing");
      this.sensing.tick();
      this.level.getProfiler().pop();
      int i = this.level.getServer().getTickCount() + this.getId();
      if (i % 2 != 0 && this.tickCount > 1) {
         this.level.getProfiler().push("targetSelector");
         this.targetSelector.tickRunningGoals(false);
         this.level.getProfiler().pop();
         this.level.getProfiler().push("goalSelector");
         this.goalSelector.tickRunningGoals(false);
         this.level.getProfiler().pop();
      } else {
         this.level.getProfiler().push("targetSelector");
         this.targetSelector.tick();
         this.level.getProfiler().pop();
         this.level.getProfiler().push("goalSelector");
         this.goalSelector.tick();
         this.level.getProfiler().pop();
      }

      this.level.getProfiler().push("navigation");
      this.navigation.tick();
      this.level.getProfiler().pop();
      this.level.getProfiler().push("mob tick");
      this.customServerAiStep();
      this.level.getProfiler().pop();
      this.level.getProfiler().push("controls");
      this.level.getProfiler().push("move");
      this.moveControl.tick();
      this.level.getProfiler().popPush("look");
      this.lookControl.tick();
      this.level.getProfiler().popPush("jump");
      this.jumpControl.tick();
      this.level.getProfiler().pop();
      this.level.getProfiler().pop();
      this.sendDebugPackets();
   }

   protected void sendDebugPackets() {
      DebugPackets.sendGoalSelector(this.level, this, this.goalSelector);
   }

   protected void customServerAiStep() {
   }

   /**
    * The speed it takes to move the entityliving's rotationPitch through the faceEntity method. This is only currently
    * use in wolves.
    */
   public int getMaxHeadXRot() {
      return 40;
   }

   public int getMaxHeadYRot() {
      return 75;
   }

   public int getHeadRotSpeed() {
      return 10;
   }

   /**
    * Changes pitch and yaw so that the entity calling the function is facing the entity provided as an argument.
    */
   public void lookAt(Entity pEntity, float pMaxYawIncrease, float pMaxPitchIncrease) {
      double d0 = pEntity.getX() - this.getX();
      double d2 = pEntity.getZ() - this.getZ();
      double d1;
      if (pEntity instanceof LivingEntity) {
         LivingEntity livingentity = (LivingEntity)pEntity;
         d1 = livingentity.getEyeY() - this.getEyeY();
      } else {
         d1 = (pEntity.getBoundingBox().minY + pEntity.getBoundingBox().maxY) / 2.0D - this.getEyeY();
      }

      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      float f = (float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
      float f1 = (float)(-(Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI)));
      this.setXRot(this.rotlerp(this.getXRot(), f1, pMaxPitchIncrease));
      this.setYRot(this.rotlerp(this.getYRot(), f, pMaxYawIncrease));
   }

   /**
    * Arguments: current rotation, intended rotation, max increment.
    */
   private float rotlerp(float pAngle, float pTargetAngle, float pMaxIncrease) {
      float f = Mth.wrapDegrees(pTargetAngle - pAngle);
      if (f > pMaxIncrease) {
         f = pMaxIncrease;
      }

      if (f < -pMaxIncrease) {
         f = -pMaxIncrease;
      }

      return pAngle + f;
   }

   /**
    * Returns true if entity is spawned from spawner or if entity can spawn on given BlockPos
    */
   public static boolean checkMobSpawnRules(EntityType<? extends Mob> pType, LevelAccessor pLevel, MobSpawnType pReason, BlockPos pPos, Random pRandom) {
      BlockPos blockpos = pPos.below();
      return pReason == MobSpawnType.SPAWNER || pLevel.getBlockState(blockpos).isValidSpawn(pLevel, blockpos, pType);
   }

   public boolean checkSpawnRules(LevelAccessor pLevel, MobSpawnType pSpawnReason) {
      return true;
   }

   public boolean checkSpawnObstruction(LevelReader pLevel) {
      return !pLevel.containsAnyLiquid(this.getBoundingBox()) && pLevel.isUnobstructed(this);
   }

   /**
    * Will return how many at most can spawn in a chunk at once.
    */
   public int getMaxSpawnClusterSize() {
      return 4;
   }

   public boolean isMaxGroupSizeReached(int pSize) {
      return false;
   }

   /**
    * The maximum height from where the entity is alowed to jump (used in pathfinder)
    */
   public int getMaxFallDistance() {
      if (this.getTarget() == null) {
         return 3;
      } else {
         int i = (int)(this.getHealth() - this.getMaxHealth() * 0.33F);
         i -= (3 - this.level.getDifficulty().getId()) * 4;
         if (i < 0) {
            i = 0;
         }

         return i + 3;
      }
   }

   public Iterable<ItemStack> getHandSlots() {
      return this.handItems;
   }

   public Iterable<ItemStack> getArmorSlots() {
      return this.armorItems;
   }

   public ItemStack getItemBySlot(EquipmentSlot pSlot) {
      switch(pSlot.getType()) {
      case HAND:
         return this.handItems.get(pSlot.getIndex());
      case ARMOR:
         return this.armorItems.get(pSlot.getIndex());
      default:
         return ItemStack.EMPTY;
      }
   }

   public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
      this.verifyEquippedItem(pStack);
      switch(pSlot.getType()) {
      case HAND:
         this.handItems.set(pSlot.getIndex(), pStack);
         break;
      case ARMOR:
         this.armorItems.set(pSlot.getIndex(), pStack);
      }

   }

   protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit) {
      super.dropCustomDeathLoot(pSource, pLooting, pRecentlyHit);

      for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
         ItemStack itemstack = this.getItemBySlot(equipmentslot);
         float f = this.getEquipmentDropChance(equipmentslot);
         boolean flag = f > 1.0F;
         if (!itemstack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(itemstack) && (pRecentlyHit || flag) && Math.max(this.random.nextFloat() - (float)pLooting * 0.01F, 0.0F) < f) {
            if (!flag && itemstack.isDamageableItem()) {
               itemstack.setDamageValue(itemstack.getMaxDamage() - this.random.nextInt(1 + this.random.nextInt(Math.max(itemstack.getMaxDamage() - 3, 1))));
            }

            this.spawnAtLocation(itemstack);
            this.setItemSlot(equipmentslot, ItemStack.EMPTY);
         }
      }

   }

   protected float getEquipmentDropChance(EquipmentSlot pSlot) {
      float f;
      switch(pSlot.getType()) {
      case HAND:
         f = this.handDropChances[pSlot.getIndex()];
         break;
      case ARMOR:
         f = this.armorDropChances[pSlot.getIndex()];
         break;
      default:
         f = 0.0F;
      }

      return f;
   }

   /**
    * Gives armor or weapon for entity based on given DifficultyInstance
    */
   protected void populateDefaultEquipmentSlots(DifficultyInstance pDifficulty) {
      if (this.random.nextFloat() < 0.15F * pDifficulty.getSpecialMultiplier()) {
         int i = this.random.nextInt(2);
         float f = this.level.getDifficulty() == Difficulty.HARD ? 0.1F : 0.25F;
         if (this.random.nextFloat() < 0.095F) {
            ++i;
         }

         if (this.random.nextFloat() < 0.095F) {
            ++i;
         }

         if (this.random.nextFloat() < 0.095F) {
            ++i;
         }

         boolean flag = true;

         for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
            if (equipmentslot.getType() == EquipmentSlot.Type.ARMOR) {
               ItemStack itemstack = this.getItemBySlot(equipmentslot);
               if (!flag && this.random.nextFloat() < f) {
                  break;
               }

               flag = false;
               if (itemstack.isEmpty()) {
                  Item item = getEquipmentForSlot(equipmentslot, i);
                  if (item != null) {
                     this.setItemSlot(equipmentslot, new ItemStack(item));
                  }
               }
            }
         }
      }

   }

   @Nullable
   public static Item getEquipmentForSlot(EquipmentSlot pSlot, int pChance) {
      switch(pSlot) {
      case HEAD:
         if (pChance == 0) {
            return Items.LEATHER_HELMET;
         } else if (pChance == 1) {
            return Items.GOLDEN_HELMET;
         } else if (pChance == 2) {
            return Items.CHAINMAIL_HELMET;
         } else if (pChance == 3) {
            return Items.IRON_HELMET;
         } else if (pChance == 4) {
            return Items.DIAMOND_HELMET;
         }
      case CHEST:
         if (pChance == 0) {
            return Items.LEATHER_CHESTPLATE;
         } else if (pChance == 1) {
            return Items.GOLDEN_CHESTPLATE;
         } else if (pChance == 2) {
            return Items.CHAINMAIL_CHESTPLATE;
         } else if (pChance == 3) {
            return Items.IRON_CHESTPLATE;
         } else if (pChance == 4) {
            return Items.DIAMOND_CHESTPLATE;
         }
      case LEGS:
         if (pChance == 0) {
            return Items.LEATHER_LEGGINGS;
         } else if (pChance == 1) {
            return Items.GOLDEN_LEGGINGS;
         } else if (pChance == 2) {
            return Items.CHAINMAIL_LEGGINGS;
         } else if (pChance == 3) {
            return Items.IRON_LEGGINGS;
         } else if (pChance == 4) {
            return Items.DIAMOND_LEGGINGS;
         }
      case FEET:
         if (pChance == 0) {
            return Items.LEATHER_BOOTS;
         } else if (pChance == 1) {
            return Items.GOLDEN_BOOTS;
         } else if (pChance == 2) {
            return Items.CHAINMAIL_BOOTS;
         } else if (pChance == 3) {
            return Items.IRON_BOOTS;
         } else if (pChance == 4) {
            return Items.DIAMOND_BOOTS;
         }
      default:
         return null;
      }
   }

   /**
    * Enchants Entity's current equipments based on given DifficultyInstance
    */
   protected void populateDefaultEquipmentEnchantments(DifficultyInstance pDifficulty) {
      float f = pDifficulty.getSpecialMultiplier();
      this.enchantSpawnedWeapon(f);

      for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
         if (equipmentslot.getType() == EquipmentSlot.Type.ARMOR) {
            this.enchantSpawnedArmor(f, equipmentslot);
         }
      }

   }

   protected void enchantSpawnedWeapon(float p_21572_) {
      if (!this.getMainHandItem().isEmpty() && this.random.nextFloat() < 0.25F * p_21572_) {
         this.setItemSlot(EquipmentSlot.MAINHAND, EnchantmentHelper.enchantItem(this.random, this.getMainHandItem(), (int)(5.0F + p_21572_ * (float)this.random.nextInt(18)), false));
      }

   }

   protected void enchantSpawnedArmor(float p_21381_, EquipmentSlot p_21382_) {
      ItemStack itemstack = this.getItemBySlot(p_21382_);
      if (!itemstack.isEmpty() && this.random.nextFloat() < 0.5F * p_21381_) {
         this.setItemSlot(p_21382_, EnchantmentHelper.enchantItem(this.random, itemstack, (int)(5.0F + p_21381_ * (float)this.random.nextInt(18)), false));
      }

   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
      this.getAttribute(Attributes.FOLLOW_RANGE).addPermanentModifier(new AttributeModifier("Random spawn bonus", this.random.nextGaussian() * 0.05D, AttributeModifier.Operation.MULTIPLY_BASE));
      if (this.random.nextFloat() < 0.05F) {
         this.setLeftHanded(true);
      } else {
         this.setLeftHanded(false);
      }

      return pSpawnData;
   }

   /**
    * returns true if all the conditions for steering the entity are met. For pigs, this is true if it is being ridden
    * by a player and the player is holding a carrot-on-a-stick
    */
   public boolean canBeControlledByRider() {
      return false;
   }

   /**
    * Enable the Entity persistence
    */
   public void setPersistenceRequired() {
      this.persistenceRequired = true;
   }

   public void setDropChance(EquipmentSlot pSlot, float pChance) {
      switch(pSlot.getType()) {
      case HAND:
         this.handDropChances[pSlot.getIndex()] = pChance;
         break;
      case ARMOR:
         this.armorDropChances[pSlot.getIndex()] = pChance;
      }

   }

   public boolean canPickUpLoot() {
      return this.canPickUpLoot;
   }

   public void setCanPickUpLoot(boolean pCanPickup) {
      this.canPickUpLoot = pCanPickup;
   }

   public boolean canTakeItem(ItemStack pItemstack) {
      EquipmentSlot equipmentslot = getEquipmentSlotForItem(pItemstack);
      return this.getItemBySlot(equipmentslot).isEmpty() && this.canPickUpLoot();
   }

   /**
    * Return the persistenceRequired field (whether this entity is allowed to naturally despawn)
    */
   public boolean isPersistenceRequired() {
      return this.persistenceRequired;
   }

   public final InteractionResult interact(Player pPlayer, InteractionHand pHand) {
      if (!this.isAlive()) {
         return InteractionResult.PASS;
      } else if (this.getLeashHolder() == pPlayer) {
         this.dropLeash(true, !pPlayer.getAbilities().instabuild);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      } else {
         InteractionResult interactionresult = this.checkAndHandleImportantInteractions(pPlayer, pHand);
         if (interactionresult.consumesAction()) {
            return interactionresult;
         } else {
            interactionresult = this.mobInteract(pPlayer, pHand);
            return interactionresult.consumesAction() ? interactionresult : super.interact(pPlayer, pHand);
         }
      }
   }

   private InteractionResult checkAndHandleImportantInteractions(Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (itemstack.is(Items.LEAD) && this.canBeLeashed(pPlayer)) {
         this.setLeashedTo(pPlayer, true);
         itemstack.shrink(1);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      } else {
         if (itemstack.is(Items.NAME_TAG)) {
            InteractionResult interactionresult = itemstack.interactLivingEntity(pPlayer, this, pHand);
            if (interactionresult.consumesAction()) {
               return interactionresult;
            }
         }

         if (itemstack.getItem() instanceof SpawnEggItem) {
            if (this.level instanceof ServerLevel) {
               SpawnEggItem spawneggitem = (SpawnEggItem)itemstack.getItem();
               Optional<Mob> optional = spawneggitem.spawnOffspringFromSpawnEgg(pPlayer, this, (EntityType<? extends Mob>)this.getType(), (ServerLevel)this.level, this.position(), itemstack);
               optional.ifPresent((p_21476_) -> {
                  this.onOffspringSpawnedFromEgg(pPlayer, p_21476_);
               });
               return optional.isPresent() ? InteractionResult.SUCCESS : InteractionResult.PASS;
            } else {
               return InteractionResult.CONSUME;
            }
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   protected void onOffspringSpawnedFromEgg(Player pPlayer, Mob pChild) {
   }

   protected InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      return InteractionResult.PASS;
   }

   public boolean isWithinRestriction() {
      return this.isWithinRestriction(this.blockPosition());
   }

   public boolean isWithinRestriction(BlockPos pPos) {
      if (this.restrictRadius == -1.0F) {
         return true;
      } else {
         return this.restrictCenter.distSqr(pPos) < (double)(this.restrictRadius * this.restrictRadius);
      }
   }

   public void restrictTo(BlockPos pPos, int pDistance) {
      this.restrictCenter = pPos;
      this.restrictRadius = (float)pDistance;
   }

   public BlockPos getRestrictCenter() {
      return this.restrictCenter;
   }

   public float getRestrictRadius() {
      return this.restrictRadius;
   }

   public void clearRestriction() {
      this.restrictRadius = -1.0F;
   }

   public boolean hasRestriction() {
      return this.restrictRadius != -1.0F;
   }

   @Nullable
   public <T extends Mob> T convertTo(EntityType<T> p_21407_, boolean p_21408_) {
      if (this.isRemoved()) {
         return (T)null;
      } else {
         T t = p_21407_.create(this.level);
         t.copyPosition(this);
         t.setBaby(this.isBaby());
         t.setNoAi(this.isNoAi());
         if (this.hasCustomName()) {
            t.setCustomName(this.getCustomName());
            t.setCustomNameVisible(this.isCustomNameVisible());
         }

         if (this.isPersistenceRequired()) {
            t.setPersistenceRequired();
         }

         t.setInvulnerable(this.isInvulnerable());
         if (p_21408_) {
            t.setCanPickUpLoot(this.canPickUpLoot());

            for(EquipmentSlot equipmentslot : EquipmentSlot.values()) {
               ItemStack itemstack = this.getItemBySlot(equipmentslot);
               if (!itemstack.isEmpty()) {
                  t.setItemSlot(equipmentslot, itemstack.copy());
                  t.setDropChance(equipmentslot, this.getEquipmentDropChance(equipmentslot));
                  itemstack.setCount(0);
               }
            }
         }

         this.level.addFreshEntity(t);
         if (this.isPassenger()) {
            Entity entity = this.getVehicle();
            this.stopRiding();
            t.startRiding(entity, true);
         }

         this.discard();
         return t;
      }
   }

   /**
    * Applies logic related to leashes, for example dragging the entity or breaking the leash.
    */
   protected void tickLeash() {
      if (this.leashInfoTag != null) {
         this.restoreLeashFromSave();
      }

      if (this.leashHolder != null) {
         if (!this.isAlive() || !this.leashHolder.isAlive()) {
            this.dropLeash(true, true);
         }

      }
   }

   /**
    * Removes the leash from this entity
    */
   public void dropLeash(boolean pSendPacket, boolean pDropLead) {
      if (this.leashHolder != null) {
         this.leashHolder = null;
         this.leashInfoTag = null;
         if (!this.level.isClientSide && pDropLead) {
            this.spawnAtLocation(Items.LEAD);
         }

         if (!this.level.isClientSide && pSendPacket && this.level instanceof ServerLevel) {
            ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, (Entity)null));
         }
      }

   }

   public boolean canBeLeashed(Player pPlayer) {
      return !this.isLeashed() && !(this instanceof Enemy);
   }

   public boolean isLeashed() {
      return this.leashHolder != null;
   }

   @Nullable
   public Entity getLeashHolder() {
      if (this.leashHolder == null && this.delayedLeashHolderId != 0 && this.level.isClientSide) {
         this.leashHolder = this.level.getEntity(this.delayedLeashHolderId);
      }

      return this.leashHolder;
   }

   /**
    * Sets the entity to be leashed to.
    */
   public void setLeashedTo(Entity pEntity, boolean pSendAttachNotification) {
      this.leashHolder = pEntity;
      this.leashInfoTag = null;
      if (!this.level.isClientSide && pSendAttachNotification && this.level instanceof ServerLevel) {
         ((ServerLevel)this.level).getChunkSource().broadcast(this, new ClientboundSetEntityLinkPacket(this, this.leashHolder));
      }

      if (this.isPassenger()) {
         this.stopRiding();
      }

   }

   public void setDelayedLeashHolderId(int pLeashHolderID) {
      this.delayedLeashHolderId = pLeashHolderID;
      this.dropLeash(false, false);
   }

   public boolean startRiding(Entity pEntity, boolean pForce) {
      boolean flag = super.startRiding(pEntity, pForce);
      if (flag && this.isLeashed()) {
         this.dropLeash(true, true);
      }

      return flag;
   }

   private void restoreLeashFromSave() {
      if (this.leashInfoTag != null && this.level instanceof ServerLevel) {
         if (this.leashInfoTag.hasUUID("UUID")) {
            UUID uuid = this.leashInfoTag.getUUID("UUID");
            Entity entity = ((ServerLevel)this.level).getEntity(uuid);
            if (entity != null) {
               this.setLeashedTo(entity, true);
               return;
            }
         } else if (this.leashInfoTag.contains("X", 99) && this.leashInfoTag.contains("Y", 99) && this.leashInfoTag.contains("Z", 99)) {
            BlockPos blockpos = NbtUtils.readBlockPos(this.leashInfoTag);
            this.setLeashedTo(LeashFenceKnotEntity.getOrCreateKnot(this.level, blockpos), true);
            return;
         }

         if (this.tickCount > 100) {
            this.spawnAtLocation(Items.LEAD);
            this.leashInfoTag = null;
         }
      }

   }

   public boolean isControlledByLocalInstance() {
      return this.canBeControlledByRider() && super.isControlledByLocalInstance();
   }

   /**
    * Returns whether the entity is in a server world
    */
   public boolean isEffectiveAi() {
      return super.isEffectiveAi() && !this.isNoAi();
   }

   /**
    * Set whether this Entity's AI is disabled
    */
   public void setNoAi(boolean pDisable) {
      byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, pDisable ? (byte)(b0 | 1) : (byte)(b0 & -2));
   }

   public void setLeftHanded(boolean pLeftHanded) {
      byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, pLeftHanded ? (byte)(b0 | 2) : (byte)(b0 & -3));
   }

   public void setAggressive(boolean pHasAggro) {
      byte b0 = this.entityData.get(DATA_MOB_FLAGS_ID);
      this.entityData.set(DATA_MOB_FLAGS_ID, pHasAggro ? (byte)(b0 | 4) : (byte)(b0 & -5));
   }

   /**
    * Get whether this Entity's AI is disabled
    */
   public boolean isNoAi() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 1) != 0;
   }

   public boolean isLeftHanded() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 2) != 0;
   }

   public boolean isAggressive() {
      return (this.entityData.get(DATA_MOB_FLAGS_ID) & 4) != 0;
   }

   /**
    * Set whether this zombie is a child.
    */
   public void setBaby(boolean pChildZombie) {
   }

   public HumanoidArm getMainArm() {
      return this.isLeftHanded() ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
   }

   public double getMeleeAttackRangeSqr(LivingEntity p_147273_) {
      return (double)(this.getBbWidth() * 2.0F * this.getBbWidth() * 2.0F + p_147273_.getBbWidth());
   }

   public boolean doHurtTarget(Entity pEntity) {
      float f = (float)this.getAttributeValue(Attributes.ATTACK_DAMAGE);
      float f1 = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
      if (pEntity instanceof LivingEntity) {
         f += EnchantmentHelper.getDamageBonus(this.getMainHandItem(), ((LivingEntity)pEntity).getMobType());
         f1 += (float)EnchantmentHelper.getKnockbackBonus(this);
      }

      int i = EnchantmentHelper.getFireAspect(this);
      if (i > 0) {
         pEntity.setSecondsOnFire(i * 4);
      }

      boolean flag = pEntity.hurt(DamageSource.mobAttack(this), f);
      if (flag) {
         if (f1 > 0.0F && pEntity instanceof LivingEntity) {
            ((LivingEntity)pEntity).knockback((double)(f1 * 0.5F), (double)Mth.sin(this.getYRot() * ((float)Math.PI / 180F)), (double)(-Mth.cos(this.getYRot() * ((float)Math.PI / 180F))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.6D, 1.0D, 0.6D));
         }

         if (pEntity instanceof Player) {
            Player player = (Player)pEntity;
            this.maybeDisableShield(player, this.getMainHandItem(), player.isUsingItem() ? player.getUseItem() : ItemStack.EMPTY);
         }

         this.doEnchantDamageEffects(this, pEntity);
         this.setLastHurtMob(pEntity);
      }

      return flag;
   }

   private void maybeDisableShield(Player p_21425_, ItemStack p_21426_, ItemStack p_21427_) {
      if (!p_21426_.isEmpty() && !p_21427_.isEmpty() && p_21426_.getItem() instanceof AxeItem && p_21427_.is(Items.SHIELD)) {
         float f = 0.25F + (float)EnchantmentHelper.getBlockEfficiency(this) * 0.05F;
         if (this.random.nextFloat() < f) {
            p_21425_.getCooldowns().addCooldown(Items.SHIELD, 100);
            this.level.broadcastEntityEvent(p_21425_, (byte)30);
         }
      }

   }

   protected boolean isSunBurnTick() {
      if (this.level.isDay() && !this.level.isClientSide) {
         float f = this.getBrightness();
         BlockPos blockpos = new BlockPos(this.getX(), this.getEyeY(), this.getZ());
         boolean flag = this.isInWaterRainOrBubble() || this.isInPowderSnow || this.wasInPowderSnow;
         if (f > 0.5F && this.random.nextFloat() * 30.0F < (f - 0.4F) * 2.0F && !flag && this.level.canSeeSky(blockpos)) {
            return true;
         }
      }

      return false;
   }

   protected void jumpInLiquid(Tag<Fluid> pFluidTag) {
      if (this.getNavigation().canFloat()) {
         super.jumpInLiquid(pFluidTag);
      } else {
         this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.3D, 0.0D));
      }

   }

   public void removeFreeWill() {
      this.goalSelector.removeAllGoals();
      this.getBrain().removeAllBehaviors();
   }

   protected void removeAfterChangingDimensions() {
      super.removeAfterChangingDimensions();
      this.dropLeash(true, false);
      this.getAllSlots().forEach((p_181125_) -> {
         p_181125_.setCount(0);
      });
   }

   @Nullable
   public ItemStack getPickResult() {
      SpawnEggItem spawneggitem = SpawnEggItem.byId(this.getType());
      return spawneggitem == null ? null : new ItemStack(spawneggitem);
   }
}
