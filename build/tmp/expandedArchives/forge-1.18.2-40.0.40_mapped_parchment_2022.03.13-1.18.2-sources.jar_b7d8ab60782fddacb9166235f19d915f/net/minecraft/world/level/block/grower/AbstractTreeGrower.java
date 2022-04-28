package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public abstract class AbstractTreeGrower {
   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of this tree
    */
   @Nullable
   protected abstract Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(Random pRandom, boolean pLargeHive);

   public boolean growTree(ServerLevel pLevel, ChunkGenerator pChunkGenerator, BlockPos pPos, BlockState pState, Random pRandom) {
      Holder<? extends ConfiguredFeature<?, ?>> holder = this.getConfiguredFeature(pRandom, this.hasFlowers(pLevel, pPos));
      if (holder == null) {
         return false;
      } else {
         ConfiguredFeature<?, ?> configuredfeature = holder.value();
         pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 4);
         if (configuredfeature.place(pLevel, pChunkGenerator, pRandom, pPos)) {
            return true;
         } else {
            pLevel.setBlock(pPos, pState, 4);
            return false;
         }
      }
   }

   private boolean hasFlowers(LevelAccessor pLevel, BlockPos pPos) {
      for(BlockPos blockpos : BlockPos.MutableBlockPos.betweenClosed(pPos.below().north(2).west(2), pPos.above().south(2).east(2))) {
         if (pLevel.getBlockState(blockpos).is(BlockTags.FLOWERS)) {
            return true;
         }
      }

      return false;
   }
}