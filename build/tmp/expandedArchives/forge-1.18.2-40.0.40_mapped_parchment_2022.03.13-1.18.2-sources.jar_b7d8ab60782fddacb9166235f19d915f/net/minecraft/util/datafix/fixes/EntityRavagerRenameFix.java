package net.minecraft.util.datafix.fixes;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.schemas.Schema;
import java.util.Map;
import java.util.Objects;

public class EntityRavagerRenameFix extends SimplestEntityRenameFix {
   public static final Map<String, String> RENAMED_IDS = ImmutableMap.<String, String>builder().put("minecraft:illager_beast_spawn_egg", "minecraft:ravager_spawn_egg").build();

   public EntityRavagerRenameFix(Schema p_15594_, boolean p_15595_) {
      super("EntityRavagerRenameFix", p_15594_, p_15595_);
   }

   protected String rename(String pName) {
      return Objects.equals("minecraft:illager_beast", pName) ? "minecraft:ravager" : pName;
   }
}