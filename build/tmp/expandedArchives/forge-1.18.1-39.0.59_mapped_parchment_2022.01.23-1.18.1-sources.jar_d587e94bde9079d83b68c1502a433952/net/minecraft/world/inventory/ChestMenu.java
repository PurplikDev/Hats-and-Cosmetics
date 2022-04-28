package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChestMenu extends AbstractContainerMenu {
   private static final int SLOTS_PER_ROW = 9;
   private final Container container;
   private final int containerRows;

   private ChestMenu(MenuType<?> p_39224_, int p_39225_, Inventory p_39226_, int p_39227_) {
      this(p_39224_, p_39225_, p_39226_, new SimpleContainer(9 * p_39227_), p_39227_);
   }

   public static ChestMenu oneRow(int pId, Inventory pPlayer) {
      return new ChestMenu(MenuType.GENERIC_9x1, pId, pPlayer, 1);
   }

   public static ChestMenu twoRows(int pId, Inventory pPlayer) {
      return new ChestMenu(MenuType.GENERIC_9x2, pId, pPlayer, 2);
   }

   public static ChestMenu threeRows(int pId, Inventory pPlayer) {
      return new ChestMenu(MenuType.GENERIC_9x3, pId, pPlayer, 3);
   }

   public static ChestMenu fourRows(int pId, Inventory pPlayer) {
      return new ChestMenu(MenuType.GENERIC_9x4, pId, pPlayer, 4);
   }

   public static ChestMenu fiveRows(int pId, Inventory pPlayer) {
      return new ChestMenu(MenuType.GENERIC_9x5, pId, pPlayer, 5);
   }

   public static ChestMenu sixRows(int pId, Inventory pPlayer) {
      return new ChestMenu(MenuType.GENERIC_9x6, pId, pPlayer, 6);
   }

   public static ChestMenu threeRows(int pId, Inventory pPlayer, Container pBlockEntity) {
      return new ChestMenu(MenuType.GENERIC_9x3, pId, pPlayer, pBlockEntity, 3);
   }

   public static ChestMenu sixRows(int pId, Inventory pPlayer, Container pBlockEntity) {
      return new ChestMenu(MenuType.GENERIC_9x6, pId, pPlayer, pBlockEntity, 6);
   }

   public ChestMenu(MenuType<?> p_39229_, int p_39230_, Inventory p_39231_, Container p_39232_, int p_39233_) {
      super(p_39229_, p_39230_);
      checkContainerSize(p_39232_, p_39233_ * 9);
      this.container = p_39232_;
      this.containerRows = p_39233_;
      p_39232_.startOpen(p_39231_.player);
      int i = (this.containerRows - 4) * 18;

      for(int j = 0; j < this.containerRows; ++j) {
         for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(p_39232_, k + j * 9, 8 + k * 18, 18 + j * 18));
         }
      }

      for(int l = 0; l < 3; ++l) {
         for(int j1 = 0; j1 < 9; ++j1) {
            this.addSlot(new Slot(p_39231_, j1 + l * 9 + 9, 8 + j1 * 18, 103 + l * 18 + i));
         }
      }

      for(int i1 = 0; i1 < 9; ++i1) {
         this.addSlot(new Slot(p_39231_, i1, 8 + i1 * 18, 161 + i));
      }

   }

   /**
    * Determines whether supplied player can use this container
    */
   public boolean stillValid(Player pPlayer) {
      return this.container.stillValid(pPlayer);
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
         if (pIndex < this.containerRows * 9) {
            if (!this.moveItemStackTo(itemstack1, this.containerRows * 9, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.moveItemStackTo(itemstack1, 0, this.containerRows * 9, false)) {
            return ItemStack.EMPTY;
         }

         if (itemstack1.isEmpty()) {
            slot.set(ItemStack.EMPTY);
         } else {
            slot.setChanged();
         }
      }

      return itemstack;
   }

   /**
    * Called when the container is closed.
    */
   public void removed(Player pPlayer) {
      super.removed(pPlayer);
      this.container.stopOpen(pPlayer);
   }

   /**
    * Gets the inventory associated with this chest container.
    * 
    * @see #field_75155_e
    */
   public Container getContainer() {
      return this.container;
   }

   public int getRowCount() {
      return this.containerRows;
   }
}