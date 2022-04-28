package net.minecraft.world.level.block;

import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.RandomPatchConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

public class GrassBlock extends SpreadingSnowyDirtBlock implements BonemealableBlock {
   public GrassBlock(BlockBehaviour.Properties p_53685_) {
      super(p_53685_);
   }

   /**
    * @return whether bonemeal can be used on this block
    */
   public boolean isValidBonemealTarget(BlockGetter pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
      return pLevel.getBlockState(pPos.above()).isAir();
   }

   public boolean isBonemealSuccess(Level pLevel, Random pRand, BlockPos pPos, BlockState pState) {
      return true;
   }

   public void performBonemeal(ServerLevel pLevel, Random pRand, BlockPos pPos, BlockState pState) {
      BlockPos blockpos = pPos.above();
      BlockState blockstate = Blocks.GRASS.defaultBlockState();

      label46:
      for(int i = 0; i < 128; ++i) {
         BlockPos blockpos1 = blockpos;

         for(int j = 0; j < i / 16; ++j) {
            blockpos1 = blockpos1.offset(pRand.nextInt(3) - 1, (pRand.nextInt(3) - 1) * pRand.nextInt(3) / 2, pRand.nextInt(3) - 1);
            if (!pLevel.getBlockState(blockpos1.below()).is(this) || pLevel.getBlockState(blockpos1).isCollisionShapeFullBlock(pLevel, blockpos1)) {
               continue label46;
            }
         }

         BlockState blockstate1 = pLevel.getBlockState(blockpos1);
         if (blockstate1.is(blockstate.getBlock()) && pRand.nextInt(10) == 0) {
            ((BonemealableBlock)blockstate.getBlock()).performBonemeal(pLevel, pRand, blockpos1, blockstate1);
         }

         if (blockstate1.isAir()) {
            PlacedFeature placedfeature;
            if (pRand.nextInt(8) == 0) {
               List<ConfiguredFeature<?, ?>> list = pLevel.getBiome(blockpos1).getGenerationSettings().getFlowerFeatures();
               if (list.isEmpty()) {
                  continue;
               }

               placedfeature = ((RandomPatchConfiguration)list.get(0).config()).feature().get();
            } else {
               placedfeature = VegetationPlacements.GRASS_BONEMEAL;
            }

            placedfeature.place(pLevel, pLevel.getChunkSource().getGenerator(), pRand, blockpos1);
         }
      }

   }
}