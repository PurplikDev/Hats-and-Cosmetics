package net.minecraft.world.effect;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;

public class AbsoptionMobEffect extends MobEffect {
   protected AbsoptionMobEffect(MobEffectCategory p_19414_, int p_19415_) {
      super(p_19414_, p_19415_);
   }

   public void removeAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
      pLivingEntity.setAbsorptionAmount(pLivingEntity.getAbsorptionAmount() - (float)(4 * (pAmplifier + 1)));
      super.removeAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
   }

   public void addAttributeModifiers(LivingEntity pLivingEntity, AttributeMap pAttributeMap, int pAmplifier) {
      pLivingEntity.setAbsorptionAmount(pLivingEntity.getAbsorptionAmount() + (float)(4 * (pAmplifier + 1)));
      super.addAttributeModifiers(pLivingEntity, pAttributeMap, pAmplifier);
   }
}