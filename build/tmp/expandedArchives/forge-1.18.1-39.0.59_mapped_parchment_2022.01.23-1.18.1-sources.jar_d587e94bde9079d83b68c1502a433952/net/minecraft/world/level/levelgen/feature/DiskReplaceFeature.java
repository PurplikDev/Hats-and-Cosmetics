package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;

public class DiskReplaceFeature extends BaseDiskFeature {
   public DiskReplaceFeature(Codec<DiskConfiguration> p_65613_) {
      super(p_65613_);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public boolean place(FeaturePlaceContext<DiskConfiguration> p_159573_) {
      return !p_159573_.level().getFluidState(p_159573_.origin()).is(FluidTags.WATER) ? false : super.place(p_159573_);
   }
}