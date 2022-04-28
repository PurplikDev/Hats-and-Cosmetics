package net.minecraft.advancements.critereon;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class TickTrigger extends SimpleCriterionTrigger<TickTrigger.TriggerInstance> {
   public static final ResourceLocation ID = new ResourceLocation("tick");

   public ResourceLocation getId() {
      return ID;
   }

   public TickTrigger.TriggerInstance createInstance(JsonObject pJson, EntityPredicate.Composite pEntityPredicate, DeserializationContext pConditionsParser) {
      return new TickTrigger.TriggerInstance(pEntityPredicate);
   }

   public void trigger(ServerPlayer pPlayer) {
      this.trigger(pPlayer, (p_70648_) -> {
         return true;
      });
   }

   public static class TriggerInstance extends AbstractCriterionTriggerInstance {
      public TriggerInstance(EntityPredicate.Composite pPlayer) {
         super(TickTrigger.ID, pPlayer);
      }
   }
}