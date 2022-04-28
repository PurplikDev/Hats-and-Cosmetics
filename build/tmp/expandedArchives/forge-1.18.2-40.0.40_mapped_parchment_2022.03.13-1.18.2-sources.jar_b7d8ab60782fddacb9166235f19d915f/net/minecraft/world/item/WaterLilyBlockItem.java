package net.minecraft.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

public class WaterLilyBlockItem extends BlockItem {
   public WaterLilyBlockItem(Block p_43436_, Item.Properties p_43437_) {
      super(p_43436_, p_43437_);
   }

   /**
    * Called when this item is used when targetting a Block
    */
   public InteractionResult useOn(UseOnContext pContext) {
      return InteractionResult.PASS;
   }

   /**
    * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see
    * {@link #onItemUse}.
    */
   public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
      BlockHitResult blockhitresult = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY);
      BlockHitResult blockhitresult1 = blockhitresult.withPosition(blockhitresult.getBlockPos().above());
      InteractionResult interactionresult = super.useOn(new UseOnContext(pPlayer, pHand, blockhitresult1));
      return new InteractionResultHolder<>(interactionresult, pPlayer.getItemInHand(pHand));
   }
}