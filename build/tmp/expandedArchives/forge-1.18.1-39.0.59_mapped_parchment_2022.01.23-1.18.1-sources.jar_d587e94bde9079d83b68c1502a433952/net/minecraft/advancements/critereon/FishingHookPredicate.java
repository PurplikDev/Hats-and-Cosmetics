package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import javax.annotation.Nullable;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;

public class FishingHookPredicate {
   public static final FishingHookPredicate ANY = new FishingHookPredicate(false);
   private static final String IN_OPEN_WATER_KEY = "in_open_water";
   private final boolean inOpenWater;

   private FishingHookPredicate(boolean pInOpenWater) {
      this.inOpenWater = pInOpenWater;
   }

   public static FishingHookPredicate inOpenWater(boolean pInOpenWater) {
      return new FishingHookPredicate(pInOpenWater);
   }

   public static FishingHookPredicate fromJson(@Nullable JsonElement pJson) {
      if (pJson != null && !pJson.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(pJson, "fishing_hook");
         JsonElement jsonelement = jsonobject.get("in_open_water");
         return jsonelement != null ? new FishingHookPredicate(GsonHelper.convertToBoolean(jsonelement, "in_open_water")) : ANY;
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonobject = new JsonObject();
         jsonobject.add("in_open_water", new JsonPrimitive(this.inOpenWater));
         return jsonobject;
      }
   }

   public boolean matches(Entity pEntity) {
      if (this == ANY) {
         return true;
      } else if (!(pEntity instanceof FishingHook)) {
         return false;
      } else {
         FishingHook fishinghook = (FishingHook)pEntity;
         return this.inOpenWater == fishinghook.isOpenWaterFishing();
      }
   }
}