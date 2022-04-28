package net.minecraft.world.item;

import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.player.Player;

public class SaddleItem extends Item {
   public SaddleItem(Item.Properties p_43053_) {
      super(p_43053_);
   }

   /**
    * Returns true if the item can be used on the given entity, e.g. shears on sheep.
    */
   public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pTarget, InteractionHand pHand) {
      if (pTarget instanceof Saddleable && pTarget.isAlive()) {
         Saddleable saddleable = (Saddleable)pTarget;
         if (!saddleable.isSaddled() && saddleable.isSaddleable()) {
            if (!pPlayer.level.isClientSide) {
               saddleable.equipSaddle(SoundSource.NEUTRAL);
               pStack.shrink(1);
            }

            return InteractionResult.sidedSuccess(pPlayer.level.isClientSide);
         }
      }

      return InteractionResult.PASS;
   }
}