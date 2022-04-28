package net.minecraft.world.entity.ai.behavior;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;

public class LocateHidingPlaceDuringRaid extends LocateHidingPlace {
   public LocateHidingPlaceDuringRaid(int p_23427_, float p_23428_) {
      super(p_23427_, p_23428_, 1);
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      Raid raid = pLevel.getRaidAt(pOwner.blockPosition());
      return super.checkExtraStartConditions(pLevel, pOwner) && raid != null && raid.isActive() && !raid.isVictory() && !raid.isLoss();
   }
}