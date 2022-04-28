package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V501 extends Schema {
   public V501(int p_17974_, Schema p_17975_) {
      super(p_17974_, p_17975_);
   }

   protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> {
         return V100.equipment(pSchema);
      });
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_17983_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_17983_);
      registerMob(p_17983_, map, "PolarBear");
      return map;
   }
}