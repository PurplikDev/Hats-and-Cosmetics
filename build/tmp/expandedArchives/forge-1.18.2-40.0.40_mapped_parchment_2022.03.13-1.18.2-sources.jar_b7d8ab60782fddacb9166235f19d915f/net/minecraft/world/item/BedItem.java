package net.minecraft.world.item;

import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BedItem extends BlockItem {
   public BedItem(Block p_40558_, Item.Properties p_40559_) {
      super(p_40558_, p_40559_);
   }

   protected boolean placeBlock(BlockPlaceContext pContext, BlockState pState) {
      return pContext.getLevel().setBlock(pContext.getClickedPos(), pState, 26);
   }
}