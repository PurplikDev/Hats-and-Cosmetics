package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;

public class LighthingBoltPredicate {
   public static final LighthingBoltPredicate ANY = new LighthingBoltPredicate(MinMaxBounds.Ints.ANY, EntityPredicate.ANY);
   private static final String BLOCKS_SET_ON_FIRE_KEY = "blocks_set_on_fire";
   private static final String ENTITY_STRUCK_KEY = "entity_struck";
   private final MinMaxBounds.Ints blocksSetOnFire;
   private final EntityPredicate entityStruck;

   private LighthingBoltPredicate(MinMaxBounds.Ints pBlocksSetOnFire, EntityPredicate pEntityStruck) {
      this.blocksSetOnFire = pBlocksSetOnFire;
      this.entityStruck = pEntityStruck;
   }

   public static LighthingBoltPredicate blockSetOnFire(MinMaxBounds.Ints pBlocksSetOnFire) {
      return new LighthingBoltPredicate(pBlocksSetOnFire, EntityPredicate.ANY);
   }

   public static LighthingBoltPredicate fromJson(@Nullable JsonElement pJson) {
      if (pJson != null && !pJson.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(pJson, "lightning");
         return new LighthingBoltPredicate(MinMaxBounds.Ints.fromJson(jsonobject.get("blocks_set_on_fire")), EntityPredicate.fromJson(jsonobject.get("entity_struck")));
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonobject = new JsonObject();
         jsonobject.add("blocks_set_on_fire", this.blocksSetOnFire.serializeToJson());
         jsonobject.add("entity_struck", this.entityStruck.serializeToJson());
         return jsonobject;
      }
   }

   public boolean matches(Entity pEntity, ServerLevel pLevel, @Nullable Vec3 pPosition) {
      if (this == ANY) {
         return true;
      } else if (!(pEntity instanceof LightningBolt)) {
         return false;
      } else {
         LightningBolt lightningbolt = (LightningBolt)pEntity;
         return this.blocksSetOnFire.matches(lightningbolt.getBlocksSetOnFire()) && (this.entityStruck == EntityPredicate.ANY || lightningbolt.getHitEntities().anyMatch((p_153245_) -> {
            return this.entityStruck.matches(pLevel, pPosition, p_153245_);
         }));
      }
   }
}