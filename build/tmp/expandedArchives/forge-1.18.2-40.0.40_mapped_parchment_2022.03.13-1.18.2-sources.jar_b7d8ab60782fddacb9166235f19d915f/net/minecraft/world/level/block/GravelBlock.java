package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class GravelBlock extends FallingBlock {
   public GravelBlock(BlockBehaviour.Properties p_53736_) {
      super(p_53736_);
   }

   public int getDustColor(BlockState pState, BlockGetter pReader, BlockPos pPos) {
      return -8356741;
   }
}