package net.minecraft.world.entity.animal.horse;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;

public class Horse extends AbstractHorse {
   private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("556E1665-8B10-40C8-8F9D-CF9B1667F295");
   private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(Horse.class, EntityDataSerializers.INT);

   public Horse(EntityType<? extends Horse> p_30689_, Level p_30690_) {
      super(p_30689_, p_30690_);
   }

   protected void randomizeAttributes() {
      this.getAttribute(Attributes.MAX_HEALTH).setBaseValue((double)this.generateRandomMaxHealth());
      this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(this.generateRandomSpeed());
      this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(this.generateRandomJumpStrength());
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.entityData.define(DATA_ID_TYPE_VARIANT, 0);
   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      pCompound.putInt("Variant", this.getTypeVariant());
      if (!this.inventory.getItem(1).isEmpty()) {
         pCompound.put("ArmorItem", this.inventory.getItem(1).save(new CompoundTag()));
      }

   }

   public ItemStack getArmor() {
      return this.getItemBySlot(EquipmentSlot.CHEST);
   }

   private void setArmor(ItemStack p_30733_) {
      this.setItemSlot(EquipmentSlot.CHEST, p_30733_);
      this.setDropChance(EquipmentSlot.CHEST, 0.0F);
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.setTypeVariant(pCompound.getInt("Variant"));
      if (pCompound.contains("ArmorItem", 10)) {
         ItemStack itemstack = ItemStack.of(pCompound.getCompound("ArmorItem"));
         if (!itemstack.isEmpty() && this.isArmor(itemstack)) {
            this.inventory.setItem(1, itemstack);
         }
      }

      this.updateContainerEquipment();
   }

   private void setTypeVariant(int p_30737_) {
      this.entityData.set(DATA_ID_TYPE_VARIANT, p_30737_);
   }

   private int getTypeVariant() {
      return this.entityData.get(DATA_ID_TYPE_VARIANT);
   }

   private void setVariantAndMarkings(Variant p_30700_, Markings p_30701_) {
      this.setTypeVariant(p_30700_.getId() & 255 | p_30701_.getId() << 8 & '\uff00');
   }

   public Variant getVariant() {
      return Variant.byId(this.getTypeVariant() & 255);
   }

   public Markings getMarkings() {
      return Markings.byId((this.getTypeVariant() & '\uff00') >> 8);
   }

   protected void updateContainerEquipment() {
      if (!this.level.isClientSide) {
         super.updateContainerEquipment();
         this.setArmorEquipment(this.inventory.getItem(1));
         this.setDropChance(EquipmentSlot.CHEST, 0.0F);
      }
   }

