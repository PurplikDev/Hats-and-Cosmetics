package net.minecraft.world.level.block.grower;

import java.util.Random;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class JungleTreeGrower extends AbstractMegaTreeGrower {
   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of this tree
    */
   protected ConfiguredFeature<?, ?> getConfiguredFeature(Random pRandom, boolean pLargeHive) {
      return TreeFeatures.JUNGLE_TREE_NO_VINE;
   }

   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of the huge variant of this tree
    */
   protected ConfiguredFeature<?, ?> getConfiguredMegaFeature(Random pRand) {
      return TreeFeatures.MEGA_JUNGLE_TREE;
   }
}