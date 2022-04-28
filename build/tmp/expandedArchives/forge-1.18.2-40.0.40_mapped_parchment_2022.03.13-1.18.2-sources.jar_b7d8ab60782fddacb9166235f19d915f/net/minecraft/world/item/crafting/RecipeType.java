package net.minecraft.world.item.crafting;

import java.util.Optional;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;

public interface RecipeType<T extends Recipe<?>> {
   RecipeType<CraftingRecipe> CRAFTING = register("crafting");
   RecipeType<SmeltingRecipe> SMELTING = register("smelting");
   RecipeType<BlastingRecipe> BLASTING = register("blasting");
   RecipeType<SmokingRecipe> SMOKING = register("smoking");
   RecipeType<CampfireCookingRecipe> CAMPFIRE_COOKING = register("campfire_cooking");
   RecipeType<StonecutterRecipe> STONECUTTING = register("stonecutting");
   RecipeType<UpgradeRecipe> SMITHING = register("smithing");

   static <T extends Recipe<?>> RecipeType<T> register(final String pIdentifier) {
      return Registry.register(Registry.RECIPE_TYPE, new ResourceLocation(pIdentifier), new RecipeType<T>() {
         public String toString() {
            return pIdentifier;
         }
      });
   }

   default <C extends Container> Optional<T> tryMatch(Recipe<C> pRecipe, Level pLevel, C pContainer) {
      return pRecipe.matches(pContainer, pLevel) ? Optional.of((T)pRecipe) : Optional.empty();
   }
}