package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.GlowLichenConfiguration;

public class GlowLichenFeature extends Feature<GlowLichenConfiguration> {
   public GlowLichenFeature(Codec<GlowLichenConfiguration> p_159838_) {
      super(p_159838_);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public boolean place(FeaturePlaceContext<GlowLichenConfiguration> pContext) {
      WorldGenLevel worldgenlevel = pContext.level();
      BlockPos blockpos = pContext.origin();
      Random random = pContext.random();
      GlowLichenConfiguration glowlichenconfiguration = pContext.config();
      if (!isAirOrWater(worldgenlevel.getBlockState(blockpos))) {
         return false;
      } else {
         List<Direction> list = getShuffledDirections(glowlichenconfiguration, random);
         if (placeGlowLichenIfPossible(worldgenlevel, blockpos, worldgenlevel.getBlockState(blockpos), glowlichenconfiguration, random, list)) {
            return true;
         } else {
            BlockPos.MutableBlockPos blockpos$mutableblockpos = blockpos.mutable();

            for(Direction direction : list) {
               blockpos$mutableblockpos.set(blockpos);
               List<Direction> list1 = getShuffledDirectionsExcept(glowlichenconfiguration, random, direction.getOpposite());

               for(int i = 0; i < glowlichenconfiguration.searchRange; ++i) {
                  blockpos$mutableblockpos.setWithOffset(blockpos, direction);
                  BlockState blockstate = worldgenlevel.getBlockState(blockpos$mutableblockpos);
                  if (!isAirOrWater(blockstate) && !blockstate.is(Blocks.GLOW_LICHEN)) {
                     break;
                  }

                  if (placeGlowLichenIfPossible(worldgenlevel, blockpos$mutableblockpos, blockstate, glowlichenconfiguration, random, list1)) {
                     return true;
                  }
               }
            }

            return false;
         }
      }
   }

   public static boolean placeGlowLichenIfPossible(WorldGenLevel pLevel, BlockPos pPos, BlockState pState, GlowLichenConfiguration pConfig, Random pRandom, List<Direction> pDirections) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();

      for(Direction direction : pDirections) {
         BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos.setWithOffset(pPos, direction));
         if (pConfig.canBePlacedOn.contains(blockstate.getBlock())) {
            GlowLichenBlock glowlichenblock = (GlowLichenBlock)Blocks.GLOW_LICHEN;
            BlockState blockstate1 = glowlichenblock.getStateForPlacement(pState, pLevel, pPos, direction);
            if (blockstate1 == null) {
               return false;
            }

            pLevel.setBlock(pPos, blockstate1, 3);
            pLevel.getChunk(pPos).markPosForPostprocessing(pPos);
            if (pRandom.nextFloat() < pConfig.chanceOfSpreading) {
               glowlichenblock.spreadFromFaceTowardRandomDirection(blockstate1, pLevel, pPos, direction, pRandom, true);
            }

            return true;
         }
      }

      return false;
   }

   public static List<Direction> getShuffledDirections(GlowLichenConfiguration pConfig, Random pRandom) {
      List<Direction> list = Lists.newArrayList(pConfig.validDirections);
      Collections.shuffle(list, pRandom);
      return list;
   }

   public static List<Direction> getShuffledDirectionsExcept(GlowLichenConfiguration pConfig, Random pRandom, Direction pExcludedDirection) {
      List<Direction> list = pConfig.validDirections.stream().filter((p_159857_) -> {
         return p_159857_ != pExcludedDirection;
      }).collect(Collectors.toList());
      Collections.shuffle(list, pRandom);
      return list;
   }

   private static boolean isAirOrWater(BlockState pState) {
      return pState.isAir() || pState.is(Blocks.WATER);
   }
}