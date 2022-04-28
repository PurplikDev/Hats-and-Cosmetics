package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;

public abstract class AbstractHugeMushroomFeature extends Feature<HugeMushroomFeatureConfiguration> {
   public AbstractHugeMushroomFeature(Codec<HugeMushroomFeatureConfiguration> p_65093_) {
      super(p_65093_);
   }

   protected void placeTrunk(LevelAccessor pLevel, Random pRandom, BlockPos pPos, HugeMushroomFeatureConfiguration pConfig, int pMaxHeight, BlockPos.MutableBlockPos pMutablePos) {
      for(int i = 0; i < pMaxHeight; ++i) {
         pMutablePos.set(pPos).move(Direction.UP, i);
         if (!pLevel.getBlockState(pMutablePos).isSolidRender(pLevel, pMutablePos)) {
            this.setBlock(pLevel, pMutablePos, pConfig.stemProvider.getState(pRandom, pPos));
         }
      }

   }

   protected int getTreeHeight(Random pRandom) {
      int i = pRandom.nextInt(3) + 4;
      if (pRandom.nextInt(12) == 0) {
         i *= 2;
      }

      return i;
   }

   protected boolean isValidPosition(LevelAccessor pLevel, BlockPos pPos, int pMaxHeight, BlockPos.MutableBlockPos pMutablePos, HugeMushroomFeatureConfiguration pConfig) {
      int i = pPos.getY();
      if (i >= pLevel.getMinBuildHeight() + 1 && i + pMaxHeight + 1 < pLevel.getMaxBuildHeight()) {
         BlockState blockstate = pLevel.getBlockState(pPos.below());
         if (!isDirt(blockstate) && !blockstate.is(BlockTags.MUSHROOM_GROW_BLOCK)) {
            return false;
         } else {
            for(int j = 0; j <= pMaxHeight; ++j) {
               int k = this.getTreeRadiusForHeight(-1, -1, pConfig.foliageRadius, j);

               for(int l = -k; l <= k; ++l) {
                  for(int i1 = -k; i1 <= k; ++i1) {
                     BlockState blockstate1 = pLevel.getBlockState(pMutablePos.setWithOffset(pPos, l, j, i1));
                     if (!blockstate1.isAir() && !blockstate1.is(BlockTags.LEAVES)) {
                        return false;
                     }
                  }
               }
            }

            return true;
         }
      } else {
         return false;
      }
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public boolean place(FeaturePlaceContext<HugeMushroomFeatureConfiguration> pContext) {
      WorldGenLevel worldgenlevel = pContext.level();
      BlockPos blockpos = pContext.origin();
      Random random = pContext.random();
      HugeMushroomFeatureConfiguration hugemushroomfeatureconfiguration = pContext.config();
      int i = this.getTreeHeight(random);
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
      if (!this.isValidPosition(worldgenlevel, blockpos, i, blockpos$mutableblockpos, hugemushroomfeatureconfiguration)) {
         return false;
      } else {
         this.makeCap(worldgenlevel, random, blockpos, i, blockpos$mutableblockpos, hugemushroomfeatureconfiguration);
         this.placeTrunk(worldgenlevel, random, blockpos, hugemushroomfeatureconfiguration, i, blockpos$mutableblockpos);
         return true;
      }
   }

   protected abstract int getTreeRadiusForHeight(int p_65094_, int p_65095_, int p_65096_, int p_65097_);

   protected abstract void makeCap(LevelAccessor pLevel, Random pRandom, BlockPos pPos, int pTreeHeight, BlockPos.MutableBlockPos pMutablePos, HugeMushroomFeatureConfiguration pConfig);
}