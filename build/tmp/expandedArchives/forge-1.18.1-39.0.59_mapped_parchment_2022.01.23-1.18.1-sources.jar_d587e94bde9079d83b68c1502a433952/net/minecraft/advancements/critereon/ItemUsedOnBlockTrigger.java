package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ItemUsedOnBlockTrigger extends SimpleCriterionTrigger<ItemUsedOnBlockTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("item_used_on_block");

   public ResourceLocation getId() {
      return ID;
   }

   public ItemUsedOnBlockTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      LocationPredicate locationpredicate = LocationPredicate.fromJson(pJson.get("location"));
      ItemPredicate itempredicate = ItemPredicate.fromJson(pJson.get("item"));
      return new ItemUsedOnBlockTrigger.TriggerInstance(pEntityPredicate, locationpredicate, itempredicate);
   }

   public void trigger(ServerPlayer pPlayer, BlockPos pPos, ItemStack pStack) {
      BlockState blockstate = pPlayer.getLevel().getBlockState(pPos);
      this.trigger(pPlayer, (p_45491_) -> {
         return p_45491_.matches(blockstate, pPlayer.getLevel(), pPos, pStack);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final LocationPredicate location;
      private final ItemPredicate item;

      public TriggerInstance(EntityPredicate.Composite pPlayer, LocationPredicate pLocation, ItemPredicate pItem) {
         super(ItemUsedOnBlockTrigger.ID, pPlayer);
         this.location = pLocation;
         this.item = pItem;
      }

      public static ItemUsedOnBlockTrigger.TriggerInstance itemUsedOnBlock(LocationPredicate.Builder pLocationBuilder, ItemPredicate.Builder pStackBuilder) {
         return new ItemUsedOnBlockTrigger.TriggerInstance(EntityPredicate.Composite.ANY, pLocationBuilder.build(), pStackBuilder.build());
      }

      public boolean matches(BlockState pState, ServerLevel pLevel, BlockPos pPos, ItemStack pStack) {
         return !this.location.matches(pLevel, (double)pPos.getX() + 0.5D, (double)pPos.getY() + 0.5D, (double)pPos.getZ() + 0.5D) ? false : this.item.matches(pStack);
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("location", this.location.serializeToJson());
         jsonobject.add("item", this.item.serializeToJson());
         return jsonobject;
      }
   }
}