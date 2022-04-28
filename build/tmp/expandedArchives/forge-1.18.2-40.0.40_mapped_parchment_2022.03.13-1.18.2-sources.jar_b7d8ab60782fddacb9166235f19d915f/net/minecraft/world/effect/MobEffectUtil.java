package net.minecraft.world.effect;

import net.minecraft.util.Mth;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;

public final class MobEffectUtil {
   public static String formatDuration(MobEffectInstance pEffect, float pDurationFactor) {
      if (pEffect.isNoCounter()) {
         return "**:**";
      } else {
         int i = Mth.floor((float)pEffect.getDuration() * pDurationFactor);
         return StringUtil.formatTickDuration(i);
      }
   }

   public static boolean hasDigSpeed(LivingEntity pEntity) {
      return pEntity.hasEffect(MobEffects.DIG_SPEED) || pEntity.hasEffect(MobEffects.CONDUIT_POWER);
   }

   public static int getDigSpeedAmplification(LivingEntity pEntity) {
      int i = 0;
      int j = 0;
      if (pEntity.hasEffect(MobEffects.DIG_SPEED)) {
         i = pEntity.getEffect(MobEffects.DIG_SPEED).getAmplifier();
      }

      if (pEntity.hasEffect(MobEffects.CONDUIT_POWER)) {
         j = pEntity.getEffect(MobEffects.CONDUIT_POWER).getAmplifier();
      }

      return Math.max(i, j);
   }

   public static boolean hasWaterBreathing(LivingEntity pEntity) {
      return pEntity.hasEffect(MobEffects.WATER_BREATHING) || pEntity.hasEffect(MobEffects.CONDUIT_POWER);
   }
}