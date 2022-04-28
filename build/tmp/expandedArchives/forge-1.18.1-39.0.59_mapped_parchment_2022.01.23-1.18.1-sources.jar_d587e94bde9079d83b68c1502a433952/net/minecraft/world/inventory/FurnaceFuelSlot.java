package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class FurnaceFuelSlot extends Slot {
   private final AbstractFurnaceMenu menu;

   public FurnaceFuelSlot(AbstractFurnaceMenu p_39520_, Container p_39521_, int p_39522_, int p_39523_, int p_39524_) {
      super(p_39521_, p_39522_, p_39523_, p_39524_);
      this.menu = p_39520_;
   }

   /**
    * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
    */
   public boolean mayPlace(ItemStack pStack) {
      return this.menu.isFuel(pStack) || isBucket(pStack);
   }

   public int getMaxStackSize(ItemStack pStack) {
      return isBucket(pStack) ? 1 : super.getMaxStackSize(pStack);
   }

   public static boolean isBucket(ItemStack pStack) {
      return pStack.is(Items.BUCKET);
   }
}