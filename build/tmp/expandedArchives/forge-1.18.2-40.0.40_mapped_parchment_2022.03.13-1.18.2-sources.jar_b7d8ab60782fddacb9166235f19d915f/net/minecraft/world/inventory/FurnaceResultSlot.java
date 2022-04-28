package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;

public class FurnaceResultSlot extends Slot {
   private final Player player;
   private int removeCount;

   public FurnaceResultSlot(Player p_39542_, Container p_39543_, int p_39544_, int p_39545_, int p_39546_) {
      super(p_39543_, p_39544_, p_39545_, p_39546_);
      this.player = p_39542_;
   }

   /**
    * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
    */
   public boolean mayPlace(ItemStack pStack) {
      return false;
   }

   /**
    * Decrease the size of the stack in slot (first int arg) by the amount of the second int arg. Returns the new stack.
    */
   public ItemStack remove(int pAmount) {
      if (this.hasItem()) {
         this.removeCount += Math.min(pAmount, this.getItem().getCount());
      }

      return super.remove(pAmount);
   }

   public void onTake(Player pPlayer, ItemStack pStack) {
      this.checkTakeAchievements(pStack);
      super.onTake(pPlayer, pStack);
   }

   /**
    * the itemStack passed in is the output - ie, iron ingots, and pickaxes, not ore and wood. Typically increases an
    * internal count then calls onCrafting(item).
    */
   protected void onQuickCraft(ItemStack pStack, int pAmount) {
      this.removeCount += pAmount;
      this.checkTakeAchievements(pStack);
   }

   /**
    * the itemStack passed in is the output - ie, iron ingots, and pickaxes, not ore and wood.
    */
   protected void checkTakeAchievements(ItemStack pStack) {
      pStack.onCraftedBy(this.player.level, this.player, this.removeCount);
      if (this.player instanceof ServerPlayer && this.container instanceof AbstractFurnaceBlockEntity) {
         ((AbstractFurnaceBlockEntity)this.container).awardUsedRecipesAndPopExperience((ServerPlayer)this.player);
      }

      this.removeCount = 0;
      net.minecraftforge.event.ForgeEventFactory.firePlayerSmeltedEvent(this.player, pStack);
   }
}
