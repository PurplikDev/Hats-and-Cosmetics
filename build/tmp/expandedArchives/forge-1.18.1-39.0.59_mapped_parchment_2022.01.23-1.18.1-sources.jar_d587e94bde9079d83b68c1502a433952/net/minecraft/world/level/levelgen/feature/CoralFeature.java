package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public abstract class CoralFeature extends Feature<NoneFeatureConfiguration> {
   public CoralFeature(Codec<NoneFeatureConfiguration> p_65429_) {
      super(p_65429_);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> pContext) {
      Random random = pContext.random();
      WorldGenLevel worldgenlevel = pContext.level();
      BlockPos blockpos = pContext.origin();
      BlockState blockstate = BlockTags.CORAL_BLOCKS.getRandomElement(random).defaultBlockState();
      return this.placeFeature(worldgenlevel, random, blockpos, blockstate);
   }

   protected abstract boolean placeFeature(LevelAccessor pLevel, Random pRandom, BlockPos pPos, BlockState pState);

   protected boolean placeCoralBlock(LevelAccessor pLevel, Random pRandom, BlockPos pPos, BlockState pState) {
      BlockPos blockpos = pPos.above();
      BlockState blockstate = pLevel.getBlockState(pPos);
      if ((blockstate.is(Blocks.WATER) || blockstate.is(BlockTags.CORALS)) && pLevel.getBlockState(blockpos).is(Blocks.WATER)) {
         pLevel.setBlock(pPos, pState, 3);
         if (pRandom.nextFloat() < 0.25F) {
            pLevel.setBlock(blockpos, BlockTags.CORALS.getRandomElement(pRandom).defaultBlockState(), 2);
         } else if (pRandom.nextFloat() < 0.05F) {
            pLevel.setBlock(blockpos, Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, Integer.valueOf(pRandom.nextInt(4) + 1)), 2);
         }

         for(Direction direction : Direction.Plane.HORIZONTAL) {
            if (pRandom.nextFloat() < 0.2F) {
               BlockPos blockpos1 = pPos.relative(direction);
               if (pLevel.getBlockState(blockpos1).is(Blocks.WATER)) {
                  BlockState blockstate1 = BlockTags.WALL_CORALS.getRandomElement(pRandom).defaultBlockState();
                  if (blockstate1.hasProperty(BaseCoralWallFanBlock.FACING)) {
                     blockstate1 = blockstate1.setValue(BaseCoralWallFanBlock.FACING, direction);
                  }

                  pLevel.setBlock(blockpos1, blockstate1, 2);
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }
}