package net.minecraft.world.level.block.grower;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;

public class DarkOakTreeGrower extends AbstractMegaTreeGrower {
   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of this tree
    */
   @Nullable
   protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredFeature(Random p_204321_, boolean p_204322_) {
      return null;
   }

   /**
    * @return a {@link net.minecraft.world.level.levelgen.feature.ConfiguredFeature} of the huge variant of this tree
    */
   @Nullable
   protected Holder<? extends ConfiguredFeature<?, ?>> getConfiguredMegaFeature(Random p_204319_) {
      return TreeFeatures.DARK_OAK;
   }
}