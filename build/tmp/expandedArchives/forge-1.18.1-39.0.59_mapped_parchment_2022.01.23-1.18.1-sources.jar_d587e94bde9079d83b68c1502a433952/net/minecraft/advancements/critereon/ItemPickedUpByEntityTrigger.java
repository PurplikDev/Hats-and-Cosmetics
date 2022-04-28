package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

public class ItemPickedUpByEntityTrigger extends SimpleCriterionTrigger<ItemPickedUpByEntityTrigger.TriggerInstance> {
   static final ResourceLocation ID = new ResourceLocation("thrown_item_picked_up_by_entity");

   public ResourceLocation getId() {
      return ID;
   }

   protected ItemPickedUpByEntityTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      ItemPredicate itempredicate = ItemPredicate.fromJson(pJson.get("item"));
      EntityPredicate.Composite entitypredicate$composite = EntityPredicate.Composite.fromJson(pJson, "entity", pConditionsParser);
      return new ItemPickedUpByEntityTrigger.TriggerInstance(pEntityPredicate, itempredicate, entitypredicate$composite);
   }

   public void trigger(ServerPlayer pPlayer, ItemStack pStack, Entity pEntity) {
      LootContext lootcontext = EntityPredicate.createContext(pPlayer, pEntity);
      this.trigger(pPlayer, (p_44371_) -> {
         return p_44371_.matches(pPlayer, pStack, lootcontext);
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final ItemPredicate item;
      private final EntityPredicate.Composite entity;

      public TriggerInstance(EntityPredicate.Composite pPlayer, ItemPredicate pItem, EntityPredicate.Composite pEntity) {
         super(ItemPickedUpByEntityTrigger.ID, pPlayer);
         this.item = pItem;
         this.entity = pEntity;
      }

      public static ItemPickedUpByEntityTrigger.TriggerInstance itemPickedUpByEntity(EntityPredicate.Composite pPlayer, ItemPredicate.Builder pItem, EntityPredicate.Composite pEntity) {
         return new ItemPickedUpByEntityTrigger.TriggerInstance(pPlayer, pItem.build(), pEntity);
      }

      public boolean matches(ServerPlayer pPlayer, ItemStack pStack, LootContext pContext) {
         if (!this.item.matches(pStack)) {
            return false;
         } else {
            return this.entity.matches(pContext);
         }
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("item", this.item.serializeToJson());
         jsonobject.add("entity", this.entity.toJson(pConditions));
         return jsonobject;
      }
   }
}