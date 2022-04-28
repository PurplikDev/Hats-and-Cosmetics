package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class HorseInventoryMenu extends AbstractContainerMenu {
   private final Container horseContainer;
   private final AbstractHorse horse;

   public HorseInventoryMenu(int p_39656_, Inventory p_39657_, Container p_39658_, final AbstractHorse p_39659_) {
      super((MenuType<?>)null, p_39656_);
      this.horseContainer = p_39658_;
      this.horse = p_39659_;
      int i = 3;
      p_39658_.startOpen(p_39657_.player);
      int j = -18;
      this.addSlot(new Slot(p_39658_, 0, 8, 18) {
         /**
          * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
          */
         public boolean mayPlace(ItemStack p_39677_) {
            return p_39677_.is(Items.SADDLE) && !this.hasItem() && p_39659_.isSaddleable();
         }

         /**
          * Actualy only call when we want to render the white square effect over the slots. Return always True, except
          * for the armor slot of the Donkey/Mule (we can't interact with the Undead and Skeleton horses)
          */
         public boolean isActive() {
            return p_39659_.isSaddleable();
         }
      });
      this.addSlot(new Slot(p_39658_, 1, 8, 36) {
         /**
          * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
          */
         public boolean mayPlace(ItemStack p_39690_) {
            return p_39659_.isArmor(p_39690_);
         }

         /**
          * Actualy only call when we want to render the white square effect over the slots. Return always True, except
          * for the armor slot of the Donkey/Mule (we can't interact with the Undead and Skeleton horses)
          */
         public boolean isActive() {
            return p_39659_.canWearArmor();
         }

         /**
          * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in the
          * case of armor slots)
          */
         public int getMaxStackSize() {
            return 1;
         }
      });
      if (this.hasChest(p_39659_)) {
         for(int k = 0; k < 3; ++k) {
            for(int l = 0; l < ((AbstractChestedHorse)p_39659_).getInventoryColumns(); ++l) {
               this.addSlot(new Slot(p_39658_, 2 + l + k * ((AbstractChestedHorse)p_39659_).getInventoryColumns(), 80 + l * 18, 18 + k * 18));
            }
         }
      }

      for(int i1 = 0; i1 < 3; ++i1) {
         for(int k1 = 0; k1 < 9; ++k1) {
            this.addSlot(new Slot(p_39657_, k1 + i1 * 9 + 9, 8 + k1 * 18, 102 + i1 * 18 + -18));
         }
      }

      for(int j1 = 0; j1 < 9; ++j1) {
         this.addSlot(new Slot(p_39657_, j1, 8 + j1 * 18, 142));
      }

   }

   /**
    * Determines whether supplied player can use this container
    */
   public boolean stillValid(Player pPlayer) {
      return !this.horse.hasInventoryChanged(this.horseContainer) && this.horseContainer.stillValid(pPlayer) && this.horse.isAlive() && this.horse.distanceTo(pPlayer) < 8.0F;
   }

   private boolean hasChest(AbstractHorse p_150578_) {
      return p_150578_ instanceof AbstractChestedHorse && ((AbstractChestedHorse)p_150578_).hasChest();
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
         int i = this.horseContainer.getContainerSize();
         if (pIndex < i) {
            if (!this.moveItemStackTo(itemstack1, i, this.slots.size(), true)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(1).mayPlace(itemstack1) && !this.getSlot(1).hasItem()) {
            if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (this.getSlot(0).mayPlace(itemstack1)) {
            if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (i <= 2 || !this.moveItemStackTo(itemstack1, 2, i, false)) {
            int j = i + 27;
            int k = j + 9;
            if (pIndex >= j && pIndex < k) {
               if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (pIndex >= i && pIndex < j) {
               if (!this.moveItemStackTo(itemstack1, j, k, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(itemstack1, j, j, false)) {
               return ItemStack.EMPTY;
            }

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
      this.horseContainer.stopOpen(pPlayer);
   }
}