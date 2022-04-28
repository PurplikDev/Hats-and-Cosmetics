package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.configurations.TwistingVinesConfig;

public class TwistingVinesFeature extends Feature<TwistingVinesConfig> {
   public TwistingVinesFeature(Codec<TwistingVinesConfig> p_67292_) {
      super(p_67292_);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public boolean place(FeaturePlaceContext<TwistingVinesConfig> pContext) {
      WorldGenLevel worldgenlevel = pContext.level();
      BlockPos blockpos = pContext.origin();
      if (isInvalidPlacementLocation(worldgenlevel, blockpos)) {
         return false;
      } else {
         Random random = pContext.random();
         TwistingVinesConfig twistingvinesconfig = pContext.config();
         int i = twistingvinesconfig.spreadWidth();
         int j = twistingvinesconfig.spreadHeight();
         int k = twistingvinesconfig.maxHeight();
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(int l = 0; l < i * i; ++l) {
            blockpos$mutableblockpos.set(blockpos).move(Mth.nextInt(random, -i, i), Mth.nextInt(random, -j, j), Mth.nextInt(random, -i, i));
            if (findFirstAirBlockAboveGround(worldgenlevel, blockpos$mutableblockpos) && !isInvalidPlacementLocation(worldgenlevel, blockpos$mutableblockpos)) {
               int i1 = Mth.nextInt(random, 1, k);
               if (random.nextInt(6) == 0) {
                  i1 *= 2;
               }

               if (random.nextInt(5) == 0) {
                  i1 = 1;
               }

               int j1 = 17;
               int k1 = 25;
               placeWeepingVinesColumn(worldgenlevel, random, blockpos$mutableblockpos, i1, 17, 25);
            }
         }

         return true;
      }
   }

   private static boolean findFirstAirBlockAboveGround(LevelAccessor pLevel, BlockPos.MutableBlockPos pPos) {
      do {
         pPos.move(0, -1, 0);
         if (pLevel.isOutsideBuildHeight(pPos)) {
            return false;
         }
      } while(pLevel.getBlockState(pPos).isAir());

      pPos.move(0, 1, 0);
      return true;
   }

   public static void placeWeepingVinesColumn(LevelAccessor p_67300_, Random p_67301_, BlockPos.MutableBlockPos p_67302_, int p_67303_, int p_67304_, int p_67305_) {
      for(int i = 1; i <= p_67303_; ++i) {
         if (p_67300_.isEmptyBlock(p_67302_)) {
            if (i == p_67303_ || !p_67300_.isEmptyBlock(p_67302_.above())) {
               p_67300_.setBlock(p_67302_, Blocks.TWISTING_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, Integer.valueOf(Mth.nextInt(p_67301_, p_67304_, p_67305_))), 2);
               break;
            }

            p_67300_.setBlock(p_67302_, Blocks.TWISTING_VINES_PLANT.defaultBlockState(), 2);
         }

         p_67302_.move(Direction.UP);
      }

   }

   private static boolean isInvalidPlacementLocation(LevelAccessor pLevel, BlockPos pPos) {
      if (!pLevel.isEmptyBlock(pPos)) {
         return true;
      } else {
         BlockState blockstate = pLevel.getBlockState(pPos.below());
         return !blockstate.is(Blocks.NETHERRACK) && !blockstate.is(Blocks.WARPED_NYLIUM) && !blockstate.is(Blocks.WARPED_WART_BLOCK);
      }
   }
}