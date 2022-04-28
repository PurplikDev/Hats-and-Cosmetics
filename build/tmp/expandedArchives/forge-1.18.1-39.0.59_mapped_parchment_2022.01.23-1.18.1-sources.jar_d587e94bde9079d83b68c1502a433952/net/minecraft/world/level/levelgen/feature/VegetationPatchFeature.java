package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.configurations.VegetationPatchConfiguration;

public class VegetationPatchFeature extends Feature<VegetationPatchConfiguration> {
   public VegetationPatchFeature(Codec<VegetationPatchConfiguration> p_160588_) {
      super(p_160588_);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public boolean place(FeaturePlaceContext<VegetationPatchConfiguration> pContext) {
      WorldGenLevel worldgenlevel = pContext.level();
      VegetationPatchConfiguration vegetationpatchconfiguration = pContext.config();
      Random random = pContext.random();
      BlockPos blockpos = pContext.origin();
      Predicate<BlockState> predicate = getReplaceableTag(vegetationpatchconfiguration);
      int i = vegetationpatchconfiguration.xzRadius.sample(random) + 1;
      int j = vegetationpatchconfiguration.xzRadius.sample(random) + 1;
      Set<BlockPos> set = this.placeGroundPatch(worldgenlevel, vegetationpatchconfiguration, random, blockpos, predicate, i, j);
      this.distributeVegetation(pContext, worldgenlevel, vegetationpatchconfiguration, random, set, i, j);
      return !set.isEmpty();
   }

   protected Set<BlockPos> placeGroundPatch(WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, Random pRandom, BlockPos pPos, Predicate<BlockState> pState, int pXRadius, int pZRadius) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
      BlockPos.MutableBlockPos blockpos$mutableblockpos1 = blockpos$mutableblockpos.mutable();
      Direction direction = pConfig.surface.getDirection();
      Direction direction1 = direction.getOpposite();
      Set<BlockPos> set = new HashSet<>();

      for(int i = -pXRadius; i <= pXRadius; ++i) {
         boolean flag = i == -pXRadius || i == pXRadius;

         for(int j = -pZRadius; j <= pZRadius; ++j) {
            boolean flag1 = j == -pZRadius || j == pZRadius;
            boolean flag2 = flag || flag1;
            boolean flag3 = flag && flag1;
            boolean flag4 = flag2 && !flag3;
            if (!flag3 && (!flag4 || pConfig.extraEdgeColumnChance != 0.0F && !(pRandom.nextFloat() > pConfig.extraEdgeColumnChance))) {
               blockpos$mutableblockpos.setWithOffset(pPos, i, 0, j);

               for(int k = 0; pLevel.isStateAtPosition(blockpos$mutableblockpos, BlockBehaviour.BlockStateBase::isAir) && k < pConfig.verticalRange; ++k) {
                  blockpos$mutableblockpos.move(direction);
               }

               for(int i1 = 0; pLevel.isStateAtPosition(blockpos$mutableblockpos, (p_160626_) -> {
                  return !p_160626_.isAir();
               }) && i1 < pConfig.verticalRange; ++i1) {
                  blockpos$mutableblockpos.move(direction1);
               }

               blockpos$mutableblockpos1.setWithOffset(blockpos$mutableblockpos, pConfig.surface.getDirection());
               BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos1);
               if (pLevel.isEmptyBlock(blockpos$mutableblockpos) && blockstate.isFaceSturdy(pLevel, blockpos$mutableblockpos1, pConfig.surface.getDirection().getOpposite())) {
                  int l = pConfig.depth.sample(pRandom) + (pConfig.extraBottomBlockChance > 0.0F && pRandom.nextFloat() < pConfig.extraBottomBlockChance ? 1 : 0);
                  BlockPos blockpos = blockpos$mutableblockpos1.immutable();
                  boolean flag5 = this.placeGround(pLevel, pConfig, pState, pRandom, blockpos$mutableblockpos1, l);
                  if (flag5) {
                     set.add(blockpos);
                  }
               }
            }
         }
      }

      return set;
   }

   protected void distributeVegetation(FeaturePlaceContext<VegetationPatchConfiguration> pContext, WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, Random pRandom, Set<BlockPos> pPossiblePositions, int pXRadius, int pZRadius) {
      for(BlockPos blockpos : pPossiblePositions) {
         if (pConfig.vegetationChance > 0.0F && pRandom.nextFloat() < pConfig.vegetationChance) {
            this.placeVegetation(pLevel, pConfig, pContext.chunkGenerator(), pRandom, blockpos);
         }
      }

   }

   protected boolean placeVegetation(WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, ChunkGenerator pChunkGenerator, Random pRandom, BlockPos pPos) {
      return pConfig.vegetationFeature.get().place(pLevel, pChunkGenerator, pRandom, pPos.relative(pConfig.surface.getDirection().getOpposite()));
   }

   protected boolean placeGround(WorldGenLevel pLevel, VegetationPatchConfiguration pConfig, Predicate<BlockState> pReplaceableBlocks, Random pRandom, BlockPos.MutableBlockPos pMutablePos, int pMaxDistance) {
      for(int i = 0; i < pMaxDistance; ++i) {
         BlockState blockstate = pConfig.groundState.getState(pRandom, pMutablePos);
         BlockState blockstate1 = pLevel.getBlockState(pMutablePos);
         if (!blockstate.is(blockstate1.getBlock())) {
            if (!pReplaceableBlocks.test(blockstate1)) {
               return i != 0;
            }

            pLevel.setBlock(pMutablePos, blockstate, 2);
            pMutablePos.move(pConfig.surface.getDirection());
         }
      }

      return true;
   }

   private static Predicate<BlockState> getReplaceableTag(VegetationPatchConfiguration pConfig) {
      Tag<Block> tag = BlockTags.getAllTags().getTag(pConfig.replaceable);
      return tag == null ? (p_160624_) -> {
         return true;
      } : (p_160591_) -> {
         return p_160591_.is(tag);
      };
   }
}