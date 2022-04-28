package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;

public class ItemFrameItem extends HangingEntityItem {
   public ItemFrameItem(EntityType<? extends HangingEntity> p_150904_, Item.Properties p_150905_) {
      super(p_150904_, p_150905_);
   }

   protected boolean mayPlace(Player pPlayer, Direction pDirection, ItemStack pItemStack, BlockPos pPos) {
      return !pPlayer.level.isOutsideBuildHeight(pPos) && pPlayer.mayUseItemAt(pPos, pDirection, pItemStack);
   }
}