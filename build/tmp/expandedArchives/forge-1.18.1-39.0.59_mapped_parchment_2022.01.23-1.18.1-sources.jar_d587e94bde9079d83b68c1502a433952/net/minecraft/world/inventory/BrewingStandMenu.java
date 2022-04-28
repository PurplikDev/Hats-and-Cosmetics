package net.minecraft.world.inventory;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionUtils;

public class BrewingStandMenu extends AbstractContainerMenu {
   private static final int BOTTLE_SLOT_START = 0;
   private static final int BOTTLE_SLOT_END = 2;
   private static final int INGREDIENT_SLOT = 3;
   private static final int FUEL_SLOT = 4;
   private static final int SLOT_COUNT = 5;
   private static final int DATA_COUNT = 2;
   private static final int INV_SLOT_START = 5;
   private static final int INV_SLOT_END = 32;
   private static final int USE_ROW_SLOT_START = 32;
   private static final int USE_ROW_SLOT_END = 41;
   private final Container brewingStand;
   private final ContainerData brewingStandData;
   private final Slot ingredientSlot;

   public BrewingStandMenu(int p_39090_, Inventory p_39091_) {
      this(p_39090_, p_39091_, new SimpleContainer(5), new SimpleContainerData(2));
   }

   public BrewingStandMenu(int p_39093_, Inventory p_39094_, Container p_39095_, ContainerData p_39096_) {
      super(MenuType.BREWING_STAND, p_39093_);
      checkContainerSize(p_39095_, 5);
      checkContainerDataCount(p_39096_, 2);
      this.brewingStand = p_39095_;
      this.brewingStandData = p_39096_;
      this.addSlot(new BrewingStandMenu.PotionSlot(p_39095_, 0, 56, 51));
      this.addSlot(new BrewingStandMenu.PotionSlot(p_39095_, 1, 79, 58));
      this.addSlot(new BrewingStandMenu.PotionSlot(p_39095_, 2, 102, 51));
      this.ingredientSlot = this.addSlot(new BrewingStandMenu.IngredientsSlot(p_39095_, 3, 79, 17));
      this.addSlot(new BrewingStandMenu.FuelSlot(p_39095_, 4, 17, 17));
      this.addDataSlots(p_39096_);

      for(int i = 0; i < 3; ++i) {
         for(int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(p_39094_, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
         }
      }

      for(int k = 0; k < 9; ++k) {
         this.addSlot(new Slot(p_39094_, k, 8 + k * 18, 142));
      }

   }

   /**
    * Determines whether supplied player can use this container
    */
   public boolean stillValid(Player pPlayer) {
      return this.brewingStand.stillValid(pPlayer);
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
         if ((pIndex < 0 || pIndex > 2) && pIndex != 3 && pIndex != 4) {
            if (BrewingStandMenu.FuelSlot.mayPlaceItem(itemstack)) {
               if (this.moveItemStackTo(itemstack1, 4, 5, false) || this.ingredientSlot.mayPlace(itemstack1) && !this.moveItemStackTo(itemstack1, 3, 4, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (this.ingredientSlot.mayPlace(itemstack1)) {
               if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (BrewingStandMenu.PotionSlot.mayPlaceItem(itemstack) && itemstack.getCount() == 1) {
               if (!this.moveItemStackTo(itemstack1, 0, 3, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (pIndex >= 5 && pIndex < 32) {
               if (!this.moveItemStackTo(itemstack1, 32, 41, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (pIndex >= 32 && pIndex < 41) {
               if (!this.moveItemStackTo(itemstack1, 5, 32, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.moveItemStackTo(itemstack1, 5, 41, false)) {
               return ItemStack.EMPTY;
            }
         } else {
            if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickCraft(itemstack1, itemstack);
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

   public int getFuel() {
      return this.brewingStandData.get(1);
   }

   public int getBrewingTicks() {
      return this.brewingStandData.get(0);
   }

   static class FuelSlot extends Slot {
      public FuelSlot(Container p_39105_, int p_39106_, int p_39107_, int p_39108_) {
         super(p_39105_, p_39106_, p_39107_, p_39108_);
      }

      /**
       * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
       */
      public boolean mayPlace(ItemStack pStack) {
         return mayPlaceItem(pStack);
      }

      /**
       * Returns true if the given ItemStack is usable as a fuel in the brewing stand.
       */
      public static boolean mayPlaceItem(ItemStack pItemStack) {
         return pItemStack.is(Items.BLAZE_POWDER);
      }

      /**
       * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in the
       * case of armor slots)
       */
      public int getMaxStackSize() {
         return 64;
      }
   }

   static class IngredientsSlot extends Slot {
      public IngredientsSlot(Container p_39115_, int p_39116_, int p_39117_, int p_39118_) {
         super(p_39115_, p_39116_, p_39117_, p_39118_);
      }

      /**
       * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
       */
      public boolean mayPlace(ItemStack pStack) {
         return net.minecraftforge.common.brewing.BrewingRecipeRegistry.isValidIngredient(pStack);
      }

      /**
       * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in the
       * case of armor slots)
       */
      public int getMaxStackSize() {
         return 64;
      }
   }

   static class PotionSlot extends Slot {
      public PotionSlot(Container p_39123_, int p_39124_, int p_39125_, int p_39126_) {
         super(p_39123_, p_39124_, p_39125_, p_39126_);
      }

      /**
       * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
       */
      public boolean mayPlace(ItemStack pStack) {
         return mayPlaceItem(pStack);
      }

      /**
       * Returns the maximum stack size for a given slot (usually the same as getInventoryStackLimit(), but 1 in the
       * case of armor slots)
       */
      public int getMaxStackSize() {
         return 1;
      }

      public void onTake(Player pPlayer, ItemStack pStack) {
         Potion potion = PotionUtils.getPotion(pStack);
         if (pPlayer instanceof ServerPlayer) {
            net.minecraftforge.event.ForgeEventFactory.onPlayerBrewedPotion(pPlayer, pStack);
            CriteriaTriggers.BREWED_POTION.trigger((ServerPlayer)pPlayer, potion);
         }

         super.onTake(pPlayer, pStack);
      }

      /**
       * Returns true if this itemstack can be filled with a potion
       */
      public static boolean mayPlaceItem(ItemStack pStack) {
         return net.minecraftforge.common.brewing.BrewingRecipeRegistry.isValidInput(pStack);
      }
   }
}
