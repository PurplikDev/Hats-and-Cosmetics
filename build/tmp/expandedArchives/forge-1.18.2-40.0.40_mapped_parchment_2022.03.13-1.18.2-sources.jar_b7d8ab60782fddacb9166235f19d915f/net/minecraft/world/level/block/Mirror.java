package net.minecraft.world.level.block;

import com.mojang.math.OctahedralGroup;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public enum Mirror {
   NONE(new TranslatableComponent("mirror.none"), OctahedralGroup.IDENTITY),
   LEFT_RIGHT(new TranslatableComponent("mirror.left_right"), OctahedralGroup.INVERT_Z),
   FRONT_BACK(new TranslatableComponent("mirror.front_back"), OctahedralGroup.INVERT_X);

   private final Component symbol;
   private final OctahedralGroup rotation;

   private Mirror(Component p_153785_, OctahedralGroup p_153786_) {
      this.symbol = p_153785_;
      this.rotation = p_153786_;
   }

   /**
    * Mirrors the given rotation like specified by this mirror. Rotations start at 0 and go up to rotationCount-1. 0 is
    * front, rotationCount/2 is back.
    */
   public int mirror(int pRotation, int pRotationCount) {
      int i = pRotationCount / 2;
      int j = pRotation > i ? pRotation - pRotationCount : pRotation;
      switch(this) {
      case FRONT_BACK:
         return (pRotationCount - j) % pRotationCount;
      case LEFT_RIGHT:
         return (i - j + pRotationCount) % pRotationCount;
      default:
         return pRotation;
      }
   }

   /**
    * Determines the rotation that is equivalent to this mirror if the rotating object faces in the given direction
    */
   public Rotation getRotation(Direction pFacing) {
      Direction.Axis direction$axis = pFacing.getAxis();
      return (this != LEFT_RIGHT || direction$axis != Direction.Axis.Z) && (this != FRONT_BACK || direction$axis != Direction.Axis.X) ? Rotation.NONE : Rotation.CLOCKWISE_180;
   }

   /**
    * Mirror the given facing according to this mirror
    */
   public Direction mirror(Direction pFacing) {
      if (this == FRONT_BACK && pFacing.getAxis() == Direction.Axis.X) {
         return pFacing.getOpposite();
      } else {
         return this == LEFT_RIGHT && pFacing.getAxis() == Direction.Axis.Z ? pFacing.getOpposite() : pFacing;
      }
   }

   public OctahedralGroup rotation() {
      return this.rotation;
   }

   public Component symbol() {
      return this.symbol;
   }
}