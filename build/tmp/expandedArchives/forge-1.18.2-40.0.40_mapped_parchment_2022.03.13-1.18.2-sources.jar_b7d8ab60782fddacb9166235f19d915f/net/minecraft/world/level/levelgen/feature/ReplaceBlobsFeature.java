package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceSphereConfiguration;

public class ReplaceBlobsFeature extends Feature<ReplaceSphereConfiguration> {
   public ReplaceBlobsFeature(Codec<ReplaceSphereConfiguration> p_66633_) {
      super(p_66633_);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public boolean place(FeaturePlaceContext<ReplaceSphereConfiguration> pContext) {
      ReplaceSphereConfiguration replacesphereconfiguration = pContext.config();
      WorldGenLevel worldgenlevel = pContext.level();
      Random random = pContext.random();
      Block block = replacesphereconfiguration.targetState.getBlock();
      BlockPos blockpos = findTarget(worldgenlevel, pContext.origin().mutable().clamp(Direction.Axis.Y, worldgenlevel.getMinBuildHeight() + 1, worldgenlevel.getMaxBuildHeight() - 1), block);
      if (blockpos == null) {
         return false;
      } else {
         int i = replacesphereconfiguration.radius().sample(random);
         int j = replacesphereconfiguration.radius().sample(random);
         int k = replacesphereconfiguration.radius().sample(random);
         int l = Math.max(i, Math.max(j, k));
         boolean flag = false;

         for(BlockPos blockpos1 : BlockPos.withinManhattan(blockpos, i, j, k)) {
            if (blockpos1.distManhattan(blockpos) > l) {
               break;
            }

            BlockState blockstate = worldgenlevel.getBlockState(blockpos1);
            if (blockstate.is(block)) {
               this.setBlock(worldgenlevel, blockpos1, replacesphereconfiguration.replaceState);
               flag = true;
            }
         }

         return flag;
      }
   }

   @Nullable
   private static BlockPos findTarget(LevelAccessor pLevel, BlockPos.MutableBlockPos pTopPos, Block pBlock) {
      while(pTopPos.getY() > pLevel.getMinBuildHeight() + 1) {
         BlockState blockstate = pLevel.getBlockState(pTopPos);
         if (blockstate.is(pBlock)) {
            return pTopPos;
         }

         pTopPos.move(Direction.DOWN);
      }

      return null;
   }
}