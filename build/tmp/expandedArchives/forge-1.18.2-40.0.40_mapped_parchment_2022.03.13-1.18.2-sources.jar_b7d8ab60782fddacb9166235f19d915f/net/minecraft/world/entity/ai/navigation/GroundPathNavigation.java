package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
   private boolean avoidSun;

   public GroundPathNavigation(Mob p_26448_, Level p_26449_) {
      super(p_26448_, p_26449_);
   }

   protected PathFinder createPathFinder(int p_26453_) {
      this.nodeEvaluator = new WalkNodeEvaluator();
      this.nodeEvaluator.setCanPassDoors(true);
      return new PathFinder(this.nodeEvaluator, p_26453_);
   }

   /**
    * If on ground or swimming and can swim
    */
   protected boolean canUpdatePath() {
      return this.mob.isOnGround() || this.isInLiquid() || this.mob.isPassenger();
   }

   protected Vec3 getTempMobPos() {
      return new Vec3(this.mob.getX(), (double)this.getSurfaceY(), this.mob.getZ());
   }

   /**
    * Returns path to given BlockPos
    */
   public Path createPath(BlockPos pPos, int pAccuracy) {
      if (this.level.getBlockState(pPos).isAir()) {
         BlockPos blockpos;
         for(blockpos = pPos.below(); blockpos.getY() > this.level.getMinBuildHeight() && this.level.getBlockState(blockpos).isAir(); blockpos = blockpos.below()) {
         }

         if (blockpos.getY() > this.level.getMinBuildHeight()) {
            return super.createPath(blockpos.above(), pAccuracy);
         }

         while(blockpos.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(blockpos).isAir()) {
            blockpos = blockpos.above();
         }

         pPos = blockpos;
      }

      if (!this.level.getBlockState(pPos).getMaterial().isSolid()) {
         return super.createPath(pPos, pAccuracy);
      } else {
         BlockPos blockpos1;
         for(blockpos1 = pPos.above(); blockpos1.getY() < this.level.getMaxBuildHeight() && this.level.getBlockState(blockpos1).getMaterial().isSolid(); blockpos1 = blockpos1.above()) {
         }

         return super.createPath(blockpos1, pAccuracy);
      }
   }

   /**
    * Returns a path to the given entity or null
    */
   public Path createPath(Entity p_26465_, int p_26466_) {
      return this.createPath(p_26465_.blockPosition(), p_26466_);
   }

   /**
    * Gets the safe pathing Y position for the entity depending on if it can path swim or not
    */
   private int getSurfaceY() {
      if (this.mob.isInWater() && this.canFloat()) {
         int i = this.mob.getBlockY();
         BlockState blockstate = this.level.getBlockState(new BlockPos(this.mob.getX(), (double)i, this.mob.getZ()));
         int j = 0;

         while(blockstate.is(Blocks.WATER)) {
            ++i;
            blockstate = this.level.getBlockState(new BlockPos(this.mob.getX(), (double)i, this.mob.getZ()));
            ++j;
            if (j > 16) {
               return this.mob.getBlockY();
            }
         }

         return i;
      } else {
         return Mth.floor(this.mob.getY() + 0.5D);
      }
   }

   /**
    * Trims path data from the end to the first sun covered block
    */
   protected void trimPath() {
      super.trimPath();
      if (this.avoidSun) {
         if (this.level.canSeeSky(new BlockPos(this.mob.getX(), this.mob.getY() + 0.5D, this.mob.getZ()))) {
            return;
         }

         for(int i = 0; i < this.path.getNodeCount(); ++i) {
            Node node = this.path.getNode(i);
            if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) {
               this.path.truncateNodes(i);
               return;
            }
         }
      }

   }

   protected boolean hasValidPathType(BlockPathTypes p_26467_) {
      if (p_26467_ == BlockPathTypes.WATER) {
         return false;
      } else if (p_26467_ == BlockPathTypes.LAVA) {
         return false;
      } else {
         return p_26467_ != BlockPathTypes.OPEN;
      }
   }

   public void setCanOpenDoors(boolean pCanBreakDoors) {
      this.nodeEvaluator.setCanOpenDoors(pCanBreakDoors);
   }

   public boolean canPassDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setCanPassDoors(boolean p_148215_) {
      this.nodeEvaluator.setCanPassDoors(p_148215_);
   }

   public boolean canOpenDoors() {
      return this.nodeEvaluator.canPassDoors();
   }

   public void setAvoidSun(boolean pAvoidSun) {
      this.avoidSun = pAvoidSun;
   }
}