package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

public class GoOutsideToCelebrate extends MoveToSkySeeingSpot {
   public GoOutsideToCelebrate(float p_23050_) {
      super(p_23050_);
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      Raid raid = pLevel.getRaidAt(pOwner.blockPosition());
      return raid != null && raid.isVictory() && super.checkExtraStartConditions(pLevel, pOwner);
   }
}