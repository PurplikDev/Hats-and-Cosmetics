package net.minecraft.world.effect;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttackDamageMobEffect extends MobEffect {
   protected final double multiplier;

   protected AttackDamageMobEffect(MobEffectCategory p_19426_, int p_19427_, double p_19428_) {
      super(p_19426_, p_19427_);
      this.multiplier = p_19428_;
   }

   public double getAttributeModifierValue(int pAmplifier, AttributeModifier pModifier) {
      return this.multiplier * (double)(pAmplifier + 1);
   }
}