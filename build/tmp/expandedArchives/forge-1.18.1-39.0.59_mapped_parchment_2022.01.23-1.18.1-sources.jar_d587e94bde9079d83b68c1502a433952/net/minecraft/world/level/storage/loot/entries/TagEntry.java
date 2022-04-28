package net.minecraft.world.level.storage.loot.entries;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import java.util.function.Consumer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot pool entry container that generates based on an item tag.
 * If {@code expand} is set to true, it will expand into separate LootPoolEntries for every item in the tag, otherwise
 * it will simply generate all items in the tag.
 */
public class TagEntry extends LootPoolSingletonContainer {
   final Tag<Item> tag;
   final boolean expand;

   TagEntry(Tag<Item> pTag, boolean pExpand, int pWeight, int pQuality, LootItemCondition[] pConditions, LootItemFunction[] pFunctions) {
      super(pWeight, pQuality, pConditions, pFunctions);
      this.tag = pTag;
      this.expand = pExpand;
   }

   public LootPoolEntryType getType() {
      return LootPoolEntries.TAG;
   }

   /**
    * Generate the loot stacks of this entry.
    * Contrary to the method name this method does not always generate one stack, it can also generate zero or multiple
    * stacks.
    */
   public void createItemStack(Consumer<ItemStack> pStackConsumer, LootContext pLootContext) {
      this.tag.getValues().forEach((p_79852_) -> {
         pStackConsumer.accept(new ItemStack(p_79852_));
      });
   }

   private boolean expandTag(LootContext pContext, Consumer<LootPoolEntry> pGeneratorConsumer) {
      if (!this.canRun(pContext)) {
         return false;
      } else {
         for(final Item item : this.tag.getValues()) {
            pGeneratorConsumer.accept(new LootPoolSingletonContainer.EntryBase() {
               /**
                * Generate the loot stacks of this entry.
                * Contrary to the method name this method does not always generate one stack, it can also generate zero
                * or multiple stacks.
                */
               public void createItemStack(Consumer<ItemStack> p_79869_, LootContext p_79870_) {
                  p_79869_.accept(new ItemStack(item));
               }
            });
         }

         return true;
      }
   }

   /**
    * Expand this loot pool entry container by calling {@code entryConsumer} with any applicable entries
    * 
    * @return whether this loot pool entry container successfully expanded or not
    */
   public boolean expand(LootContext pLootContext, Consumer<LootPoolEntry> pEntryConsumer) {
      return this.expand ? this.expandTag(pLootContext, pEntryConsumer) : super.expand(pLootContext, pEntryConsumer);
   }

   public static LootPoolSingletonContainer.Builder<?> tagContents(Tag<Item> pTag) {
      return simpleBuilder((p_165166_, p_165167_, p_165168_, p_165169_) -> {
         return new TagEntry(pTag, false, p_165166_, p_165167_, p_165168_, p_165169_);
      });
   }

   public static LootPoolSingletonContainer.Builder<?> expandTag(Tag<Item> pTag) {
      return simpleBuilder((p_79841_, p_79842_, p_79843_, p_79844_) -> {
         return new TagEntry(pTag, true, p_79841_, p_79842_, p_79843_, p_79844_);
      });
   }

   public static class Serializer extends LootPoolSingletonContainer.Serializer<TagEntry> {
      public void serializeCustom(JsonObject pObject, TagEntry pContext, JsonSerializationContext pConditions) {
         super.serializeCustom(pObject, pContext, pConditions);
         pObject.addProperty("name", SerializationTags.getInstance().getIdOrThrow(Registry.ITEM_REGISTRY, pContext.tag, () -> {
            return new IllegalStateException("Unknown item tag");
         }).toString());
         pObject.addProperty("expand", pContext.expand);
      }

      protected TagEntry deserialize(JsonObject pObject, JsonDeserializationContext pContext, int pWeight, int pQuality, LootItemCondition[] pConditions, LootItemFunction[] pFunctions) {
         ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(pObject, "name"));
         Tag<Item> tag = SerializationTags.getInstance().getTagOrThrow(Registry.ITEM_REGISTRY, resourcelocation, (p_165172_) -> {
            return new JsonParseException("Can't find tag: " + p_165172_);
         });
         boolean flag = GsonHelper.getAsBoolean(pObject, "expand");
         return new TagEntry(tag, flag, pWeight, pQuality, pConditions, pFunctions);
      }
   }
}