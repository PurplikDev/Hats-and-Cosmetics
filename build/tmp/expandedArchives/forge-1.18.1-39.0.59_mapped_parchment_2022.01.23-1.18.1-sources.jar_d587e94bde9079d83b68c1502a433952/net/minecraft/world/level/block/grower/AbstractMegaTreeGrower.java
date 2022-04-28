package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public abstract class AbstractMegaTreeGrower extends AbstractTreeGrower {
   public boolean growTree(ServerLevel pLevel, ChunkGenerator pChunkGenerator, BlockPos pPos, BlockState pState, Random pRand) {
      for(int i = 0; i >= -1; --i) {
         for(int j = 0; j >= -1; --j) {
            if (isTwoByTwoSapling(pState, pLevel, pPos, i, j)) {
               return this.placeMega(pLevel, pChunkGenerator, pPos, pState, pRand, i, j);
            }
         }
      }

      return super.growTree(pLevel, pChunkGenerator, pPos, pState, pRand);
   }

   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of the huge variant of this tree
    */
   @Nullable
   protected abstract ConfiguredFeature<?, ?> getConfiguredMegaFeature(Random pRandom);

   public boolean placeMega(ServerLevel pLevel, ChunkGenerator pChunkGenerator, BlockPos pPos, BlockState pState, Random pRandom, int pBranchX, int pBranchY) {
      ConfiguredFeature<?, ?> configuredfeature = this.getConfiguredMegaFeature(pRandom);
      if (configuredfeature == null) {
         return false;
      } else {
         BlockState blockstate = Blocks.AIR.defaultBlockState();
         pLevel.setBlock(pPos.offset(pBranchX, 0, pBranchY), blockstate, 4);
         pLevel.setBlock(pPos.offset(pBranchX + 1, 0, pBranchY), blockstate, 4);
         pLevel.setBlock(pPos.offset(pBranchX, 0, pBranchY + 1), blockstate, 4);
         pLevel.setBlock(pPos.offset(pBranchX + 1, 0, pBranchY + 1), blockstate, 4);
         if (configuredfeature.place(pLevel, pChunkGenerator, pRandom, pPos.offset(pBranchX, 0, pBranchY))) {
            return true;
         } else {
            pLevel.setBlock(pPos.offset(pBranchX, 0, pBranchY), pState, 4);
            pLevel.setBlock(pPos.offset(pBranchX + 1, 0, pBranchY), pState, 4);
            pLevel.setBlock(pPos.offset(pBranchX, 0, pBranchY + 1), pState, 4);
            pLevel.setBlock(pPos.offset(pBranchX + 1, 0, pBranchY + 1), pState, 4);
            return false;
         }
      }
   }

   public static boolean isTwoByTwoSapling(BlockState pBlockUnder, BlockGetter pLevel, BlockPos pPos, int pXOffset, int pZOffset) {
      Block block = pBlockUnder.getBlock();
      return pLevel.getBlockState(pPos.offset(pXOffset, 0, pZOffset)).is(block) && pLevel.getBlockState(pPos.offset(pXOffset + 1, 0, pZOffset)).is(block) && pLevel.getBlockState(pPos.offset(pXOffset, 0, pZOffset + 1)).is(block) && pLevel.getBlockState(pPos.offset(pXOffset + 1, 0, pZOffset + 1)).is(block);
   }
}