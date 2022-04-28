package net.minecraft.world.inventory;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public abstract class AbstractContainerMenu {
   public static final int SLOT_CLICKED_OUTSIDE = -999;
   public static final int QUICKCRAFT_TYPE_CHARITABLE = 0;
   public static final int QUICKCRAFT_TYPE_GREEDY = 1;
   public static final int QUICKCRAFT_TYPE_CLONE = 2;
   public static final int QUICKCRAFT_HEADER_START = 0;
   public static final int QUICKCRAFT_HEADER_CONTINUE = 1;
   public static final int QUICKCRAFT_HEADER_END = 2;
   public static final int CARRIED_SLOT_SIZE = Integer.MAX_VALUE;
   private final NonNullList<ItemStack> lastSlots = NonNullList.create();
   public final NonNullList<Slot> slots = NonNullList.create();
   private final List<DataSlot> dataSlots = Lists.newArrayList();
   private ItemStack carried = ItemStack.EMPTY;
   private final NonNullList<ItemStack> remoteSlots = NonNullList.create();
   private final IntList remoteDataSlots = new IntArrayList();
   private ItemStack remoteCarried = ItemStack.EMPTY;
   private int stateId;
   @Nullable
   private final MenuType<?> menuType;
   public final int containerId;
   private int quickcraftType = -1;
   private int quickcraftStatus;
   private final Set<Slot> quickcraftSlots = Sets.newHashSet();
   private final List<ContainerListener> containerListeners = Lists.newArrayList();
   @Nullable
   private ContainerSynchronizer synchronizer;
   private boolean suppressRemoteUpdates;

   protected AbstractContainerMenu(@Nullable MenuType<?> pMenuType, int pContainerId) {
      this.menuType = pMenuType;
      this.containerId = pContainerId;
   }

   protected static boolean stillValid(ContainerLevelAccess pLevelPos, Player pPlayer, Block pTargetBlock) {
      return pLevelPos.evaluate((p_38916_, p_38917_) -> {
         return !p_38916_.getBlockState(p_38917_).is(pTargetBlock) ? false : pPlayer.distanceToSqr((double)p_38917_.getX() + 0.5D, (double)p_38917_.getY() + 0.5D, (double)p_38917_.getZ() + 0.5D) <= 64.0D;
      }, true);
   }

   public MenuType<?> getType() {
      if (this.menuType == null) {
         throw new UnsupportedOperationException("Unable to construct this menu by type");
      } else {
         return this.menuType;
      }
   }

   protected static void checkContainerSize(Container pInventory, int pMinSize) {
      int i = pInventory.getContainerSize();
      if (i < pMinSize) {
         throw new IllegalArgumentException("Container size " + i + " is smaller than expected " + pMinSize);
      }
   }

   protected static void checkContainerDataCount(ContainerData pIntArray, int pMinSize) {
      int i = pIntArray.getCount();
      if (i < pMinSize) {
         throw new IllegalArgumentException("Container data count " + i + " is smaller than expected " + pMinSize);
      }
   }

   /**
    * Adds an item slot to this container
    */
   protected Slot addSlot(Slot pSlot) {
      pSlot.index = this.slots.size();
      this.slots.add(pSlot);
      this.lastSlots.add(ItemStack.EMPTY);
      this.remoteSlots.add(ItemStack.EMPTY);
      return pSlot;
   }

   protected DataSlot addDataSlot(DataSlot pIntValue) {
      this.dataSlots.add(pIntValue);
      this.remoteDataSlots.add(0);
      return pIntValue;
   }

   protected void addDataSlots(ContainerData pArray) {
      for(int i = 0; i < pArray.getCount(); ++i) {
         this.addDataSlot(DataSlot.forContainer(pArray, i));
      }

   }

   public void addSlotListener(ContainerListener pListener) {
      if (!this.containerListeners.contains(pListener)) {
         this.containerListeners.add(pListener);
         this.broadcastChanges();
      }
   }

   public void setSynchronizer(ContainerSynchronizer p_150417_) {
      this.synchronizer = p_150417_;
      this.sendAllDataToRemote();
   }

   public void sendAllDataToRemote() {
      int i = 0;

      for(int j = this.slots.size(); i < j; ++i) {
         this.remoteSlots.set(i, this.slots.get(i).getItem().copy());
      }

      this.remoteCarried = this.getCarried().copy();
      i = 0;

      for(int k = this.dataSlots.size(); i < k; ++i) {
         this.remoteDataSlots.set(i, this.dataSlots.get(i).get());
      }

      if (this.synchronizer != null) {
         this.synchronizer.sendInitialData(this, this.remoteSlots, this.remoteCarried, this.remoteDataSlots.toIntArray());
      }

   }

   /**
    * Remove the given Listener. Method name is for legacy.
    */
   public void removeSlotListener(ContainerListener pListener) {
      this.containerListeners.remove(pListener);
   }

   /**
    * returns a list if itemStacks, for each slot.
    */
   public NonNullList<ItemStack> getItems() {
      NonNullList<ItemStack> nonnulllist = NonNullList.create();

      for(Slot slot : this.slots) {
         nonnulllist.add(slot.getItem());
      }

      return nonnulllist;
   }

   /**
    * Looks for changes made in the container, sends them to every listener.
    */
   public void broadcastChanges() {
      for(int i = 0; i < this.slots.size(); ++i) {
         ItemStack itemstack = this.slots.get(i).getItem();
         Supplier<ItemStack> supplier = Suppliers.memoize(itemstack::copy);
         this.triggerSlotListeners(i, itemstack, supplier);
         this.synchronizeSlotToRemote(i, itemstack, supplier);
      }

      this.synchronizeCarriedToRemote();

      for(int j = 0; j < this.dataSlots.size(); ++j) {
         DataSlot dataslot = this.dataSlots.get(j);
         int k = dataslot.get();
         if (dataslot.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(j, k);
         }

         this.synchronizeDataSlotToRemote(j, k);
      }

   }

   public void broadcastFullState() {
      for(int i = 0; i < this.slots.size(); ++i) {
         ItemStack itemstack = this.slots.get(i).getItem();
         this.triggerSlotListeners(i, itemstack, itemstack::copy);
      }

      for(int j = 0; j < this.dataSlots.size(); ++j) {
         DataSlot dataslot = this.dataSlots.get(j);
         if (dataslot.checkAndClearUpdateFlag()) {
            this.updateDataSlotListeners(j, dataslot.get());
         }
      }

      this.sendAllDataToRemote();
   }

   private void updateDataSlotListeners(int p_182421_, int p_182422_) {
      for(ContainerListener containerlistener : this.containerListeners) {
         containerlistener.dataChanged(this, p_182421_, p_182422_);
      }

   }

   private void triggerSlotListeners(int p_150408_, ItemStack p_150409_, Supplier<ItemStack> p_150410_) {
      ItemStack itemstack = this.lastSlots.get(p_150408_);
      if (!ItemStack.matches(itemstack, p_150409_)) {
         boolean clientStackChanged = !p_150409_.equals(itemstack, true);
         ItemStack itemstack1 = p_150410_.get();
         this.lastSlots.set(p_150408_, itemstack1);

         if(clientStackChanged)
         for(ContainerListener containerlistener : this.containerListeners) {
            containerlistener.slotChanged(this, p_150408_, itemstack1);
         }
      }

   }

   private void synchronizeSlotToRemote(int p_150436_, ItemStack p_150437_, Supplier<ItemStack> p_150438_) {
      if (!this.suppressRemoteUpdates) {
         ItemStack itemstack = this.remoteSlots.get(p_150436_);
         if (!ItemStack.matches(itemstack, p_150437_)) {
            ItemStack itemstack1 = p_150438_.get();
            this.remoteSlots.set(p_150436_, itemstack1);
            if (this.synchronizer != null) {
               this.synchronizer.sendSlotChange(this, p_150436_, itemstack1);
            }
         }

      }
   }

   private void synchronizeDataSlotToRemote(int p_150441_, int p_150442_) {
      if (!this.suppressRemoteUpdates) {
         int i = this.remoteDataSlots.getInt(p_150441_);
         if (i != p_150442_) {
            this.remoteDataSlots.set(p_150441_, p_150442_);
            if (this.synchronizer != null) {
               this.synchronizer.sendDataChange(this, p_150441_, p_150442_);
            }
         }

      }
   }

   private void synchronizeCarriedToRemote() {
      if (!this.suppressRemoteUpdates) {
         if (!ItemStack.matches(this.getCarried(), this.remoteCarried)) {
            this.remoteCarried = this.getCarried().copy();
            if (this.synchronizer != null) {
               this.synchronizer.sendCarriedChange(this, this.remoteCarried);
            }
         }

      }
   }

   public void setRemoteSlot(int p_150405_, ItemStack p_150406_) {
      this.remoteSlots.set(p_150405_, p_150406_.copy());
   }

   public void setRemoteSlotNoCopy(int p_182415_, ItemStack p_182416_) {
      this.remoteSlots.set(p_182415_, p_182416_);
   }

   public void setRemoteCarried(ItemStack p_150423_) {
      this.remoteCarried = p_150423_.copy();
   }

   /**
    * Handles the given Button-click on the server, currently only used by enchanting. Name is for legacy.
    */
   public boolean clickMenuButton(Player pPlayer, int pId) {
      return false;
   }

   public Slot getSlot(int pSlotId) {
      return this.slots.get(pSlotId);
   }

   /**
    * Handle when the stack in slot {@code index} is shift-clicked. Normally this moves the stack between the player
    * inventory and the other inventory(s).
    */
   public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
      return this.slots.get(pIndex).getItem();
   }

   public void clicked(int p_150400_, int p_150401_, ClickType p_150402_, Player p_150403_) {
      try {
         this.doClick(p_150400_, p_150401_, p_150402_, p_150403_);
      } catch (Exception exception) {
         CrashReport crashreport = CrashReport.forThrowable(exception, "Container click");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Click info");
         crashreportcategory.setDetail("Menu Type", () -> {
            return this.menuType != null ? Registry.MENU.getKey(this.menuType).toString() : "<no type>";
         });
         crashreportcategory.setDetail("Menu Class", () -> {
            return this.getClass().getCanonicalName();
         });
         crashreportcategory.setDetail("Slot Count", this.slots.size());
         crashreportcategory.setDetail("Slot", p_150400_);
         crashreportcategory.setDetail("Button", p_150401_);
         crashreportcategory.setDetail("Type", p_150402_);
         throw new ReportedException(crashreport);
      }
   }

   private void doClick(int p_150431_, int p_150432_, ClickType p_150433_, Player p_150434_) {
      Inventory inventory = p_150434_.getInventory();
      if (p_150433_ == ClickType.QUICK_CRAFT) {
         int i = this.quickcraftStatus;
         this.quickcraftStatus = getQuickcraftHeader(p_150432_);
         if ((i != 1 || this.quickcraftStatus != 2) && i != this.quickcraftStatus) {
            this.resetQuickCraft();
         } else if (this.getCarried().isEmpty()) {
            this.resetQuickCraft();
         } else if (this.quickcraftStatus == 0) {
            this.quickcraftType = getQuickcraftType(p_150432_);
            if (isValidQuickcraftType(this.quickcraftType, p_150434_)) {
               this.quickcraftStatus = 1;
               this.quickcraftSlots.clear();
            } else {
               this.resetQuickCraft();
            }
         } else if (this.quickcraftStatus == 1) {
            Slot slot = this.slots.get(p_150431_);
            ItemStack itemstack = this.getCarried();
            if (canItemQuickReplace(slot, itemstack, true) && slot.mayPlace(itemstack) && (this.quickcraftType == 2 || itemstack.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot)) {
               this.quickcraftSlots.add(slot);
            }
         } else if (this.quickcraftStatus == 2) {
            if (!this.quickcraftSlots.isEmpty()) {
               if (this.quickcraftSlots.size() == 1) {
                  int l = (this.quickcraftSlots.iterator().next()).index;
                  this.resetQuickCraft();
                  this.doClick(l, this.quickcraftType, ClickType.PICKUP, p_150434_);
                  return;
               }

               ItemStack itemstack3 = this.getCarried().copy();
               int j1 = this.getCarried().getCount();

               for(Slot slot1 : this.quickcraftSlots) {
                  ItemStack itemstack1 = this.getCarried();
                  if (slot1 != null && canItemQuickReplace(slot1, itemstack1, true) && slot1.mayPlace(itemstack1) && (this.quickcraftType == 2 || itemstack1.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot1)) {
                     ItemStack itemstack2 = itemstack3.copy();
                     int j = slot1.hasItem() ? slot1.getItem().getCount() : 0;
                     getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemstack2, j);
                     int k = Math.min(itemstack2.getMaxStackSize(), slot1.getMaxStackSize(itemstack2));
                     if (itemstack2.getCount() > k) {
                        itemstack2.setCount(k);
                     }

                     j1 -= itemstack2.getCount() - j;
                     slot1.set(itemstack2);
                  }
               }

               itemstack3.setCount(j1);
               this.setCarried(itemstack3);
            }

            this.resetQuickCraft();
         } else {
            this.resetQuickCraft();
         }
      } else if (this.quickcraftStatus != 0) {
         this.resetQuickCraft();
      } else if ((p_150433_ == ClickType.PICKUP || p_150433_ == ClickType.QUICK_MOVE) && (p_150432_ == 0 || p_150432_ == 1)) {
         ClickAction clickaction = p_150432_ == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
         if (p_150431_ == -999) {
            if (!this.getCarried().isEmpty()) {
               if (clickaction == ClickAction.PRIMARY) {
                  p_150434_.drop(this.getCarried(), true);
                  this.setCarried(ItemStack.EMPTY);
               } else {
                  p_150434_.drop(this.getCarried().split(1), true);
               }
            }
         } else if (p_150433_ == ClickType.QUICK_MOVE) {
            if (p_150431_ < 0) {
               return;
            }

            Slot slot6 = this.slots.get(p_150431_);
            if (!slot6.mayPickup(p_150434_)) {
               return;
            }

            for(ItemStack itemstack9 = this.quickMoveStack(p_150434_, p_150431_); !itemstack9.isEmpty() && ItemStack.isSame(slot6.getItem(), itemstack9); itemstack9 = this.quickMoveStack(p_150434_, p_150431_)) {
            }
         } else {
            if (p_150431_ < 0) {
               return;
            }

            Slot slot7 = this.slots.get(p_150431_);
            ItemStack itemstack10 = slot7.getItem();
            ItemStack itemstack11 = this.getCarried();
            p_150434_.updateTutorialInventoryAction(itemstack11, slot7.getItem(), clickaction);
            if (!itemstack11.overrideStackedOnOther(slot7, clickaction, p_150434_) && !itemstack10.overrideOtherStackedOnMe(itemstack11, slot7, clickaction, p_150434_, this.createCarriedSlotAccess())) {
               if (itemstack10.isEmpty()) {
                  if (!itemstack11.isEmpty()) {
                     int l2 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                     this.setCarried(slot7.safeInsert(itemstack11, l2));
                  }
               } else if (slot7.mayPickup(p_150434_)) {
                  if (itemstack11.isEmpty()) {
                     int i3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : (itemstack10.getCount() + 1) / 2;
                     Optional<ItemStack> optional1 = slot7.tryRemove(i3, Integer.MAX_VALUE, p_150434_);
                     optional1.ifPresent((p_150421_) -> {
                        this.setCarried(p_150421_);
                        slot7.onTake(p_150434_, p_150421_);
                     });
                  } else if (slot7.mayPlace(itemstack11)) {
                     if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                        int j3 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                        this.setCarried(slot7.safeInsert(itemstack11, j3));
                     } else if (itemstack11.getCount() <= slot7.getMaxStackSize(itemstack11)) {
                        slot7.set(itemstack11);
                        this.setCarried(itemstack10);
                     }
                  } else if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                     Optional<ItemStack> optional = slot7.tryRemove(itemstack10.getCount(), itemstack11.getMaxStackSize() - itemstack11.getCount(), p_150434_);
                     optional.ifPresent((p_150428_) -> {
                        itemstack11.grow(p_150428_.getCount());
                        slot7.onTake(p_150434_, p_150428_);
                     });
                  }
               }
            }

            slot7.setChanged();
         }
      } else if (p_150433_ == ClickType.SWAP) {
         Slot slot2 = this.slots.get(p_150431_);
         ItemStack itemstack4 = inventory.getItem(p_150432_);
         ItemStack itemstack7 = slot2.getItem();
         if (!itemstack4.isEmpty() || !itemstack7.isEmpty()) {
            if (itemstack4.isEmpty()) {
               if (slot2.mayPickup(p_150434_)) {
                  inventory.setItem(p_150432_, itemstack7);
                  slot2.onSwapCraft(itemstack7.getCount());
                  slot2.set(ItemStack.EMPTY);
                  slot2.onTake(p_150434_, itemstack7);
               }
            } else if (itemstack7.isEmpty()) {
               if (slot2.mayPlace(itemstack4)) {
                  int l1 = slot2.getMaxStackSize(itemstack4);
                  if (itemstack4.getCount() > l1) {
                     slot2.set(itemstack4.split(l1));
                  } else {
                     inventory.setItem(p_150432_, ItemStack.EMPTY);
                     slot2.set(itemstack4);
                  }
               }
            } else if (slot2.mayPickup(p_150434_) && slot2.mayPlace(itemstack4)) {
               int i2 = slot2.getMaxStackSize(itemstack4);
               if (itemstack4.getCount() > i2) {
                  slot2.set(itemstack4.split(i2));
                  slot2.onTake(p_150434_, itemstack7);
                  if (!inventory.add(itemstack7)) {
                     p_150434_.drop(itemstack7, true);
                  }
               } else {
                  inventory.setItem(p_150432_, itemstack7);
                  slot2.set(itemstack4);
                  slot2.onTake(p_150434_, itemstack7);
               }
            }
         }
      } else if (p_150433_ == ClickType.CLONE && p_150434_.getAbilities().instabuild && this.getCarried().isEmpty() && p_150431_ >= 0) {
         Slot slot5 = this.slots.get(p_150431_);
         if (slot5.hasItem()) {
            ItemStack itemstack6 = slot5.getItem().copy();
            itemstack6.setCount(itemstack6.getMaxStackSize());
            this.setCarried(itemstack6);
         }
      } else if (p_150433_ == ClickType.THROW && this.getCarried().isEmpty() && p_150431_ >= 0) {
         Slot slot4 = this.slots.get(p_150431_);
         int i1 = p_150432_ == 0 ? 1 : slot4.getItem().getCount();
         ItemStack itemstack8 = slot4.safeTake(i1, Integer.MAX_VALUE, p_150434_);
         p_150434_.drop(itemstack8, true);
      } else if (p_150433_ == ClickType.PICKUP_ALL && p_150431_ >= 0) {
         Slot slot3 = this.slots.get(p_150431_);
         ItemStack itemstack5 = this.getCarried();
         if (!itemstack5.isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(p_150434_))) {
            int k1 = p_150432_ == 0 ? 0 : this.slots.size() - 1;
            int j2 = p_150432_ == 0 ? 1 : -1;

            for(int k2 = 0; k2 < 2; ++k2) {
               for(int k3 = k1; k3 >= 0 && k3 < this.slots.size() && itemstack5.getCount() < itemstack5.getMaxStackSize(); k3 += j2) {
                  Slot slot8 = this.slots.get(k3);
                  if (slot8.hasItem() && canItemQuickReplace(slot8, itemstack5, true) && slot8.mayPickup(p_150434_) && this.canTakeItemForPickAll(itemstack5, slot8)) {
                     ItemStack itemstack12 = slot8.getItem();
                     if (k2 != 0 || itemstack12.getCount() != itemstack12.getMaxStackSize()) {
                        ItemStack itemstack13 = slot8.safeTake(itemstack12.getCount(), itemstack5.getMaxStackSize() - itemstack5.getCount(), p_150434_);
                        itemstack5.grow(itemstack13.getCount());
                     }
                  }
               }
            }
         }
      }

   }

   private SlotAccess createCarriedSlotAccess() {
      return new SlotAccess() {
         public ItemStack get() {
            return AbstractContainerMenu.this.getCarried();
         }

         public boolean set(ItemStack p_150452_) {
            AbstractContainerMenu.this.setCarried(p_150452_);
            return true;
         }
      };
   }

   /**
    * Called to determine if the current slot is valid for the stack merging (double-click) code. The stack passed in is
    * null for the initial slot that was double-clicked.
    */
   public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
      return true;
   }

   /**
    * Called when the container is closed.
    */
   public void removed(Player pPlayer) {
      if (pPlayer instanceof ServerPlayer) {
         ItemStack itemstack = this.getCarried();
         if (!itemstack.isEmpty()) {
            if (pPlayer.isAlive() && !((ServerPlayer)pPlayer).hasDisconnected()) {
               pPlayer.getInventory().placeItemBackInInventory(itemstack);
            } else {
               pPlayer.drop(itemstack, false);
            }

            this.setCarried(ItemStack.EMPTY);
         }
      }

   }

   protected void clearContainer(Player p_150412_, Container p_150413_) {
      if (!p_150412_.isAlive() || p_150412_ instanceof ServerPlayer && ((ServerPlayer)p_150412_).hasDisconnected()) {
         for(int j = 0; j < p_150413_.getContainerSize(); ++j) {
            p_150412_.drop(p_150413_.removeItemNoUpdate(j), false);
         }

      } else {
         for(int i = 0; i < p_150413_.getContainerSize(); ++i) {
            Inventory inventory = p_150412_.getInventory();
            if (inventory.player instanceof ServerPlayer) {
               inventory.placeItemBackInInventory(p_150413_.removeItemNoUpdate(i));
            }
         }

      }
   }

   /**
    * Callback for when the crafting matrix is changed.
    */
   public void slotsChanged(Container pInventory) {
      this.broadcastChanges();
   }

   /**
    * Puts an ItemStack in a slot.
    */
   public void setItem(int pSlotId, int pStateId, ItemStack pStack) {
      this.getSlot(pSlotId).set(pStack);
      this.stateId = pStateId;
   }

   public void initializeContents(int p_182411_, List<ItemStack> p_182412_, ItemStack p_182413_) {
      for(int i = 0; i < p_182412_.size(); ++i) {
         this.getSlot(i).set(p_182412_.get(i));
      }

      this.carried = p_182413_;
      this.stateId = p_182411_;
   }

   public void setData(int pId, int pData) {
      this.dataSlots.get(pId).set(pData);
   }

   /**
    * Determines whether supplied player can use this container
    */
   public abstract boolean stillValid(Player pPlayer);

   /**
    * Merges provided ItemStack with the first avaliable one in the container/player inventor between minIndex
    * (included) and maxIndex (excluded). Args : stack, minIndex, maxIndex, negativDirection. [!] the Container
    * implementation do not check if the item is valid for the slot
    */
   protected boolean moveItemStackTo(ItemStack pStack, int pStartIndex, int pEndIndex, boolean pReverseDirection) {
      boolean flag = false;
      int i = pStartIndex;
      if (pReverseDirection) {
         i = pEndIndex - 1;
      }

      if (pStack.isStackable()) {
         while(!pStack.isEmpty()) {
            if (pReverseDirection) {
               if (i < pStartIndex) {
                  break;
               }
            } else if (i >= pEndIndex) {
               break;
            }

            Slot slot = this.slots.get(i);
            ItemStack itemstack = slot.getItem();
            if (!itemstack.isEmpty() && ItemStack.isSameItemSameTags(pStack, itemstack)) {
               int j = itemstack.getCount() + pStack.getCount();
               int maxSize = Math.min(slot.getMaxStackSize(), pStack.getMaxStackSize());
               if (j <= maxSize) {
                  pStack.setCount(0);
                  itemstack.setCount(j);
                  slot.setChanged();
                  flag = true;
               } else if (itemstack.getCount() < maxSize) {
                  pStack.shrink(maxSize - itemstack.getCount());
                  itemstack.setCount(maxSize);
                  slot.setChanged();
                  flag = true;
               }
            }

            if (pReverseDirection) {
               --i;
            } else {
               ++i;
            }
         }
      }

      if (!pStack.isEmpty()) {
         if (pReverseDirection) {
            i = pEndIndex - 1;
         } else {
            i = pStartIndex;
         }

         while(true) {
            if (pReverseDirection) {
               if (i < pStartIndex) {
                  break;
               }
            } else if (i >= pEndIndex) {
               break;
            }

            Slot slot1 = this.slots.get(i);
            ItemStack itemstack1 = slot1.getItem();
            if (itemstack1.isEmpty() && slot1.mayPlace(pStack)) {
               if (pStack.getCount() > slot1.getMaxStackSize()) {
                  slot1.set(pStack.split(slot1.getMaxStackSize()));
               } else {
                  slot1.set(pStack.split(pStack.getCount()));
               }

               slot1.setChanged();
               flag = true;
               break;
            }

            if (pReverseDirection) {
               --i;
            } else {
               ++i;
            }
         }
      }

      return flag;
   }

   /**
    * Extracts the drag mode. Args : eventButton. Return (0 : evenly split, 1 : one item by slot, 2 : not used ?)
    */
   public static int getQuickcraftType(int pEventButton) {
      return pEventButton >> 2 & 3;
   }

   /**
    * Args : clickedButton, Returns (0 : start drag, 1 : add slot, 2 : end drag)
    */
   public static int getQuickcraftHeader(int pClickedButton) {
      return pClickedButton & 3;
   }

   public static int getQuickcraftMask(int p_38931_, int p_38932_) {
      return p_38931_ & 3 | (p_38932_ & 3) << 2;
   }

   public static boolean isValidQuickcraftType(int pDragMode, Player pPlayer) {
      if (pDragMode == 0) {
         return true;
      } else if (pDragMode == 1) {
         return true;
      } else {
         return pDragMode == 2 && pPlayer.getAbilities().instabuild;
      }
   }

   /**
    * Reset the drag fields
    */
   protected void resetQuickCraft() {
      this.quickcraftStatus = 0;
      this.quickcraftSlots.clear();
   }

   /**
    * Checks if it's possible to add the given itemstack to the given slot.
    */
   public static boolean canItemQuickReplace(@Nullable Slot pSlot, ItemStack pStack, boolean pStackSizeMatters) {
      boolean flag = pSlot == null || !pSlot.hasItem();
      if (!flag && ItemStack.isSameItemSameTags(pStack, pSlot.getItem())) {
         return pSlot.getItem().getCount() + (pStackSizeMatters ? 0 : pStack.getCount()) <= pStack.getMaxStackSize();
      } else {
         return flag;
      }
   }

   /**
    * Compute the new stack size, Returns the stack with the new size. Args : dragSlots, dragMode, dragStack,
    * slotStackSize
    */
   public static void getQuickCraftSlotCount(Set<Slot> pDragSlots, int pDragMode, ItemStack pStack, int pSlotStackSize) {
      switch(pDragMode) {
      case 0:
         pStack.setCount(Mth.floor((float)pStack.getCount() / (float)pDragSlots.size()));
         break;
      case 1:
         pStack.setCount(1);
         break;
      case 2:
         pStack.setCount(pStack.getMaxStackSize());
      }

      pStack.grow(pSlotStackSize);
   }

   /**
    * Returns true if the player can "drag-spilt" items into this slot,. returns true by default. Called to check if the
    * slot can be added to a list of Slots to split the held ItemStack across.
    */
   public boolean canDragTo(Slot pSlot) {
      return true;
   }

   /**
    * Like the version that takes an inventory. If the given TileEntity is not an Inventory, 0 is returned instead.
    */
   public static int getRedstoneSignalFromBlockEntity(@Nullable BlockEntity pTe) {
      return pTe instanceof Container ? getRedstoneSignalFromContainer((Container)pTe) : 0;
   }

   public static int getRedstoneSignalFromContainer(@Nullable Container pInv) {
      if (pInv == null) {
         return 0;
      } else {
         int i = 0;
         float f = 0.0F;

         for(int j = 0; j < pInv.getContainerSize(); ++j) {
            ItemStack itemstack = pInv.getItem(j);
            if (!itemstack.isEmpty()) {
               f += (float)itemstack.getCount() / (float)Math.min(pInv.getMaxStackSize(), itemstack.getMaxStackSize());
               ++i;
            }
         }

         f /= (float)pInv.getContainerSize();
         return Mth.floor(f * 14.0F) + (i > 0 ? 1 : 0);
      }
   }

   public void setCarried(ItemStack pStack) {
      this.carried = pStack;
   }

   public ItemStack getCarried() {
      return this.carried;
   }

   public void suppressRemoteUpdates() {
      this.suppressRemoteUpdates = true;
   }

   public void resumeRemoteUpdates() {
      this.suppressRemoteUpdates = false;
   }

   public void transferState(AbstractContainerMenu p_150415_) {
      Table<Container, Integer, Integer> table = HashBasedTable.create();

      for(int i = 0; i < p_150415_.slots.size(); ++i) {
         Slot slot = p_150415_.slots.get(i);
         table.put(slot.container, slot.getContainerSlot(), i);
      }

      for(int j = 0; j < this.slots.size(); ++j) {
         Slot slot1 = this.slots.get(j);
         Integer integer = table.get(slot1.container, slot1.getContainerSlot());
         if (integer != null) {
            this.lastSlots.set(j, p_150415_.lastSlots.get(integer));
            this.remoteSlots.set(j, p_150415_.remoteSlots.get(integer));
         }
      }

   }

   public OptionalInt findSlot(Container p_182418_, int p_182419_) {
      for(int i = 0; i < this.slots.size(); ++i) {
         Slot slot = this.slots.get(i);
         if (slot.container == p_182418_ && p_182419_ == slot.getContainerSlot()) {
            return OptionalInt.of(i);
         }
      }

      return OptionalInt.empty();
   }

   public int getStateId() {
      return this.stateId;
   }

   public int incrementStateId() {
      this.stateId = this.stateId + 1 & 32767;
      return this.stateId;
   }
}