   private void setArmorEquipment(ItemStack p_30735_) {
      this.setArmor(p_30735_);
      if (!this.level.isClientSide) {
         this.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
         if (this.isArmor(p_30735_)) {
            int i = ((HorseArmorItem)p_30735_.getItem()).getProtection();
            if (i != 0) {
               this.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(ARMOR_MODIFIER_UUID, "Horse armor bonus", (double)i, AttributeModifier.Operation.ADDITION));
            }
         }
      }

   }

   /**
    * Called by InventoryBasic.onInventoryChanged() on a array that is never filled.
    */
   public void containerChanged(Container pInvBasic) {
      ItemStack itemstack = this.getArmor();
      super.containerChanged(pInvBasic);
      ItemStack itemstack1 = this.getArmor();
      if (this.tickCount > 20 && this.isArmor(itemstack1) && itemstack != itemstack1) {
         this.playSound(SoundEvents.HORSE_ARMOR, 0.5F, 1.0F);
      }

   }

   protected void playGallopSound(SoundType p_30709_) {
      super.playGallopSound(p_30709_);
      if (this.random.nextInt(10) == 0) {
         this.playSound(SoundEvents.HORSE_BREATHE, p_30709_.getVolume() * 0.6F, p_30709_.getPitch());
      }

      ItemStack stack = this.inventory.getItem(1);
      if (isArmor(stack)) stack.onHorseArmorTick(level, this);
   }

   protected SoundEvent getAmbientSound() {
      super.getAmbientSound();
      return SoundEvents.HORSE_AMBIENT;
   }

   protected SoundEvent getDeathSound() {
      super.getDeathSound();
      return SoundEvents.HORSE_DEATH;
   }

   @Nullable
   protected SoundEvent getEatingSound() {
      return SoundEvents.HORSE_EAT;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      super.getHurtSound(pDamageSource);
      return SoundEvents.HORSE_HURT;
   }

   protected SoundEvent getAngrySound() {
      super.getAngrySound();
      return SoundEvents.HORSE_ANGRY;
   }

   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (!this.isBaby()) {
         if (this.isTamed() && pPlayer.isSecondaryUseActive()) {
            this.openInventory(pPlayer);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
         }

         if (this.isVehicle()) {
            return super.mobInteract(pPlayer, pHand);
         }
      }

      if (!itemstack.isEmpty()) {
         if (this.isFood(itemstack)) {
            return this.fedFood(pPlayer, itemstack);
         }

         InteractionResult interactionresult = itemstack.interactLivingEntity(pPlayer, this, pHand);
         if (interactionresult.consumesAction()) {
            return interactionresult;
         }

         if (!this.isTamed()) {
            this.makeMad();
            return InteractionResult.sidedSuccess(this.level.isClientSide);
         }

         boolean flag = !this.isBaby() && !this.isSaddled() && itemstack.is(Items.SADDLE);
         if (this.isArmor(itemstack) || flag) {
            this.openInventory(pPlayer);
            return InteractionResult.sidedSuccess(this.level.isClientSide);
         }
      }

      if (this.isBaby()) {
         return super.mobInteract(pPlayer, pHand);
      } else {
         this.doPlayerRide(pPlayer);
         return InteractionResult.sidedSuccess(this.level.isClientSide);
      }
   }

   /**
    * Returns true if the mob is currently able to mate with the specified mob.
    */
   public boolean canMate(Animal pOtherAnimal) {
      if (pOtherAnimal == this) {
         return false;
      } else if (!(pOtherAnimal instanceof Donkey) && !(pOtherAnimal instanceof Horse)) {
         return false;
      } else {
         return this.canParent() && ((AbstractHorse)pOtherAnimal).canParent();
      }
   }

   public AgeableMob getBreedOffspring(ServerLevel p_149533_, AgeableMob p_149534_) {
      AbstractHorse abstracthorse;
      if (p_149534_ instanceof Donkey) {
         abstracthorse = EntityType.MULE.create(p_149533_);
      } else {
         Horse horse = (Horse)p_149534_;
         abstracthorse = EntityType.HORSE.create(p_149533_);
         int i = this.random.nextInt(9);
         Variant variant;
         if (i < 4) {
            variant = this.getVariant();
         } else if (i < 8) {
            variant = horse.getVariant();
         } else {
            variant = Util.getRandom(Variant.values(), this.random);
         }

         int j = this.random.nextInt(5);
         Markings markings;
         if (j < 2) {
            markings = this.getMarkings();
         } else if (j < 4) {
            markings = horse.getMarkings();
         } else {
            markings = Util.getRandom(Markings.values(), this.random);
         }

         ((Horse)abstracthorse).setVariantAndMarkings(variant, markings);
      }

      this.setOffspringAttributes(p_149534_, abstracthorse);
      return abstracthorse;
   }

   public boolean canWearArmor() {
      return true;
   }

   public boolean isArmor(ItemStack pStack) {
      return pStack.getItem() instanceof HorseArmorItem;
   }

   @Nullable
   public SpawnGroupData finalizeSpawn(ServerLevelAccessor pLevel, DifficultyInstance pDifficulty, MobSpawnType pReason, @Nullable SpawnGroupData pSpawnData, @Nullable CompoundTag pDataTag) {
      Variant variant;
      if (pSpawnData instanceof Horse.HorseGroupData) {
         variant = ((Horse.HorseGroupData)pSpawnData).variant;
      } else {
         variant = Util.getRandom(Variant.values(), this.random);
         pSpawnData = new Horse.HorseGroupData(variant);
      }

      this.setVariantAndMarkings(variant, Util.getRandom(Markings.values(), this.random));
      return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
   }

   public static class HorseGroupData extends AgeableMob.AgeableMobGroupData {
      public final Variant variant;

      public HorseGroupData(Variant p_30740_) {
         super(true);
         this.variant = p_30740_;
      }
   }
}
