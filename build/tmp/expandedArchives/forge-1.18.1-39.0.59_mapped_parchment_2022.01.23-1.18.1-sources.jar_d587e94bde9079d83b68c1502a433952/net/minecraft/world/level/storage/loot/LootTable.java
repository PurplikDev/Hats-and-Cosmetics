package net.minecraft.world.level.storage.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LootTable {
   static final Logger LOGGER = LogManager.getLogger();
   public static final LootTable EMPTY = new LootTable(LootContextParamSets.EMPTY, new LootPool[0], new LootItemFunction[0]);
   public static final LootContextParamSet DEFAULT_PARAM_SET = LootContextParamSets.ALL_PARAMS;
   final LootContextParamSet paramSet;
   private final List<LootPool> pools;
   final LootItemFunction[] functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> compositeFunction;

   LootTable(LootContextParamSet pParamSet, LootPool[] pPools, LootItemFunction[] pFunctions) {
      this.paramSet = pParamSet;
      this.pools = Lists.newArrayList(pPools);
      this.functions = pFunctions;
      this.compositeFunction = LootItemFunctions.compose(pFunctions);
   }

   /**
    * Create a wrapped Consumer which will split stacks according to their maximum stack size before passing them on to
    * the given stackConsumer.
    */
   public static Consumer<ItemStack> createStackSplitter(Consumer<ItemStack> pStackConsumer) {
      return (p_79146_) -> {
         if (p_79146_.getCount() < p_79146_.getMaxStackSize()) {
            pStackConsumer.accept(p_79146_);
         } else {
            int i = p_79146_.getCount();

            while(i > 0) {
               ItemStack itemstack = p_79146_.copy();
               itemstack.setCount(Math.min(p_79146_.getMaxStackSize(), i));
               i -= itemstack.getCount();
               pStackConsumer.accept(itemstack);
            }
         }

      };
   }

   /**
    * Generate items to the given Consumer, ignoring maximum stack size.
    */
   public void getRandomItemsRaw(LootContext pContext, Consumer<ItemStack> pStacksOut) {
      if (pContext.addVisitedTable(this)) {
         Consumer<ItemStack> consumer = LootItemFunction.decorate(this.compositeFunction, pStacksOut, pContext);

         for(LootPool lootpool : this.pools) {
            lootpool.addRandomItems(consumer, pContext);
         }

         pContext.removeVisitedTable(this);
      } else {
         LOGGER.warn("Detected infinite loop in loot tables");
      }

   }

   /**
    * Generate random items to the given Consumer, ensuring they do not exeed their maximum stack size.
    */
   @Deprecated //Use other method or manually call ForgeHooks.modifyLoot
   public void getRandomItems(LootContext pContextData, Consumer<ItemStack> pStacksOut) {
      this.getRandomItemsRaw(pContextData, createStackSplitter(pStacksOut));
   }

   /**
    * Generate random items to a List.
    */
   public List<ItemStack> getRandomItems(LootContext pContext) {
      List<ItemStack> list = Lists.newArrayList();
      this.getRandomItems(pContext, list::add);
      list = net.minecraftforge.common.ForgeHooks.modifyLoot(this.getLootTableId(), list, pContext);
      return list;
   }

   /**
    * Get the parameter set for this LootTable.
    */
   public LootContextParamSet getParamSet() {
      return this.paramSet;
   }

   /**
    * Validate this LootTable using the given ValidationContext.
    */
   public void validate(ValidationContext pValidator) {
      for(int i = 0; i < this.pools.size(); ++i) {
         this.pools.get(i).validate(pValidator.forChild(".pools[" + i + "]"));
      }

      for(int j = 0; j < this.functions.length; ++j) {
         this.functions[j].validate(pValidator.forChild(".functions[" + j + "]"));
      }

   }

   /**
    * Fill the given container with random items from this loot table.
    */
   public void fill(Container pContainer, LootContext pContext) {
      List<ItemStack> list = this.getRandomItems(pContext);
      Random random = pContext.getRandom();
      List<Integer> list1 = this.getAvailableSlots(pContainer, random);
      this.shuffleAndSplitItems(list, list1.size(), random);

      for(ItemStack itemstack : list) {
         if (list1.isEmpty()) {
            LOGGER.warn("Tried to over-fill a container");
            return;
         }

         if (itemstack.isEmpty()) {
            pContainer.setItem(list1.remove(list1.size() - 1), ItemStack.EMPTY);
         } else {
            pContainer.setItem(list1.remove(list1.size() - 1), itemstack);
         }
      }

   }

   /**
    * shuffles items by changing their order and splitting stacks
    */
   private void shuffleAndSplitItems(List<ItemStack> pStacks, int pEmptySlotsCount, Random pRand) {
      List<ItemStack> list = Lists.newArrayList();
      Iterator<ItemStack> iterator = pStacks.iterator();

      while(iterator.hasNext()) {
         ItemStack itemstack = iterator.next();
         if (itemstack.isEmpty()) {
            iterator.remove();
         } else if (itemstack.getCount() > 1) {
            list.add(itemstack);
            iterator.remove();
         }
      }

      while(pEmptySlotsCount - pStacks.size() - list.size() > 0 && !list.isEmpty()) {
         ItemStack itemstack2 = list.remove(Mth.nextInt(pRand, 0, list.size() - 1));
         int i = Mth.nextInt(pRand, 1, itemstack2.getCount() / 2);
         ItemStack itemstack1 = itemstack2.split(i);
         if (itemstack2.getCount() > 1 && pRand.nextBoolean()) {
            list.add(itemstack2);
         } else {
            pStacks.add(itemstack2);
         }

         if (itemstack1.getCount() > 1 && pRand.nextBoolean()) {
            list.add(itemstack1);
         } else {
            pStacks.add(itemstack1);
         }
      }

      pStacks.addAll(list);
      Collections.shuffle(pStacks, pRand);
   }

   private List<Integer> getAvailableSlots(Container pInventory, Random pRand) {
      List<Integer> list = Lists.newArrayList();

      for(int i = 0; i < pInventory.getContainerSize(); ++i) {
         if (pInventory.getItem(i).isEmpty()) {
            list.add(i);
         }
      }

      Collections.shuffle(list, pRand);
      return list;
   }

   public static LootTable.Builder lootTable() {
      return new LootTable.Builder();
   }

   //======================== FORGE START =============================================
   private boolean isFrozen = false;
   public void freeze() {
      this.isFrozen = true;
      this.pools.forEach(LootPool::freeze);
   }
   public boolean isFrozen(){ return this.isFrozen; }
   private void checkFrozen() {
      if (this.isFrozen())
         throw new RuntimeException("Attempted to modify LootTable after being finalized!");
   }

   private ResourceLocation lootTableId;
   public void setLootTableId(final ResourceLocation id) {
      if (this.lootTableId != null) throw new IllegalStateException("Attempted to rename loot table from '" + this.lootTableId + "' to '" + id + "': this is not supported");
      this.lootTableId = java.util.Objects.requireNonNull(id);
   }
   public ResourceLocation getLootTableId() { return this.lootTableId; }

   public LootPool getPool(String name) {
      return pools.stream().filter(e -> name.equals(e.getName())).findFirst().orElse(null);
   }

   public LootPool removePool(String name) {
      checkFrozen();
      for (LootPool pool : this.pools) {
         if (name.equals(pool.getName())) {
            this.pools.remove(pool);
            return pool;
         }
      }
      return null;
   }

   public void addPool(LootPool pool) {
      checkFrozen();
      if (pools.stream().anyMatch(e -> e == pool || e.getName() != null && e.getName().equals(pool.getName())))
         throw new RuntimeException("Attempted to add a duplicate pool to loot table: " + pool.getName());
      this.pools.add(pool);
   }
   //======================== FORGE END ===============================================

   public static class Builder implements FunctionUserBuilder<LootTable.Builder> {
      private final List<LootPool> pools = Lists.newArrayList();
      private final List<LootItemFunction> functions = Lists.newArrayList();
      private LootContextParamSet paramSet = LootTable.DEFAULT_PARAM_SET;

      public LootTable.Builder withPool(LootPool.Builder pLootPool) {
         this.pools.add(pLootPool.build());
         return this;
      }

      public LootTable.Builder setParamSet(LootContextParamSet pParameterSet) {
         this.paramSet = pParameterSet;
         return this;
      }

      public LootTable.Builder apply(LootItemFunction.Builder pFunctionBuilder) {
         this.functions.add(pFunctionBuilder.build());
         return this;
      }

      public LootTable.Builder unwrap() {
         return this;
      }

      public LootTable build() {
         return new LootTable(this.paramSet, this.pools.toArray(new LootPool[0]), this.functions.toArray(new LootItemFunction[0]));
      }
   }

   public static class Serializer implements JsonDeserializer<LootTable>, JsonSerializer<LootTable> {
      public LootTable deserialize(JsonElement p_79173_, Type p_79174_, JsonDeserializationContext p_79175_) throws JsonParseException {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(p_79173_, "loot table");
         LootPool[] alootpool = GsonHelper.getAsObject(jsonobject, "pools", new LootPool[0], p_79175_, LootPool[].class);
         LootContextParamSet lootcontextparamset = null;
         if (jsonobject.has("type")) {
            String s = GsonHelper.getAsString(jsonobject, "type");
            lootcontextparamset = LootContextParamSets.get(new ResourceLocation(s));
         }

         LootItemFunction[] alootitemfunction = GsonHelper.getAsObject(jsonobject, "functions", new LootItemFunction[0], p_79175_, LootItemFunction[].class);
         return new LootTable(lootcontextparamset != null ? lootcontextparamset : LootContextParamSets.ALL_PARAMS, alootpool, alootitemfunction);
      }

      public JsonElement serialize(LootTable p_79177_, Type p_79178_, JsonSerializationContext p_79179_) {
         JsonObject jsonobject = new JsonObject();
         if (p_79177_.paramSet != LootTable.DEFAULT_PARAM_SET) {
            ResourceLocation resourcelocation = LootContextParamSets.getKey(p_79177_.paramSet);
            if (resourcelocation != null) {
               jsonobject.addProperty("type", resourcelocation.toString());
            } else {
               LootTable.LOGGER.warn("Failed to find id for param set {}", (Object)p_79177_.paramSet);
            }
         }

         if (!p_79177_.pools.isEmpty()) {
            jsonobject.add("pools", p_79179_.serialize(p_79177_.pools));
         }

         if (!ArrayUtils.isEmpty((Object[])p_79177_.functions)) {
            jsonobject.add("functions", p_79179_.serialize(p_79177_.functions));
         }

         return jsonobject;
      }
   }
}
