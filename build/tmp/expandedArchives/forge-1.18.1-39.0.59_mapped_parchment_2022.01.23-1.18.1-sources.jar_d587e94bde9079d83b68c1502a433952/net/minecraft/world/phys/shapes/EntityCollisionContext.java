package net.minecraft.world.phys.shapes;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class EntityCollisionContext implements CollisionContext {
   protected static final CollisionContext EMPTY = new EntityCollisionContext(false, -Double.MAX_VALUE, ItemStack.EMPTY, (p_82891_) -> {
      return false;
   }, (Entity)null) {
      public boolean isAbove(VoxelShape p_82898_, BlockPos p_82899_, boolean p_82900_) {
         return p_82900_;
      }
   };
   private final boolean descending;
   private final double entityBottom;
   private final ItemStack heldItem;
   private final Predicate<Fluid> canStandOnFluid;
   @Nullable
   private final Entity entity;

   protected EntityCollisionContext(boolean pDescending, double pEntityBottom, ItemStack pHeldItem, Predicate<Fluid> pCanStandOnFluid, @Nullable Entity pEntity) {
      this.descending = pDescending;
      this.entityBottom = pEntityBottom;
      this.heldItem = pHeldItem;
      this.canStandOnFluid = pCanStandOnFluid;
      this.entity = pEntity;
   }

   /** @deprecated */
   @Deprecated
   protected EntityCollisionContext(Entity pEntity) {
      this(pEntity.isDescending(), pEntity.getY(), pEntity instanceof LivingEntity ? ((LivingEntity)pEntity).getMainHandItem() : ItemStack.EMPTY, pEntity instanceof LivingEntity ? ((LivingEntity)pEntity)::canStandOnFluid : (p_82881_) -> {
         return false;
      }, pEntity);
   }

   public boolean isHoldingItem(Item pItem) {
      return this.heldItem.is(pItem);
   }

   public boolean canStandOnFluid(FluidState pState, FlowingFluid pFlowing) {
      return this.canStandOnFluid.test(pFlowing) && !pState.getType().isSame(pFlowing);
   }

   public boolean isDescending() {
      return this.descending;
   }

   public boolean isAbove(VoxelShape pShape, BlockPos pPos, boolean pCanAscend) {
      return this.entityBottom > (double)pPos.getY() + pShape.max(Direction.Axis.Y) - (double)1.0E-5F;
   }

   @Nullable
   public Entity getEntity() {
      return this.entity;
   }
}