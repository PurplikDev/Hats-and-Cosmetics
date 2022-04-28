package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class AzaleaTreeGrower extends AbstractTreeGrower {
   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of this tree
    */
   @Nullable
   protected ConfiguredFeature<?, ?> getConfiguredFeature(Random p_155872_, boolean p_155873_) {
      return TreeFeatures.AZALEA_TREE;
   }
}