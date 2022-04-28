package net.minecraft.data.tags;

import java.nio.file.Path;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class FluidTagsProvider extends TagsProvider<Fluid> {
   @Deprecated
   public FluidTagsProvider(DataGenerator pGenerator) {
      super(pGenerator, Registry.FLUID);
   }
   public FluidTagsProvider(DataGenerator pGenerator, String modId, @javax.annotation.Nullable net.minecraftforge.common.data.ExistingFileHelper existingFileHelper) {
      super(pGenerator, Registry.FLUID, modId, existingFileHelper);
   }

   protected void addTags() {
      this.tag(FluidTags.WATER).add(Fluids.WATER, Fluids.FLOWING_WATER);
      this.tag(FluidTags.LAVA).add(Fluids.LAVA, Fluids.FLOWING_LAVA);
   }

   /**
    * Resolves a Path for the location to save the given tag.
    */
   protected Path getPath(ResourceLocation pId) {
      return this.generator.getOutputFolder().resolve("data/" + pId.getNamespace() + "/tags/fluids/" + pId.getPath() + ".json");
   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "Fluid Tags";
   }
}
