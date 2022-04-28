package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class LocationTrigger extends SimpleCriterionTrigger<LocationTrigger.TriggerInstance> {
   final ResourceLocation id;

   public LocationTrigger(ResourceLocation pId) {
      this.id = pId;
   }

   public ResourceLocation getId() {
      return this.id;
   }

   public LocationTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "location", pJson);
      LocationPredicate locationpredicate = LocationPredicate.fromJson(jsonobject);
      return new LocationTrigger.TriggerInstance(this.id, pEntityPredicate, locationpredicate);
   }

   public void trigger(ServerPlayer pPlayer) {
      this.trigger(pPlayer, (p_53649_) -> {
         return p_53649_.matches(pPlayer.getLevel(), pPlayer.getX(), pPlayer.getY(), pPlayer.getZ());
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      private final LocationPredicate location;

      public TriggerInstance(ResourceLocation pCriterion, EntityPredicate.Composite pPlayer, LocationPredicate pLocation) {
         super(pCriterion, pPlayer);
         this.location = pLocation;
      }

      public static LocationTrigger.TriggerInstance located(LocationPredicate pLocation) {
         return new LocationTrigger.TriggerInstance(CriteriaTriggers.LOCATION.id, EntityPredicate.Composite.ANY, pLocation);
      }

      public static LocationTrigger.TriggerInstance located(EntityPredicate pEntityPredicate) {
         return new LocationTrigger.TriggerInstance(CriteriaTriggers.LOCATION.id, EntityPredicate.Composite.wrap(pEntityPredicate), LocationPredicate.ANY);
      }

      public static LocationTrigger.TriggerInstance sleptInBed() {
         return new LocationTrigger.TriggerInstance(CriteriaTriggers.SLEPT_IN_BED.id, EntityPredicate.Composite.ANY, LocationPredicate.ANY);
      }

      public static LocationTrigger.TriggerInstance raidWon() {
         return new LocationTrigger.TriggerInstance(CriteriaTriggers.RAID_WIN.id, EntityPredicate.Composite.ANY, LocationPredicate.ANY);
      }

      public static LocationTrigger.TriggerInstance walkOnBlockWithEquipment(Block pBlock, Item pItem) {
         return located(EntityPredicate.Builder.entity().equipment(EntityEquipmentPredicate.Builder.equipment().feet(ItemPredicate.Builder.item().of(pItem).build()).build()).steppingOn(LocationPredicate.Builder.location().setBlock(BlockPredicate.Builder.block().of(pBlock).build()).build()).build());
      }

      public boolean matches(ServerLevel pLevel, double pX, double pY, double pZ) {
         return this.location.matches(pLevel, pX, pY, pZ);
      }

      public JsonObject serializeToJson(SerializationContext pConditions) {
         JsonObject jsonobject = super.serializeToJson(pConditions);
         jsonobject.add("location", this.location.serializeToJson());
         return jsonobject;
      }
   }
}