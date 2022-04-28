package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class TallFlowerBlock extends DoublePlantBlock implements BonemealableBlock {
   public TallFlowerBlock(BlockBehaviour.Properties p_57296_) {
      super(p_57296_);
   }

   public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
      return false;
   }

   /**
    * @return whether bonemeal can be used on this block
    */
   public boolean isValidBonemealTarget(BlockGetter pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
      return true;
   }

   public boolean isBonemealSuccess(Level pLevel, Random pRand, BlockPos pPos, BlockState pState) {
      return true;
   }

   public void performBonemeal(ServerLevel pLevel, Random pRand, BlockPos pPos, BlockState pState) {
      popResource(pLevel, pPos, new ItemStack(this));
   }
}