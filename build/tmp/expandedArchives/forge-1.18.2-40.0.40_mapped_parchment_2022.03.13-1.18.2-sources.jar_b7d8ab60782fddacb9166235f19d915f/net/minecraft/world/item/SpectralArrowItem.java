package net.minecraft.world.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.level.Level;

public class SpectralArrowItem extends ArrowItem {
   public SpectralArrowItem(Item.Properties p_43235_) {
      super(p_43235_);
   }

   public AbstractArrow createArrow(Level pLevel, ItemStack pStack, LivingEntity pShooter) {
      return new SpectralArrow(pLevel, pShooter);
   }
}