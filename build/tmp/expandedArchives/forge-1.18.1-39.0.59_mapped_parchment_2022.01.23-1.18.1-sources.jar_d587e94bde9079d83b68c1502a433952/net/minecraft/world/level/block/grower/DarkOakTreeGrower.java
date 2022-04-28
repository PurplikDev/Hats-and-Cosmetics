package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class DarkOakTreeGrower extends AbstractMegaTreeGrower {
   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of this tree
    */
   @Nullable
   protected ConfiguredFeature<?, ?> getConfiguredFeature(Random pRandom, boolean pLargeHive) {
      return null;
   }

   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of the huge variant of this tree
    */
   @Nullable
   protected ConfiguredFeature<?, ?> getConfiguredMegaFeature(Random pRand) {
      return TreeFeatures.DARK_OAK;
   }
}