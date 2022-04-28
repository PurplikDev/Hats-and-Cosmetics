package net.minecraft.tags;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public class Tag<T> {
   private static final Tag<?> EMPTY = new Tag(List.of());
   final List<T> elements;

   public Tag(Collection<T> p_203860_) {
      this.elements = List.copyOf(p_203860_);
   }

   public List<T> getValues() {
      return this.elements;
   }

   public static <T> Tag<T> empty() {
      return (Tag<T>)EMPTY;
   }

   public static class Builder implements net.minecraftforge.common.extensions.IForgeRawTagBuilder {
      private final List<Tag.BuilderEntry> removeEntries = new java.util.ArrayList<>(); // FORGE: internal field for tracking "remove" entries
      /** FORGE: Gets a view of this builder's "remove" entries (only used during datagen) **/
      public Stream<Tag.BuilderEntry> getRemoveEntries() { return this.removeEntries.stream(); }
      public Tag.Builder remove(final Tag.BuilderEntry proxy) { // internal forge method for adding remove entries
         this.removeEntries.add(proxy);
         return this;
      }
      private final List<Tag.BuilderEntry> entries = new ArrayList<>();
      private boolean replace = false;

      public static Tag.Builder tag() {
         return new Tag.Builder();
      }

      public Tag.Builder add(Tag.BuilderEntry pEntry) {
         this.entries.add(pEntry);
         return this;
      }

      public Tag.Builder add(Tag.Entry pEntry, String pSource) {
         return this.add(new Tag.BuilderEntry(pEntry, pSource));
      }

      public Tag.Builder addElement(ResourceLocation pId, String pSource) {
         return this.add(new Tag.ElementEntry(pId), pSource);
      }

      public Tag.Builder addOptionalElement(ResourceLocation pId, String pSource) {
         return this.add(new Tag.OptionalElementEntry(pId), pSource);
      }

      public Tag.Builder addTag(ResourceLocation pId, String pSource) {
         return this.add(new Tag.TagEntry(pId), pSource);
      }

      public Tag.Builder addOptionalTag(ResourceLocation pId, String pSource) {
         return this.add(new Tag.OptionalTagEntry(pId), pSource);
      }

      public Tag.Builder replace(boolean value) {
         this.replace = value;
         return this;
      }

      public Tag.Builder replace() {
         return replace(true);
      }

      public <T> Either<Collection<Tag.BuilderEntry>, Tag<T>> build(Function<ResourceLocation, Tag<T>> pTagFunction, Function<ResourceLocation, T> pValueFunction) {
         ImmutableSet.Builder<T> builder = ImmutableSet.builder();
         List<Tag.BuilderEntry> list = new ArrayList<>();

         for(Tag.BuilderEntry tag$builderentry : this.entries) {
            if (!tag$builderentry.entry().build(pTagFunction, pValueFunction, builder::add)) {
               list.add(tag$builderentry);
            }
         }

         return list.isEmpty() ? Either.right(new Tag<>(builder.build())) : Either.left(list);
      }

      public Stream<Tag.BuilderEntry> getEntries() {
         return this.entries.stream();
      }

      public void visitRequiredDependencies(Consumer<ResourceLocation> pVisitor) {
         this.entries.forEach((p_144378_) -> {
            p_144378_.entry.visitRequiredDependencies(pVisitor);
         });
      }

      public void visitOptionalDependencies(Consumer<ResourceLocation> pVisitor) {
         this.entries.forEach((p_144370_) -> {
            p_144370_.entry.visitOptionalDependencies(pVisitor);
         });
      }

      public Tag.Builder addFromJson(JsonObject pJson, String pSource) {
         JsonArray jsonarray = GsonHelper.getAsJsonArray(pJson, "values");
         List<Tag.Entry> list = new ArrayList<>();

         for(JsonElement jsonelement : jsonarray) {
            list.add(parseEntry(jsonelement));
         }

         if (GsonHelper.getAsBoolean(pJson, "replace", false)) {
            this.entries.clear();
         }

         net.minecraftforge.common.ForgeHooks.deserializeTagAdditions(list, pJson, entries);
         list.forEach((p_13319_) -> {
            this.entries.add(new Tag.BuilderEntry(p_13319_, pSource));
         });
         return this;
      }

      private static Tag.Entry parseEntry(JsonElement pJson) {
         String s;
         boolean flag;
         if (pJson.isJsonObject()) {
            JsonObject jsonobject = pJson.getAsJsonObject();
            s = GsonHelper.getAsString(jsonobject, "id");
            flag = GsonHelper.getAsBoolean(jsonobject, "required", true);
         } else {
            s = GsonHelper.convertToString(pJson, "id");
            flag = true;
         }

         if (s.startsWith("#")) {
            ResourceLocation resourcelocation1 = new ResourceLocation(s.substring(1));
            return (Tag.Entry)(flag ? new Tag.TagEntry(resourcelocation1) : new Tag.OptionalTagEntry(resourcelocation1));
         } else {
            ResourceLocation resourcelocation = new ResourceLocation(s);
            return (Tag.Entry)(flag ? new Tag.ElementEntry(resourcelocation) : new Tag.OptionalElementEntry(resourcelocation));
         }
      }

      public JsonObject serializeToJson() {
         JsonObject jsonobject = new JsonObject();
         JsonArray jsonarray = new JsonArray();

         for(Tag.BuilderEntry tag$builderentry : this.entries) {
            tag$builderentry.entry().serializeTo(jsonarray);
         }

         jsonobject.addProperty("replace", replace);
         jsonobject.add("values", jsonarray);
         this.serializeTagAdditions(jsonobject);
         return jsonobject;
      }
   }

   public static record BuilderEntry(Tag.Entry entry, String source) {
      public String toString() {
         return this.entry + " (from " + this.source + ")";
      }
   }

   public static class ElementEntry implements Tag.Entry {
      private final ResourceLocation id;

      public ElementEntry(ResourceLocation pId) {
         this.id = pId;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> pResourceTagFunction, Function<ResourceLocation, T> pResourceElementFunction, Consumer<T> pElementConsumer) {
         T t = pResourceElementFunction.apply(this.id);
         if (t == null) {
            return false;
         } else {
            pElementConsumer.accept(t);
            return true;
         }
      }

      public void serializeTo(JsonArray pJsonArray) {
         pJsonArray.add(this.id.toString());
      }

      public boolean verifyIfPresent(Predicate<ResourceLocation> pRegistryPredicate, Predicate<ResourceLocation> pBuilderPredicate) {
         return pRegistryPredicate.test(this.id);
      }

      public String toString() {
         return this.id.toString();
      }
      @Override public boolean equals(Object o) { return o == this || (o instanceof Tag.ElementEntry && java.util.Objects.equals(this.id, ((Tag.ElementEntry) o).id)); }
   }

   public interface Entry {
      <T> boolean build(Function<ResourceLocation, Tag<T>> pTagFunction, Function<ResourceLocation, T> pValueFunction, Consumer<T> pAction);

      void serializeTo(JsonArray pJson);

      default void visitRequiredDependencies(Consumer<ResourceLocation> pVisitor) {
      }

      default void visitOptionalDependencies(Consumer<ResourceLocation> pVisitor) {
      }

      boolean verifyIfPresent(Predicate<ResourceLocation> pRegistryPredicate, Predicate<ResourceLocation> pBuilderPredicate);
   }

   public static class OptionalElementEntry implements Tag.Entry {
      private final ResourceLocation id;

      public OptionalElementEntry(ResourceLocation pId) {
         this.id = pId;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> pResourceTagFunction, Function<ResourceLocation, T> pResourceElementFunction, Consumer<T> pElementConsumer) {
         T t = pResourceElementFunction.apply(this.id);
         if (t != null) {
            pElementConsumer.accept(t);
         }

         return true;
      }

      public void serializeTo(JsonArray pJsonArray) {
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("id", this.id.toString());
         jsonobject.addProperty("required", false);
         pJsonArray.add(jsonobject);
      }

      public boolean verifyIfPresent(Predicate<ResourceLocation> pRegistryPredicate, Predicate<ResourceLocation> pBuilderPredicate) {
         return true;
      }

      public String toString() {
         return this.id + "?";
      }
   }

   public static class OptionalTagEntry implements Tag.Entry {
      private final ResourceLocation id;

      public OptionalTagEntry(ResourceLocation pId) {
         this.id = pId;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> pResourceTagFunction, Function<ResourceLocation, T> pResourceElementFunction, Consumer<T> pElementConsumer) {
         Tag<T> tag = pResourceTagFunction.apply(this.id);
         if (tag != null) {
            tag.elements.forEach(pElementConsumer);
         }

         return true;
      }

      public void serializeTo(JsonArray pJsonArray) {
         JsonObject jsonobject = new JsonObject();
         jsonobject.addProperty("id", "#" + this.id);
         jsonobject.addProperty("required", false);
         pJsonArray.add(jsonobject);
      }

      public String toString() {
         return "#" + this.id + "?";
      }

      public void visitOptionalDependencies(Consumer<ResourceLocation> pVisitor) {
         pVisitor.accept(this.id);
      }

      public boolean verifyIfPresent(Predicate<ResourceLocation> pRegistryPredicate, Predicate<ResourceLocation> pBuilderPredicate) {
         return true;
      }
   }

   public static class TagEntry implements Tag.Entry {
      private final ResourceLocation id;

      public TagEntry(ResourceLocation pId) {
         this.id = pId;
      }

      public <T> boolean build(Function<ResourceLocation, Tag<T>> pResourceTagFunction, Function<ResourceLocation, T> pResourceElementFunction, Consumer<T> pElementConsumer) {
         Tag<T> tag = pResourceTagFunction.apply(this.id);
         if (tag == null) {
            return false;
         } else {
            tag.elements.forEach(pElementConsumer);
            return true;
         }
      }

      public void serializeTo(JsonArray pJsonArray) {
         pJsonArray.add("#" + this.id);
      }

      public String toString() {
         return "#" + this.id;
      }
      @Override public boolean equals(Object o) { return o == this || (o instanceof Tag.TagEntry && java.util.Objects.equals(this.id, ((Tag.TagEntry) o).id)); }
      public ResourceLocation getId() { return id; }

      public boolean verifyIfPresent(Predicate<ResourceLocation> pRegistryPredicate, Predicate<ResourceLocation> pBuilderPredicate) {
         return pBuilderPredicate.test(this.id);
      }

      public void visitRequiredDependencies(Consumer<ResourceLocation> pVisitor) {
         pVisitor.accept(this.id);
      }
   }
}
