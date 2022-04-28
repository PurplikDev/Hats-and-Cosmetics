package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public class ShulkerBoxSlot extends Slot {
   public ShulkerBoxSlot(Container p_40202_, int p_40203_, int p_40204_, int p_40205_) {
      super(p_40202_, p_40203_, p_40204_, p_40205_);
   }

   /**
    * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
    */
   public boolean mayPlace(ItemStack pStack) {
      return pStack.getItem().canFitInsideContainerItems();
   }
}