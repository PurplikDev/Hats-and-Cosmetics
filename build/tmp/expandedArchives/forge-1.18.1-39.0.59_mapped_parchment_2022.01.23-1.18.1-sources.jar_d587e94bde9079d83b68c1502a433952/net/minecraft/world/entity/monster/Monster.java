package net.minecraft.world.entity.monster;

import java.util.Random;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;

public abstract class Monster extends PathfinderMob implements Enemy {
   protected Monster(EntityType<? extends Monster> p_33002_, Level p_33003_) {
      super(p_33002_, p_33003_);
      this.xpReward = 5;
   }

   public SoundSource getSoundSource() {
      return SoundSource.HOSTILE;
   }

   /**
    * Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons
    * use this to react to sunlight and start to burn.
    */
   public void aiStep() {
      this.updateSwingTime();
      this.updateNoActionTime();
      super.aiStep();
   }

   protected void updateNoActionTime() {
      float f = this.getBrightness();
      if (f > 0.5F) {
         this.noActionTime += 2;
      }

   }

   protected boolean shouldDespawnInPeaceful() {
      return true;
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.HOSTILE_SWIM;
   }

   protected SoundEvent getSwimSplashSound() {
      return SoundEvents.HOSTILE_SPLASH;
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return SoundEvents.HOSTILE_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.HOSTILE_DEATH;
   }

   public LivingEntity.Fallsounds getFallSounds() {
      return new LivingEntity.Fallsounds(SoundEvents.HOSTILE_SMALL_FALL, SoundEvents.HOSTILE_BIG_FALL);
   }

   public float getWalkTargetValue(BlockPos pPos, LevelReader pLevel) {
      return 0.5F - pLevel.getBrightness(pPos);
   }

   /**
    * Static predicate for determining if the current light level and environmental conditions allow for a monster to
    * spawn.
    */
   public static boolean isDarkEnoughToSpawn(ServerLevelAccessor pLevel, BlockPos pPos, Random pRandom) {
      if (pLevel.getBrightness(LightLayer.SKY, pPos) > pRandom.nextInt(32)) {
         return false;
      } else if (pLevel.getBrightness(LightLayer.BLOCK, pPos) > 0) {
         return false;
      } else {
         int i = pLevel.getLevel().isThundering() ? pLevel.getMaxLocalRawBrightness(pPos, 10) : pLevel.getMaxLocalRawBrightness(pPos);
         return i <= pRandom.nextInt(8);
      }
   }

   /**
    * Static predicate for determining whether or not a monster can spawn at the provided location, incorporating a
    * check of the current light level at the location.
    */
   public static boolean checkMonsterSpawnRules(EntityType<? extends Monster> pType, ServerLevelAccessor pLevel, MobSpawnType pReason, BlockPos pPos, Random pRandom) {
      return pLevel.getDifficulty() != Difficulty.PEACEFUL && isDarkEnoughToSpawn(pLevel, pPos, pRandom) && checkMobSpawnRules(pType, pLevel, pReason, pPos, pRandom);
   }

   /**
    * Static predicate for determining whether or not a monster can spawn at the provided location.
    */
   public static boolean checkAnyLightMonsterSpawnRules(EntityType<? extends Monster> pType, LevelAccessor pLevel, MobSpawnType pReason, BlockPos pPos, Random pRandom) {
      return pLevel.getDifficulty() != Difficulty.PEACEFUL && checkMobSpawnRules(pType, pLevel, pReason, pPos, pRandom);
   }

   public static AttributeSupplier.Builder createMonsterAttributes() {
      return Mob.createMobAttributes().add(Attributes.ATTACK_DAMAGE);
   }

   /**
    * Entity won't drop items or experience points if this returns false
    */
   protected boolean shouldDropExperience() {
      return true;
   }

   protected boolean shouldDropLoot() {
      return true;
   }

   public boolean isPreventingPlayerRest(Player p_33036_) {
      return true;
   }

   public ItemStack getProjectile(ItemStack pShootable) {
      if (pShootable.getItem() instanceof ProjectileWeaponItem) {
         Predicate<ItemStack> predicate = ((ProjectileWeaponItem)pShootable.getItem()).getSupportedHeldProjectiles();
         ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
         return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
      } else {
         return ItemStack.EMPTY;
      }
   }
}