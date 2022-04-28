package net.minecraft.world.level.material;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class EmptyFluid extends Fluid {
   public Item getBucket() {
      return Items.AIR;
   }

   public boolean canBeReplacedWith(FluidState pFluidState, BlockGetter pBlockReader, BlockPos pPos, Fluid pFluid, Direction pDirection) {
      return true;
   }

   public Vec3 getFlow(BlockGetter pBlockReader, BlockPos pPos, FluidState pFluidState) {
      return Vec3.ZERO;
   }

   public int getTickDelay(LevelReader p_75922_) {
      return 0;
   }

   protected boolean isEmpty() {
      return true;
   }

   protected float getExplosionResistance() {
      return 0.0F;
   }

   public float getHeight(FluidState p_75926_, BlockGetter p_75927_, BlockPos p_75928_) {
      return 0.0F;
   }

   public float getOwnHeight(FluidState p_75924_) {
      return 0.0F;
   }

   protected BlockState createLegacyBlock(FluidState pState) {
      return Blocks.AIR.defaultBlockState();
   }

   public boolean isSource(FluidState pState) {
      return false;
   }

   public int getAmount(FluidState pState) {
      return 0;
   }

   public VoxelShape getShape(FluidState p_75939_, BlockGetter p_75940_, BlockPos p_75941_) {
      return Shapes.empty();
   }
}