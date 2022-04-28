package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RecipeManager extends SimpleJsonResourceReloadListener {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
   private static final Logger LOGGER = LogManager.getLogger();
   private Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> recipes = ImmutableMap.of();
   private Map<ResourceLocation, Recipe<?>> byName = ImmutableMap.of();
   private boolean hasErrors;

   public RecipeManager() {
      super(GSON, "recipes");
   }

   protected void apply(Map<ResourceLocation, JsonElement> pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      this.hasErrors = false;
      Map<RecipeType<?>, Builder<ResourceLocation, Recipe<?>>> map = Maps.newHashMap();
      Builder<ResourceLocation, Recipe<?>> builder = ImmutableMap.builder();

      for(Entry<ResourceLocation, JsonElement> entry : pObject.entrySet()) {
         ResourceLocation resourcelocation = entry.getKey();
         if (resourcelocation.getPath().startsWith("_")) continue; //Forge: filter anything beginning with "_" as it's used for metadata.

         try {
            if (entry.getValue().isJsonObject() && !net.minecraftforge.common.crafting.CraftingHelper.processConditions(entry.getValue().getAsJsonObject(), "conditions")) {
               LOGGER.debug("Skipping loading recipe {} as it's conditions were not met", resourcelocation);
               continue;
            }
            Recipe<?> recipe = fromJson(resourcelocation, GsonHelper.convertToJsonObject(entry.getValue(), "top element"));
            if (recipe == null) {
               LOGGER.info("Skipping loading recipe {} as it's serializer returned null", resourcelocation);
               continue;
            }
            map.computeIfAbsent(recipe.getType(), (p_44075_) -> {
               return ImmutableMap.builder();
            }).put(resourcelocation, recipe);
            builder.put(resourcelocation, recipe);
         } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
            LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonparseexception);
         }
      }

      this.recipes = map.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, (p_44033_) -> {
         return p_44033_.getValue().build();
      }));
      this.byName = builder.build();
      LOGGER.info("Loaded {} recipes", (int)map.size());
   }

   public boolean hadErrorsLoading() {
      return this.hasErrors;
   }

   public <C extends Container, T extends Recipe<C>> Optional<T> getRecipeFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
      return this.byType(pRecipeType).values().stream().flatMap((p_44064_) -> {
         return Util.toStream(pRecipeType.tryMatch(p_44064_, pLevel, pInventory));
      }).findFirst();
   }

   public <C extends Container, T extends Recipe<C>> List<T> getAllRecipesFor(RecipeType<T> pRecipeType) {
      return this.byType(pRecipeType).values().stream().map((p_44053_) -> {
         return (T)p_44053_;
      }).collect(Collectors.toList());
   }

   public <C extends Container, T extends Recipe<C>> List<T> getRecipesFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
      return this.byType(pRecipeType).values().stream().flatMap((p_44023_) -> {
         return Util.toStream(pRecipeType.tryMatch(p_44023_, pLevel, pInventory));
      }).sorted(Comparator.comparing((p_44012_) -> {
         return p_44012_.getResultItem().getDescriptionId();
      })).collect(Collectors.toList());
   }

   private <C extends Container, T extends Recipe<C>> Map<ResourceLocation, Recipe<C>> byType(RecipeType<T> pRecipeType) {
      return (Map<ResourceLocation, Recipe<C>>)(Map<ResourceLocation, T>)this.recipes.getOrDefault(pRecipeType, Collections.emptyMap());
   }

   public <C extends Container, T extends Recipe<C>> NonNullList<ItemStack> getRemainingItemsFor(RecipeType<T> pRecipeType, C pInventory, Level pLevel) {
      Optional<T> optional = this.getRecipeFor(pRecipeType, pInventory, pLevel);
      if (optional.isPresent()) {
         return optional.get().getRemainingItems(pInventory);
      } else {
         NonNullList<ItemStack> nonnulllist = NonNullList.withSize(pInventory.getContainerSize(), ItemStack.EMPTY);

         for(int i = 0; i < nonnulllist.size(); ++i) {
            nonnulllist.set(i, pInventory.getItem(i));
         }

         return nonnulllist;
      }
   }

   public Optional<? extends Recipe<?>> byKey(ResourceLocation pRecipeId) {
      return Optional.ofNullable(this.byName.get(pRecipeId));
   }

   public Collection<Recipe<?>> getRecipes() {
      return this.recipes.values().stream().flatMap((p_199910_) -> {
         return p_199910_.values().stream();
      }).collect(Collectors.toSet());
   }

   public Stream<ResourceLocation> getRecipeIds() {
      return this.recipes.values().stream().flatMap((p_199904_) -> {
         return p_199904_.keySet().stream();
      });
   }

   /**
    * Deserializes a recipe object from json data.
    */
   public static Recipe<?> fromJson(ResourceLocation pRecipeId, JsonObject pJson) {
      String s = GsonHelper.getAsString(pJson, "type");
      return Registry.RECIPE_SERIALIZER.getOptional(new ResourceLocation(s)).orElseThrow(() -> {
         return new JsonSyntaxException("Invalid or unsupported recipe type '" + s + "'");
      }).fromJson(pRecipeId, pJson);
   }

   public void replaceRecipes(Iterable<Recipe<?>> pRecipes) {
      this.hasErrors = false;
      Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> map = Maps.newHashMap();
      Builder<ResourceLocation, Recipe<?>> builder = ImmutableMap.builder();
      pRecipes.forEach((p_199908_) -> {
         Map<ResourceLocation, Recipe<?>> map1 = map.computeIfAbsent(p_199908_.getType(), (p_199912_) -> {
            return Maps.newHashMap();
         });
         ResourceLocation resourcelocation = p_199908_.getId();
         Recipe<?> recipe = map1.put(resourcelocation, p_199908_);
         builder.put(resourcelocation, p_199908_);
         if (recipe != null) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + resourcelocation);
         }
      });
      this.recipes = ImmutableMap.copyOf(map);
      this.byName = builder.build();
   }
}
