package net.minecraft.world.entity;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EntityDimensions {
   public final float width;
   public final float height;
   public final boolean fixed;

   public EntityDimensions(float p_20381_, float p_20382_, boolean p_20383_) {
      this.width = p_20381_;
      this.height = p_20382_;
      this.fixed = p_20383_;
   }

   public AABB makeBoundingBox(Vec3 p_20394_) {
      return this.makeBoundingBox(p_20394_.x, p_20394_.y, p_20394_.z);
   }

   public AABB makeBoundingBox(double p_20385_, double p_20386_, double p_20387_) {
      float f = this.width / 2.0F;
      float f1 = this.height;
      return new AABB(p_20385_ - (double)f, p_20386_, p_20387_ - (double)f, p_20385_ + (double)f, p_20386_ + (double)f1, p_20387_ + (double)f);
   }

   public EntityDimensions scale(float pFactor) {
      return this.scale(pFactor, pFactor);
   }

   public EntityDimensions scale(float pWidthFactor, float pHeightFactor) {
      return !this.fixed && (pWidthFactor != 1.0F || pHeightFactor != 1.0F) ? scalable(this.width * pWidthFactor, this.height * pHeightFactor) : this;
   }

   public static EntityDimensions scalable(float pWidth, float pHeight) {
      return new EntityDimensions(pWidth, pHeight, false);
   }

   public static EntityDimensions fixed(float pWidth, float pHeight) {
      return new EntityDimensions(pWidth, pHeight, true);
   }

   public String toString() {
      return "EntityDimensions w=" + this.width + ", h=" + this.height + ", fixed=" + this.fixed;
   }
}