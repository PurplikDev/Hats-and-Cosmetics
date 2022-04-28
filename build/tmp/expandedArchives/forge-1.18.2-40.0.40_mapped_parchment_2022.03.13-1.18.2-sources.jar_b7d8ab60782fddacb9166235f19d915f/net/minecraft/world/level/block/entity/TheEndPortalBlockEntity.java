package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class TheEndPortalBlockEntity extends BlockEntity {
   protected TheEndPortalBlockEntity(BlockEntityType<?> p_155855_, BlockPos p_155856_, BlockState p_155857_) {
      super(p_155855_, p_155856_, p_155857_);
   }

   public TheEndPortalBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
      this(BlockEntityType.END_PORTAL, pWorldPosition, pBlockState);
   }

   public boolean shouldRenderFace(Direction pFace) {
      return pFace.getAxis() == Direction.Axis.Y;
   }
}