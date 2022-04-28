package net.minecraft.world.inventory;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public class ResultSlot extends Slot {
   private final CraftingContainer craftSlots;
   private final Player player;
   private int removeCount;

   public ResultSlot(Player p_40166_, CraftingContainer p_40167_, Container p_40168_, int p_40169_, int p_40170_, int p_40171_) {
      super(p_40168_, p_40169_, p_40170_, p_40171_);
      this.player = p_40166_;
      this.craftSlots = p_40167_;
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

   /**
    * the itemStack passed in is the output - ie, iron ingots, and pickaxes, not ore and wood. Typically increases an
    * internal count then calls onCrafting(item).
    */
   protected void onQuickCraft(ItemStack pStack, int pAmount) {
      this.removeCount += pAmount;
      this.checkTakeAchievements(pStack);
   }

   protected void onSwapCraft(int pNumItemsCrafted) {
      this.removeCount += pNumItemsCrafted;
   }

   /**
    * the itemStack passed in is the output - ie, iron ingots, and pickaxes, not ore and wood.
    */
   protected void checkTakeAchievements(ItemStack pStack) {
      if (this.removeCount > 0) {
         pStack.onCraftedBy(this.player.level, this.player, this.removeCount);
         net.minecraftforge.event.ForgeEventFactory.firePlayerCraftingEvent(this.player, pStack, this.craftSlots);
      }

      if (this.container instanceof RecipeHolder) {
         ((RecipeHolder)this.container).awardUsedRecipes(this.player);
      }

      this.removeCount = 0;
   }

   public void onTake(Player pPlayer, ItemStack pStack) {
      this.checkTakeAchievements(pStack);
      net.minecraftforge.common.ForgeHooks.setCraftingPlayer(pPlayer);
      NonNullList<ItemStack> nonnulllist = pPlayer.level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, this.craftSlots, pPlayer.level);
      net.minecraftforge.common.ForgeHooks.setCraftingPlayer(null);
      for(int i = 0; i < nonnulllist.size(); ++i) {
         ItemStack itemstack = this.craftSlots.getItem(i);
         ItemStack itemstack1 = nonnulllist.get(i);
         if (!itemstack.isEmpty()) {
            this.craftSlots.removeItem(i, 1);
            itemstack = this.craftSlots.getItem(i);
         }

         if (!itemstack1.isEmpty()) {
            if (itemstack.isEmpty()) {
               this.craftSlots.setItem(i, itemstack1);
            } else if (ItemStack.isSame(itemstack, itemstack1) && ItemStack.tagMatches(itemstack, itemstack1)) {
               itemstack1.grow(itemstack.getCount());
               this.craftSlots.setItem(i, itemstack1);
            } else if (!this.player.getInventory().add(itemstack1)) {
               this.player.drop(itemstack1, false);
            }
         }
      }

   }
}
