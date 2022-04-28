package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class LecternMenu extends AbstractContainerMenu {
   private static final int DATA_COUNT = 1;
   private static final int SLOT_COUNT = 1;
   public static final int BUTTON_PREV_PAGE = 1;
   public static final int BUTTON_NEXT_PAGE = 2;
   public static final int BUTTON_TAKE_BOOK = 3;
   public static final int BUTTON_PAGE_JUMP_RANGE_START = 100;
   private final Container lectern;
   private final ContainerData lecternData;

   public LecternMenu(int p_39822_) {
      this(p_39822_, new SimpleContainer(1), new SimpleContainerData(1));
   }

   public LecternMenu(int p_39824_, Container p_39825_, ContainerData p_39826_) {
      super(MenuType.LECTERN, p_39824_);
      checkContainerSize(p_39825_, 1);
      checkContainerDataCount(p_39826_, 1);
      this.lectern = p_39825_;
      this.lecternData = p_39826_;
      this.addSlot(new Slot(p_39825_, 0, 0, 0) {
         /**
          * Called when the stack in a Slot changes
          */
         public void setChanged() {
            super.setChanged();
            LecternMenu.this.slotsChanged(this.container);
         }
      });
      this.addDataSlots(p_39826_);
   }

   /**
    * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
    */
   public boolean clickMenuButton(Player pPlayer, int pId) {
      if (pId >= 100) {
         int k = pId - 100;
         this.setData(0, k);
         return true;
      } else {
         switch(pId) {
         case 1:
            int j = this.lecternData.get(0);
            this.setData(0, j - 1);
            return true;
         case 2:
            int i = this.lecternData.get(0);
            this.setData(0, i + 1);
            return true;
         case 3:
            if (!pPlayer.mayBuild()) {
               return false;
            }

            ItemStack itemstack = this.lectern.removeItemNoUpdate(0);
            this.lectern.setChanged();
            if (!pPlayer.getInventory().add(itemstack)) {
               pPlayer.drop(itemstack, false);
            }

            return true;
         default:
            return false;
         }
      }
   }

   public void setData(int pId, int pData) {
      super.setData(pId, pData);
      this.broadcastChanges();
   }

   /**
    * Determines whether supplied player can use this container
    */
   public boolean stillValid(Player pPlayer) {
      return this.lectern.stillValid(pPlayer);
   }

   public ItemStack getBook() {
      return this.lectern.getItem(0);
   }

   public int getPage() {
      return this.lecternData.get(0);
   }
}