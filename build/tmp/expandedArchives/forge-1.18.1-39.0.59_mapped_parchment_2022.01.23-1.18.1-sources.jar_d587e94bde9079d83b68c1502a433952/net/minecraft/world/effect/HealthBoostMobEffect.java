package net.minecraft.world.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class HealthBoostMobEffect extends MobEffect {
   public HealthBoostMobEffect(MobEffectCategory p_19433_, int p_19434_) {
      super(p_19433_, p_19434_);
   }

   public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
      super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
      if (pLivingEntity.getHealth() > pLivingEntity.getMaxHealth()) {
         pLivingEntity.setHealth(pLivingEntity.getMaxHealth());
      }

   }
}