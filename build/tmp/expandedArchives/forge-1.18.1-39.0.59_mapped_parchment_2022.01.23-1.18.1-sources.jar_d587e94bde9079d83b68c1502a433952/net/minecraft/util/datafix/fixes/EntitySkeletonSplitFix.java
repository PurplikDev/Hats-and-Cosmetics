package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import java.util.Objects;

public class EntitySkeletonSplitFix extends SimpleEntityRenameFix {
   public EntitySkeletonSplitFix(Schema p_15688_, boolean p_15689_) {
      super("EntitySkeletonSplitFix", p_15688_, p_15689_);
   }

   protected Pair<String, Dynamic<?>> getNewNameAndTag(String pName, Dynamic<?> pTag) {
      if (Objects.equals(pName, "Skeleton")) {
         int i = pTag.get("SkeletonType").asInt(0);
         if (i == 1) {
            pName = "WitherSkeleton";
         } else if (i == 2) {
            pName = "Stray";
         }
      }

      return Pair.of(pName, pTag);
   }
}