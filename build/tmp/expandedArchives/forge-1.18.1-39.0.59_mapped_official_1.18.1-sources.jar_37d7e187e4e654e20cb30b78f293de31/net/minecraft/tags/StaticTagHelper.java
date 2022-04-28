package net.minecraft.tags;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class StaticTagHelper<T> {
   private final ResourceKey<? extends Registry<T>> key;
   private final String directory;
   private TagCollection<T> source = TagCollection.empty();
   private final List<StaticTagHelper.Wrapper<T>> wrappers = Lists.newArrayList();
   @Nullable private static java.util.Map<ResourceLocation, List<StaticTagHelper.Wrapper<?>>> toAdd = com.google.common.collect.Maps.newHashMap();

   public StaticTagHelper(ResourceKey<? extends Registry<T>> p_144329_, String p_144330_) {
      this.key = p_144329_;
      this.directory = p_144330_;
   }

   public Tag.Named<T> bind(String p_13245_) {
      return add(new StaticTagHelper.Wrapper<>(new ResourceLocation(p_13245_)));
   }

   public net.minecraftforge.common.Tags.IOptionalNamedTag<T> createOptional(ResourceLocation key, @Nullable Set<java.util.function.Supplier<T>> defaults) {
      return add(new StaticTagHelper.OptionalNamedTag<>(key, defaults));
   }

   /** Call via ForgeTagHandler#makeWrapperTag to avoid any exceptions due to calling this after it is safe to call {@link #bind(String)} */
   public static <T> Tag.Named<T> createDelayedTag(ResourceLocation tagRegistry, ResourceLocation name) {
      return delayedAdd(tagRegistry, new StaticTagHelper.Wrapper<>(name));
   }

   /** Call via ForgeTagHandler#createOptionalTag to avoid any exceptions due to calling this after it is safe to call {@link #createOptional(ResourceLocation, Set)} */
   public static <T> net.minecraftforge.common.Tags.IOptionalNamedTag<T> createDelayedOptional(ResourceLocation tagRegistry, ResourceLocation key, @Nullable Set<java.util.function.Supplier<T>> defaults) {
      return delayedAdd(tagRegistry, new StaticTagHelper.OptionalNamedTag<>(key, defaults));
   }

   private static synchronized <T, R extends StaticTagHelper.Wrapper<T>> R delayedAdd(ResourceLocation tagRegistry, R tag) {
      if (toAdd == null) throw new RuntimeException("Creating delayed tags or optional tags, is only supported before custom tag types have been added.");
      toAdd.computeIfAbsent(tagRegistry, registry -> Lists.newArrayList()).add(tag);
      return tag;
   }

   public static void performDelayedAdd() {
      if (toAdd != null) {
         for (java.util.Map.Entry<ResourceLocation, List<StaticTagHelper.Wrapper<?>>> entry : toAdd.entrySet()) {
            StaticTagHelper<?> tagRegistry = StaticTags.get(entry.getKey());
            if (tagRegistry == null) throw new RuntimeException("A mod attempted to add a delayed tag for a registry that doesn't have custom tag support.");
            for (StaticTagHelper.Wrapper<?> tag : entry.getValue()) {
               tagRegistry.add((StaticTagHelper.Wrapper) tag);
            }
         }
         toAdd = null;
      }
   }

   private <R extends StaticTagHelper.Wrapper<T>> R add(R namedtag) {
      namedtag.rebind(source::getTag);
      this.wrappers.add(namedtag);
      return namedtag;
   }

   public TagCollection<T> reinjectOptionalTags(TagCollection<T> tagCollection) {
      java.util.Map<ResourceLocation, Tag<T>> currentTags = tagCollection.getAllTags();
      java.util.Map<ResourceLocation, Tag<T>> missingOptionals = this.wrappers.stream().filter(e -> e instanceof OptionalNamedTag && !currentTags.containsKey(e.getName())).collect(Collectors.toMap(Wrapper::getName, namedTag -> {
         OptionalNamedTag<T> optionalNamedTag = (OptionalNamedTag<T>) namedTag;
         optionalNamedTag.defaulted = true;
         return optionalNamedTag.resolveDefaulted();
      }, (existingTag, newTag) -> SetTag.create(ImmutableSet.<T>builder().addAll(existingTag.getValues()).addAll(newTag.getValues()).build())));
      if (!missingOptionals.isEmpty()) {
         missingOptionals.putAll(currentTags);
         return TagCollection.of(missingOptionals);
      }
      return tagCollection;
   }

   public void resetToEmpty() {
      this.source = TagCollection.empty();
      Tag<T> tag = SetTag.empty();
      this.wrappers.forEach((p_13235_) -> {
         p_13235_.rebind((p_144335_) -> {
            return tag;
         });
      });
   }

   public void reset(TagContainer p_13243_) {
      TagCollection<T> tagcollection = p_13243_.getOrEmpty(this.key);
      this.source = tagcollection;
      this.wrappers.forEach((p_13241_) -> {
         p_13241_.rebind(tagcollection::getTag);
      });
   }

   public TagCollection<T> getAllTags() {
      return this.source;
   }

   public Set<ResourceLocation> getMissingTags(TagContainer p_13248_) {
      TagCollection<T> tagcollection = p_13248_.getOrEmpty(this.key);
      Set<ResourceLocation> set = this.wrappers.stream().filter(e -> !(e instanceof OptionalNamedTag)).map(StaticTagHelper.Wrapper::getName).collect(Collectors.toSet());
      ImmutableSet<ResourceLocation> immutableset = ImmutableSet.copyOf(tagcollection.getAvailableTags());
      return Sets.difference(set, immutableset);
   }

   public ResourceKey<? extends Registry<T>> getKey() {
      return this.key;
   }

   public String getDirectory() {
      return this.directory;
   }

   protected void addToCollection(TagContainer.Builder p_144337_) {
      p_144337_.add(this.key, TagCollection.of(this.wrappers.stream().distinct().collect(Collectors.toMap(Tag.Named::getName, (p_144332_) -> {
         return p_144332_;
      }))));
   }

   static class Wrapper<T> implements Tag.Named<T> {
      @Nullable
      protected Tag<T> tag;
      protected final ResourceLocation name;

      Wrapper(ResourceLocation p_13253_) {
         this.name = p_13253_;
      }

      public ResourceLocation getName() {
         return this.name;
      }

      private Tag<T> resolve() {
         if (this.tag == null) {
            throw new IllegalStateException("Tag " + this.name + " used before it was bound");
         } else {
            return this.tag;
         }
      }

      void rebind(Function<ResourceLocation, Tag<T>> p_13261_) {
         this.tag = p_13261_.apply(this.name);
      }

      public boolean contains(T p_13259_) {
         return this.resolve().contains(p_13259_);
      }

      public List<T> getValues() {
         return this.resolve().getValues();
      }

      @Override
      public String toString() {
          return "NamedTag[" + getName() + ']';
      }
      @Override public boolean equals(Object o) { return (o == this) || (o instanceof Tag.Named && java.util.Objects.equals(this.getName(), ((Tag.Named<T>)o).getName())); }
      @Override public int hashCode() { return getName().hashCode(); }
   }

   private static class OptionalNamedTag<T> extends Wrapper<T> implements net.minecraftforge.common.Tags.IOptionalNamedTag<T> {
      @Nullable
      private final Set<java.util.function.Supplier<T>> defaults;
      private boolean defaulted = false;

      private OptionalNamedTag(ResourceLocation name, @Nullable Set<java.util.function.Supplier<T>> defaults) {
         super(name);
         this.defaults = defaults;
      }

      @Override
      public boolean isDefaulted() {
         return defaulted;
      }

      SetTag<T> resolveDefaulted() {
         if (defaults == null || defaults.isEmpty()) {
            return SetTag.empty();
         }
         return SetTag.create(ImmutableSet.copyOf(defaults.stream().map(java.util.function.Supplier::get).collect(Collectors.toSet())));
      }

      @Override
      public String toString() {
          return "OptionalNamedTag[" + getName() + ']';
      }
   }
}
