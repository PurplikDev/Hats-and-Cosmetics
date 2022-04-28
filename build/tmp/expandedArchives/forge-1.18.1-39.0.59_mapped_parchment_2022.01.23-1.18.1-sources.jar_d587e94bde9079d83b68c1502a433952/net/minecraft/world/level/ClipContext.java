package net.minecraft.world.level;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ClipContext {
   private final Vec3 from;
   private final Vec3 to;
   private final ClipContext.Block block;
   private final ClipContext.Fluid fluid;
   private final CollisionContext collisionContext;

   public ClipContext(Vec3 pFrom, Vec3 pTo, ClipContext.Block pBlock, ClipContext.Fluid pFluid, @javax.annotation.Nullable Entity pEntity) {
      this.from = pFrom;
      this.to = pTo;
      this.block = pBlock;
      this.fluid = pFluid;
      this.collisionContext = pEntity == null ? CollisionContext.empty() : CollisionContext.of(pEntity);
   }

   public Vec3 getTo() {
      return this.to;
   }

   public Vec3 getFrom() {
      return this.from;
   }

   public VoxelShape getBlockShape(BlockState pBlockState, BlockGetter pLevel, BlockPos pPos) {
      return this.block.get(pBlockState, pLevel, pPos, this.collisionContext);
   }

   public VoxelShape getFluidShape(FluidState pState, BlockGetter pLevel, BlockPos pPos) {
      return this.fluid.canPick(pState) ? pState.getShape(pLevel, pPos) : Shapes.empty();
   }

   public static enum Block implements ClipContext.ShapeGetter {
      COLLIDER(BlockBehaviour.BlockStateBase::getCollisionShape),
      OUTLINE(BlockBehaviour.BlockStateBase::getShape),
      VISUAL(BlockBehaviour.BlockStateBase::getVisualShape);

      private final ClipContext.ShapeGetter shapeGetter;

      private Block(ClipContext.ShapeGetter p_45712_) {
         this.shapeGetter = p_45712_;
      }

      public VoxelShape get(BlockState pState, BlockGetter pBlock, BlockPos pPos, CollisionContext pCollisionContext) {
         return this.shapeGetter.get(pState, pBlock, pPos, pCollisionContext);
      }
   }

   public static enum Fluid {
      NONE((p_45736_) -> {
         return false;
      }),
      SOURCE_ONLY(FluidState::isSource),
      ANY((p_45734_) -> {
         return !p_45734_.isEmpty();
      });

      private final Predicate<FluidState> canPick;

      private Fluid(Predicate<FluidState> p_45730_) {
         this.canPick = p_45730_;
      }

      public boolean canPick(FluidState pState) {
         return this.canPick.test(pState);
      }
   }

   public interface ShapeGetter {
      VoxelShape get(BlockState pState, BlockGetter pBlock, BlockPos pPos, CollisionContext pCollisionContext);
   }
}
