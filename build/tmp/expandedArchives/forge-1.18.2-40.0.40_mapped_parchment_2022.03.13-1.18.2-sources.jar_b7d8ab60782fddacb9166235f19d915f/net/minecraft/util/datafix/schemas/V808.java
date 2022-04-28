package net.minecraft.util.datafix.schemas;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;

public class V808 extends NamespacedSchema {
   public V808(int p_18170_, Schema p_18171_) {
      super(p_18170_, p_18171_);
   }

   protected static void registerInventory(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
      pSchema.register(pMap, pName, () -> {
         return DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema)));
      });
   }

   public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema p_18179_) {
      Map<String, Supplier<TypeTemplate>> map = super.registerBlockEntities(p_18179_);
      registerInventory(p_18179_, map, "minecraft:shulker_box");
      return map;
   }
}