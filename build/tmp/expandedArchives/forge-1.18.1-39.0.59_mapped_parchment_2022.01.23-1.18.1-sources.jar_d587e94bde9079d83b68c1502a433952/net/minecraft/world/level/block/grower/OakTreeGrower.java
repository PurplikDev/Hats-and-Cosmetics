package net.minecraft.world.level.block.grower;

import java.util.Random;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class OakTreeGrower extends AbstractTreeGrower {
   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of this tree
    */
   protected ConfiguredFeature<?, ?> getConfiguredFeature(Random pRandom, boolean pLargeHive) {
      if (pRandom.nextInt(10) == 0) {
         return pLargeHive ? TreeFeatures.FANCY_OAK_BEES_005 : TreeFeatures.FANCY_OAK;
      } else {
         return pLargeHive ? TreeFeatures.OAK_BEES_005 : TreeFeatures.OAK;
      }
   }
}