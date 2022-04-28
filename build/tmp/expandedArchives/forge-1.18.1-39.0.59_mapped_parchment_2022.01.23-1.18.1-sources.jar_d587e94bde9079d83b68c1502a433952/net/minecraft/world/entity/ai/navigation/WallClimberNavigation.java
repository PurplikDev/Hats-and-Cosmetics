package net.minecraft.world.entity.ai.navigation;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;

public class WallClimberNavigation extends GroundPathNavigation {
   /** Current path navigation target */
   @Nullable
   private BlockPos pathToPosition;

   public WallClimberNavigation(Mob p_26580_, Level p_26581_) {
      super(p_26580_, p_26581_);
   }

   /**
    * Returns path to given BlockPos
    */
   public Path createPath(BlockPos pPos, int pAccuracy) {
      this.pathToPosition = pPos;
      return super.createPath(pPos, pAccuracy);
   }

   /**
    * Returns a path to the given entity or null
    */
   public Path createPath(Entity p_26586_, int p_26587_) {
      this.pathToPosition = p_26586_.blockPosition();
      return super.createPath(p_26586_, p_26587_);
   }

   /**
    * Try to find and set a path to EntityLiving. Returns true if successful. Args : entity, speed
    */
   public boolean moveTo(Entity pEntity, double pSpeed) {
      Path path = this.createPath(pEntity, 0);
      if (path != null) {
         return this.moveTo(path, pSpeed);
      } else {
         this.pathToPosition = pEntity.blockPosition();
         this.speedModifier = pSpeed;
         return true;
      }
   }

   public void tick() {
      if (!this.isDone()) {
         super.tick();
      } else {
         if (this.pathToPosition != null) {
            // FORGE: Fix MC-94054
            if (!this.pathToPosition.closerThan(this.mob.position(), Math.max((double)this.mob.getBbWidth(), 1.0D)) && (!(this.mob.getY() > (double)this.pathToPosition.getY()) || !(new BlockPos((double)this.pathToPosition.getX(), this.mob.getY(), (double)this.pathToPosition.getZ())).closerThan(this.mob.position(), Math.max((double)this.mob.getBbWidth(), 1.0D)))) {
               this.mob.getMoveControl().setWantedPosition((double)this.pathToPosition.getX(), (double)this.pathToPosition.getY(), (double)this.pathToPosition.getZ(), this.speedModifier);
            } else {
               this.pathToPosition = null;
            }
         }

      }
   }
}
