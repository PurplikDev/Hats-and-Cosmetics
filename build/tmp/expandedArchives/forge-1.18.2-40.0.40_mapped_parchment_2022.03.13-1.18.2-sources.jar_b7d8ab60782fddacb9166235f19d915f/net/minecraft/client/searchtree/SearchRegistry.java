package net.minecraft.client.searchtree;

import com.google.common.collect.Maps;
import java.util.Map;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SearchRegistry implements ResourceManagerReloadListener {
   public static final SearchRegistry.Key<ItemStack> CREATIVE_NAMES = new SearchRegistry.Key<>();
   public static final SearchRegistry.Key<ItemStack> CREATIVE_TAGS = new SearchRegistry.Key<>();
   public static final SearchRegistry.Key<RecipeCollection> RECIPE_COLLECTIONS = new SearchRegistry.Key<>();
   private final Map<SearchRegistry.Key<?>, MutableSearchTree<?>> searchTrees = Maps.newHashMap();

   public void onResourceManagerReload(ResourceManager pResourceManager) {
      for(MutableSearchTree<?> mutablesearchtree : this.searchTrees.values()) {
         mutablesearchtree.refresh();
      }

   }

   public <T> void register(SearchRegistry.Key<T> pKey, MutableSearchTree<T> pValue) {
      this.searchTrees.put(pKey, pValue);
   }

   public <T> MutableSearchTree<T> getTree(SearchRegistry.Key<T> pKey) {
      return (MutableSearchTree<T>)this.searchTrees.get(pKey);
   }

   @OnlyIn(Dist.CLIENT)
   public static class Key<T> {
   }
}