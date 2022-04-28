package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;

public class V701 extends Schema {
   public V701(int p_17996_, Schema p_17997_) {
      super(p_17996_, p_17997_);
   }

   protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> {
         return V100.equipment(pSchema);
      });
   }

   public Map<String, Supplier<TypeTemplate>> registerEntities(Schema p_18005_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerEntities(p_18005_);
      registerMob(p_18005_, map, "WitherSkeleton");
      registerMob(p_18005_, map, "Stray");
      return map;
   }
}