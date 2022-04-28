package net.minecraft.tags;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TagContainer {
   static final Logger LOGGER = LogManager.getLogger();
   public static final TagContainer EMPTY = new TagContainer(ImmutableMap.of());
   public final Map<ResourceKey<? extends Registry<?>>, TagCollection<?>> collections;

   public TagContainer(Map<ResourceKey<? extends Registry<?>>, TagCollection<?>> pCollections) {
      this.collections = pCollections;
   }

   @Nullable
   private <T> TagCollection<T> get(ResourceKey<? extends Registry<T>> pKey) {
      return (TagCollection<T>)this.collections.get(pKey);
   }

   public <T> TagCollection<T> getOrEmpty(ResourceKey<? extends Registry<T>> pKey) {
      return (TagCollection<T>)this.collections.getOrDefault(pKey, TagCollection.<T>empty());
   }

   public <T, E extends Exception> Tag<T> getTagOrThrow(ResourceKey<? extends Registry<T>> pKey, ResourceLocation pName, Function<ResourceLocation, E> pExceptionFunction) throws E {
      TagCollection<T> tagcollection = this.get(pKey);
      if (tagcollection == null) {
         throw pExceptionFunction.apply(pName);
      } else {
         Tag<T> tag = tagcollection.getTag(pName);
         if (tag == null) {
            throw pExceptionFunction.apply(pName);
         } else {
            return tag;
         }
      }
   }

   public <T, E extends Exception> ResourceLocation getIdOrThrow(ResourceKey<? extends Registry<T>> pKey, Tag<T> pTag, Supplier<E> pExceptionSuppplier) throws E {
      TagCollection<T> tagcollection = this.get(pKey);
      if (tagcollection == null) {
         throw pExceptionSuppplier.get();
      } else {
         ResourceLocation resourcelocation = tagcollection.getId(pTag);
         if (resourcelocation == null) {
            throw pExceptionSuppplier.get();
         } else {
            return resourcelocation;
         }
      }
   }

   public void getAll(TagContainer.CollectionConsumer pCollectionConsumer) {
      this.collections.forEach((p_144464_, p_144465_) -> {
         acceptCap(pCollectionConsumer, p_144464_, p_144465_);
      });
   }

   private static <T> void acceptCap(TagContainer.CollectionConsumer pCollectionConsumer, ResourceKey<? extends Registry<?>> pKey, TagCollection<?> pCollection) {
      pCollectionConsumer.accept((ResourceKey<? extends Registry<T>>)pKey, (TagCollection<T>)pCollection);
   }

   public void bindToGlobal() {
      StaticTags.resetAll(this);
      Blocks.rebuildCache();
      net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TagsUpdatedEvent(this));
   }

   public Map<ResourceKey<? extends Registry<?>>, TagCollection.NetworkPayload> serializeToNetwork(final RegistryAccess pRegistryAccess) {
      final Map<ResourceKey<? extends Registry<?>>, TagCollection.NetworkPayload> map = Maps.newHashMap();
      this.getAll(new TagContainer.CollectionConsumer() {
         public <T> void accept(ResourceKey<? extends Registry<T>> p_144481_, TagCollection<T> p_144482_) {
            Optional<? extends Registry<T>> optional = pRegistryAccess.registry(p_144481_);
            optional = net.minecraftforge.common.ForgeTagHandler.getWrapperRegistry(p_144481_, optional);
            if (optional.isPresent()) {
               map.put(p_144481_, p_144482_.serializeToNetwork(optional.get()));
            } else {
               TagContainer.LOGGER.error("Unknown registry {}", (Object)p_144481_);
            }

         }
      });
      return map;
   }

   public static TagContainer deserializeFromNetwork(RegistryAccess pRegistryAccess, Map<ResourceKey<? extends Registry<?>>, TagCollection.NetworkPayload> pPayloads) {
      TagContainer.Builder tagcontainer$builder = new TagContainer.Builder();
      pPayloads.forEach((p_144469_, p_144470_) -> {
         addTagsFromPayload(pRegistryAccess, tagcontainer$builder, p_144469_, p_144470_);
      });
      return tagcontainer$builder.build();
   }

   private static <T> void addTagsFromPayload(RegistryAccess pRegistryAccess, TagContainer.Builder pBuilder, ResourceKey<? extends Registry<? extends T>> pKey, TagCollection.NetworkPayload pPayload) {
      Optional<? extends Registry<? extends T>> optional = pRegistryAccess.registry(pKey);
      optional = net.minecraftforge.common.ForgeTagHandler.getWrapperRegistry((ResourceKey<? extends Registry<T>>) pKey, (Optional<? extends Registry<T>>) optional);
      if (optional.isPresent()) {
         pBuilder.add(pKey, TagCollection.createFromNetwork(pPayload, optional.get()));
      } else {
         LOGGER.error("Unknown registry {}", (Object)pKey);
      }

   }

   public static class Builder {
      private final ImmutableMap.Builder<ResourceKey<? extends Registry<?>>, TagCollection<?>> result = ImmutableMap.builder();

      public <T> TagContainer.Builder add(ResourceKey<? extends Registry<? extends T>> pKey, TagCollection<T> pCollection) {
         this.result.put(pKey, pCollection);
         return this;
      }

      public TagContainer build() {
         return new TagContainer(this.result.build());
      }
   }

   @FunctionalInterface
   interface CollectionConsumer {
      <T> void accept(ResourceKey<? extends Registry<T>> pKey, TagCollection<T> pCollection);
   }
}
