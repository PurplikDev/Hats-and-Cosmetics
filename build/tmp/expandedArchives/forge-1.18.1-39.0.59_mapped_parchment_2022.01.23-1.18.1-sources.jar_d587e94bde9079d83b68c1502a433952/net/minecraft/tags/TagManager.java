package net.minecraft.tags;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TagManager implements PreparableReloadListener {
   private static final Logger LOGGER = LogManager.getLogger();
   private final RegistryAccess registryAccess;
   protected java.util.Map<ResourceLocation, TagLoader<?>> customTagTypeReaders = net.minecraftforge.common.ForgeTagHandler.createCustomTagTypeReaders();
   private TagContainer tags = TagContainer.EMPTY;

   public TagManager(RegistryAccess pRegistryAccess) {
      this.registryAccess = pRegistryAccess;
   }

   public TagContainer getTags() {
      return this.tags;
   }

   public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier pStage, ResourceManager pResourceManager, ProfilerFiller pPreparationsProfiler, ProfilerFiller pReloadProfiler, Executor pBackgroundExecutor, Executor pGameExecutor) {
      List<TagManager.LoaderInfo<?>> list = Lists.newArrayList();
      StaticTags.visitHelpers((p_144583_) -> {
         TagManager.LoaderInfo<?> loaderinfo = this.createLoader(pResourceManager, pBackgroundExecutor, p_144583_);
         if (loaderinfo != null) {
            list.add(loaderinfo);
         }

      });
      return CompletableFuture.allOf(list.stream().map((p_144591_) -> {
         return p_144591_.pendingLoad;
      }).toArray((p_144574_) -> {
         return new CompletableFuture[p_144574_];
      })).thenCompose(pStage::wait).thenAcceptAsync((p_144594_) -> {
         TagContainer.Builder tagcontainer$builder = new TagContainer.Builder();
         list.forEach((p_144586_) -> {
            p_144586_.addToBuilder(tagcontainer$builder);
         });
         TagContainer tagcontainer = tagcontainer$builder.build();
         Multimap<ResourceKey<? extends Registry<?>>, ResourceLocation> multimap = StaticTags.getAllMissingTags(tagcontainer);
         if (!multimap.isEmpty()) {
            throw new IllegalStateException("Missing required tags: " + (String)multimap.entries().stream().map((p_144596_) -> {
               return p_144596_.getKey() + ":" + p_144596_.getValue();
            }).sorted().collect(Collectors.joining(",")));
         } else {
            tagcontainer = net.minecraftforge.common.ForgeTagHandler.reinjectOptionalTags(tagcontainer);
            SerializationTags.bind(tagcontainer);
            this.tags = tagcontainer;
         }
      }, pGameExecutor);
   }

   @Nullable
   private <T> TagManager.LoaderInfo<T> createLoader(ResourceManager pResourceManager, Executor pBackgroundExecutor, StaticTagHelper<T> pHelper) {
      Optional<? extends Registry<T>> optional = this.registryAccess.registry(pHelper.getKey());
      if (optional.isPresent()) {
         Registry<T> registry = optional.get();
         TagLoader<T> tagloader = new TagLoader<>(registry::getOptional, pHelper.getDirectory());
         CompletableFuture<? extends TagCollection<T>> completablefuture = CompletableFuture.supplyAsync(() -> {
            return tagloader.loadAndBuild(pResourceManager);
         }, pBackgroundExecutor);
         return new TagManager.LoaderInfo<>(pHelper, completablefuture);
      } else {
         if (net.minecraftforge.common.ForgeTagHandler.getCustomTagTypeNames().contains(pHelper.getKey().location()))
            return new TagManager.LoaderInfo<>(pHelper, CompletableFuture.supplyAsync(() -> ((TagLoader<T>) customTagTypeReaders.get(pHelper.getKey().location())).loadAndBuild(pResourceManager), pBackgroundExecutor));
         LOGGER.warn("Can't find registry for {}", (Object)pHelper.getKey());
         return null;
      }
   }

   static class LoaderInfo<T> {
      private final StaticTagHelper<T> helper;
      final CompletableFuture<? extends TagCollection<T>> pendingLoad;

      LoaderInfo(StaticTagHelper<T> pHelper, CompletableFuture<? extends TagCollection<T>> pPendingLoad) {
         this.helper = pHelper;
         this.pendingLoad = pPendingLoad;
      }

      public void addToBuilder(TagContainer.Builder pBuilder) {
         pBuilder.add(this.helper.getKey(), this.pendingLoad.join());
      }
   }
}
