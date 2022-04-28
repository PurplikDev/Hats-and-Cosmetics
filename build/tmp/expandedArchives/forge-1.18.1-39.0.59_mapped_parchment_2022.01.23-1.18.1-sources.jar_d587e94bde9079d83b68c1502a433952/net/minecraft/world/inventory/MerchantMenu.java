package net.minecraft.world.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.ClientSideMerchant;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantMenu extends AbstractContainerMenu {
   protected static final int PAYMENT1_SLOT = 0;
   protected static final int PAYMENT2_SLOT = 1;
   protected static final int RESULT_SLOT = 2;
   private static final int INV_SLOT_START = 3;
   private static final int INV_SLOT_END = 30;
   private static final int USE_ROW_SLOT_START = 30;
   private static final int USE_ROW_SLOT_END = 39;
   private static final int SELLSLOT1_X = 136;
   private static final int SELLSLOT2_X = 162;
   private static final int BUYSLOT_X = 220;
   private static final int ROW_Y = 37;
   private final Merchant trader;
   private final MerchantContainer tradeContainer;
   private int merchantLevel;
   private boolean showProgressBar;
   private boolean canRestock;

   public MerchantMenu(int p_40033_, Inventory p_40034_) {
      this(p_40033_, p_40034_, new ClientSideMerchant(p_40034_.player));
   }

   public MerchantMenu(int p_40036_, Inventory p_40037_, Merchant p_40038_) {
      super(MenuType.MERCHANT, p_40036_);
      this.trader = p_40038_;
      this.tradeContainer = new MerchantContainer(p_40038_);
      this.addSlot(new Slot(this.tradeContainer, 0, 136, 37));
      this.addSlot(new Slot(this.tradeContainer, 1, 162, 37));
      this.addSlot(new MerchantResultSlot(p_40037_.player, p_40038_, this.tradeContainer, 2, 220, 37));

      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(p_40037_, j + i * 9 + 9, 108 + j * 18, 84 + i * 18));
         }
      }

      for(int k = 0; k < 9; ++k) {
         this.addSlot(new Slot(p_40037_, k, 108 + k * 18, 142));
      }

   }

   public void setShowProgressBar(boolean p_40049_) {
      this.showProgressBar = p_40049_;
   }

   /**
    * Callback for when the crafting matrix is changed.
    */
   public void slotsChanged(Container pInventory) {
      this.tradeContainer.updateSellItem();
      super.slotsChanged(pInventory);
   }

   public void setSelectionHint(int pCurrentRecipeIndex) {
      this.tradeContainer.setSelectionHint(pCurrentRecipeIndex);
   }

   /**
    * Determines whether supplied player can use this container
    */
   public boolean stillValid(Player pPlayer) {
      return this.trader.getTradingPlayer() == pPlayer;
   }

   public int getTraderXp() {
      return this.trader.getVillagerXp();
   }

   public int getFutureTraderXp() {
      return this.tradeContainer.getFutureXp();
   }

   public void setXp(int pXp) {
      this.trader.overrideXp(pXp);
   }

   public int getTraderLevel() {
      return this.merchantLevel;
   }

   public void setMerchantLevel(int pLevel) {
      this.merchantLevel = pLevel;
   }

   public void setCanRestock(boolean p_40059_) {
      this.canRestock = p_40059_;
   }

   public boolean canRestock() {
      return this.canRestock;
   }

   /**
    * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
    * null for the initial slot that was double-clicked.
    */
   public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
      return false;
   }

   /**
    * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
    * inventory and the other inventory(s).
    */
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      ItemStack itemstack = ItemStack.EMPTY;
      Slot slot = this.slots.get(pIndex);
      if (slot != null && slot.hasItem()) {
         ItemStack itemstack1 = slot.getItem();
         itemstack = itemstack1.copy();
         if (pIndex == 2) {
            if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
            this.playTradeSound();
         } else if (pIndex != 0 && pIndex != 1) {
            if (pIndex >= 3 && pIndex < 30) {
               if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (pIndex >= 30 && pIndex < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }

         if (itemstack1.getCount() == itemstack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTake(pPlayer, itemstack1);
      }

      return itemstack;
   }

   private void playTradeSound() {
      if (!this.trader.isClientSide()) {
         Entity entity = (Entity)this.trader;
         entity.getLevel().playLocalSound(entity.getX(), entity.getY(), entity.getZ(), this.trader.getNotifyTradeSound(), SoundSource.NEUTRAL, 1.0F, 1.0F, false);
      }

   }

   /**
    * Called when the container is closed.
    */
   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.trader.setTradingPlayer((Player)null);
      if (!this.trader.isClientSide()) {
         if (!pPlayer.isAlive() || pPlayer instanceof ServerPlayer && ((ServerPlayer)pPlayer).hasDisconnected()) {
            ItemStack itemstack = this.tradeContainer.removeItemNoUpdate(0);
            if (!itemstack.isEmpty()) {
               pPlayer.drop(itemstack, false);
            }

            itemstack = this.tradeContainer.removeItemNoUpdate(1);
            if (!itemstack.isEmpty()) {
               pPlayer.drop(itemstack, false);
            }
         } else if (pPlayer instanceof ServerPlayer) {
            pPlayer.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(0));
            pPlayer.getInventory().placeItemBackInInventory(this.tradeContainer.removeItemNoUpdate(1));
         }

      }
   }

   public void tryMoveItems(int p_40073_) {
      if (this.getOffers().size() > p_40073_) {
         ItemStack itemstack = this.tradeContainer.getItem(0);
         if (!itemstack.isEmpty()) {
            if (!this.moveItemStackTo(itemstack, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(0, itemstack);
         }

         ItemStack itemstack1 = this.tradeContainer.getItem(1);
         if (!itemstack1.isEmpty()) {
            if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
               return;
            }

            this.tradeContainer.setItem(1, itemstack1);
         }

         if (this.tradeContainer.getItem(0).isEmpty() && this.tradeContainer.getItem(1).isEmpty()) {
            ItemStack itemstack2 = this.getOffers().get(p_40073_).getCostA();
            this.moveFromInventoryToPaymentSlot(0, itemstack2);
            ItemStack itemstack3 = this.getOffers().get(p_40073_).getCostB();
            this.moveFromInventoryToPaymentSlot(1, itemstack3);
         }

      }
   }

   private void moveFromInventoryToPaymentSlot(int p_40061_, ItemStack p_40062_) {
      if (!p_40062_.isEmpty()) {
         for(int i = 3; i < 39; ++i) {
            ItemStack itemstack = this.slots.get(i).getItem();
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(p_40062_, itemstack)) {
               ItemStack itemstack1 = this.tradeContainer.getItem(p_40061_);
               int j = itemstack1.isEmpty() ? 0 : itemstack1.getCount();
               int k = Math.min(p_40062_.getMaxStackSize() - j, itemstack.getCount());
               ItemStack itemstack2 = itemstack.copy();
               int l = j + k;
               itemstack.shrink(k);
               itemstack2.setCount(l);
               this.tradeContainer.setItem(p_40061_, itemstack2);
               if (l >= p_40062_.getMaxStackSize()) {
                  break;
               }
            }
         }
      }

   }

   /**
    * net.minecraft.client.network.play.ClientPlayNetHandler uses this to set offers for the client side
    * MerchantContainer
    */
   public void setOffers(MerchantOffers pOffers) {
      this.trader.overrideOffers(pOffers);
   }

   public MerchantOffers getOffers() {
      return this.trader.getOffers();
   }

   public boolean showProgressBar() {
      return this.showProgressBar;
   }
}