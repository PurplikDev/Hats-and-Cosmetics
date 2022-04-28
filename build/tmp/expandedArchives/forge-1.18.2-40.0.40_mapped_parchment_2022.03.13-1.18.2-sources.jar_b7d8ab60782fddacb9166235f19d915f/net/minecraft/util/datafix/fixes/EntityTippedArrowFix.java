package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import java.util.Objects;

public class EntityTippedArrowFix extends SimplestEntityRenameFix {
   public EntityTippedArrowFix(Schema p_15711_, boolean p_15712_) {
      super("EntityTippedArrowFix", p_15711_, p_15712_);
   }

   protected String rename(String pName) {
      return Objects.equals(pName, "TippedArrow") ? "Arrow" : pName;
   }
}