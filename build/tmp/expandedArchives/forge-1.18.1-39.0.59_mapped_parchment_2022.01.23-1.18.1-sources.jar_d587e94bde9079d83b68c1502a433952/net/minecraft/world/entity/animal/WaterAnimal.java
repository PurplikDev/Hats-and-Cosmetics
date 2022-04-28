package net.minecraft.world.entity.animal;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

public abstract class WaterAnimal extends PathfinderMob {
   protected WaterAnimal(EntityType<? extends WaterAnimal> p_30341_, Level p_30342_) {
      super(p_30341_, p_30342_);
      this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
   }

   public boolean canBreatheUnderwater() {
      return true;
   }

   public MobType getMobType() {
      return MobType.WATER;
   }

   public boolean checkSpawnObstruction(LevelReader pLevel) {
      return pLevel.isUnobstructed(this);
   }

   /**
    * Get number of ticks, at least during which the living entity will be silent.
    */
   public int getAmbientSoundInterval() {
      return 120;
   }

   /**
    * Get the experience points the entity currently has.
    */
   protected int getExperienceReward(Player pPlayer) {
      return 1 + this.level.random.nextInt(3);
   }

   protected void handleAirSupply(int p_30344_) {
      if (this.isAlive() && !this.isInWaterOrBubble()) {
         this.setAirSupply(p_30344_ - 1);
         if (this.getAirSupply() == -20) {
            this.setAirSupply(0);
            this.hurt(DamageSource.DROWN, 2.0F);
         }
      } else {
         this.setAirSupply(300);
      }

   }

   /**
    * Gets called every tick from main Entity class
    */
   public void baseTick() {
      int i = this.getAirSupply();
      super.baseTick();
      this.handleAirSupply(i);
   }

   public boolean isPushedByFluid() {
      return false;
   }

   public boolean canBeLeashed(Player pPlayer) {
      return false;
   }

   public static boolean checkSurfaceWaterAnimalSpawnRules(EntityType<? extends WaterAnimal> p_186238_, LevelAccessor p_186239_, MobSpawnType p_186240_, BlockPos p_186241_, Random p_186242_) {
      int i = p_186239_.getSeaLevel();
      int j = i - 13;
      return p_186239_.getFluidState(p_186241_.below()).is(FluidTags.WATER) && p_186239_.getBlockState(p_186241_.above()).is(Blocks.WATER) && p_186241_.getY() >= j && p_186241_.getY() <= i;
   }
}