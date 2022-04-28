package net.minecraft.world.inventory;

import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;

public class MerchantResultSlot extends Slot {
   private final MerchantContainer slots;
   private final Player player;
   private int removeCount;
   private final Merchant merchant;

   public MerchantResultSlot(Player p_40083_, Merchant p_40084_, MerchantContainer p_40085_, int p_40086_, int p_40087_, int p_40088_) {
      super(p_40085_, p_40086_, p_40087_, p_40088_);
      this.player = p_40083_;
      this.merchant = p_40084_;
      this.slots = p_40085_;
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

   /**
    * the itemStack passed in is the output - ie, iron ingots, and pickaxes, not ore and wood.
    */
   protected void checkTakeAchievements(ItemStack pStack) {
      pStack.onCraftedBy(this.player.level, this.player, this.removeCount);
      this.removeCount = 0;
   }

   public void onTake(Player pPlayer, ItemStack pStack) {
      this.checkTakeAchievements(pStack);
      MerchantOffer merchantoffer = this.slots.getActiveOffer();
      if (merchantoffer != null) {
         ItemStack itemstack = this.slots.getItem(0);
         ItemStack itemstack1 = this.slots.getItem(1);
         if (merchantoffer.take(itemstack, itemstack1) || merchantoffer.take(itemstack1, itemstack)) {
            this.merchant.notifyTrade(merchantoffer);
            pPlayer.awardStat(Stats.TRADED_WITH_VILLAGER);
            this.slots.setItem(0, itemstack);
            this.slots.setItem(1, itemstack1);
         }

         this.merchant.overrideXp(this.merchant.getVillagerXp() + merchantoffer.getXp());
      }

   }
}