package net.minecraft.world.level.block.entity;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;

public class HopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
   public static final int MOVE_ITEM_SPEED = 8;
   public static final int HOPPER_CONTAINER_SIZE = 5;
   private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
   private int cooldownTime = -1;
   private long tickedGameTime;

   public HopperBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
      super(BlockEntityType.HOPPER, pWorldPosition, pBlockState);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (!this.tryLoadLootTable(pTag)) {
         ContainerHelper.loadAllItems(pTag, this.items);
      }

      this.cooldownTime = pTag.getInt("TransferCooldown");
   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      if (!this.trySaveLootTable(pTag)) {
         ContainerHelper.saveAllItems(pTag, this.items);
      }

      pTag.putInt("TransferCooldown", this.cooldownTime);
   }

   /**
    * Returns the number of slots in the inventory.
    */
   public int getContainerSize() {
      return this.items.size();
   }

   /**
    * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
    */
   public ItemStack removeItem(int pIndex, int pCount) {
      this.unpackLootTable((Player)null);
      return ContainerHelper.removeItem(this.getItems(), pIndex, pCount);
   }

   /**
    * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
    */
   public void setItem(int pIndex, ItemStack pStack) {
      this.unpackLootTable((Player)null);
      this.getItems().set(pIndex, pStack);
      if (pStack.getCount() > this.getMaxStackSize()) {
         pStack.setCount(this.getMaxStackSize());
      }

   }

   protected Component getDefaultName() {
      return new TranslatableComponent("container.hopper");
   }

   public static void pushItemsTick(Level pLevel, BlockPos pPos, BlockState pState, HopperBlockEntity pBlockEntity) {
      --pBlockEntity.cooldownTime;
      pBlockEntity.tickedGameTime = pLevel.getGameTime();
      if (!pBlockEntity.isOnCooldown()) {
         pBlockEntity.setCooldown(0);
         tryMoveItems(pLevel, pPos, pState, pBlockEntity, () -> {
            return suckInItems(pLevel, pBlockEntity);
         });
      }

   }

   private static boolean tryMoveItems(Level p_155579_, BlockPos p_155580_, BlockState p_155581_, HopperBlockEntity p_155582_, BooleanSupplier p_155583_) {
      if (p_155579_.isClientSide) {
         return false;
      } else {
         if (!p_155582_.isOnCooldown() && p_155581_.getValue(HopperBlock.ENABLED)) {
            boolean flag = false;
            if (!p_155582_.isEmpty()) {
               flag = ejectItems(p_155579_, p_155580_, p_155581_, p_155582_);
            }

            if (!p_155582_.inventoryFull()) {
               flag |= p_155583_.getAsBoolean();
            }

            if (flag) {
               p_155582_.setCooldown(8);
               setChanged(p_155579_, p_155580_, p_155581_);
               return true;
            }
         }

         return false;
      }
   }

   private boolean inventoryFull() {
      for(ItemStack itemstack : this.items) {
         if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) {
            return false;
         }
      }

      return true;
   }

   private static boolean ejectItems(Level pLevel, BlockPos pPos, BlockState pState, HopperBlockEntity pSourceContainer) {
      if (net.minecraftforge.items.VanillaInventoryCodeHooks.insertHook(pSourceContainer)) return true;
      Container container = getAttachedContainer(pLevel, pPos, pState);
      if (container == null) {
         return false;
      } else {
         Direction direction = pState.getValue(HopperBlock.FACING).getOpposite();
         if (isFullContainer(container, direction)) {
            return false;
         } else {
            for(int i = 0; i < pSourceContainer.getContainerSize(); ++i) {
               if (!pSourceContainer.getItem(i).isEmpty()) {
                  ItemStack itemstack = pSourceContainer.getItem(i).copy();
                  ItemStack itemstack1 = addItem(pSourceContainer, container, pSourceContainer.removeItem(i, 1), direction);
                  if (itemstack1.isEmpty()) {
                     container.setChanged();
                     return true;
                  }

                  pSourceContainer.setItem(i, itemstack);
               }
            }

            return false;
         }
      }
   }

   private static IntStream getSlots(Container p_59340_, Direction p_59341_) {
      return p_59340_ instanceof WorldlyContainer ? IntStream.of(((WorldlyContainer)p_59340_).getSlotsForFace(p_59341_)) : IntStream.range(0, p_59340_.getContainerSize());
   }

   /**
    * @return false if the container has any room to place items in
    */
   private static boolean isFullContainer(Container pContainer, Direction pDirection) {
      return getSlots(pContainer, pDirection).allMatch((p_59379_) -> {
         ItemStack itemstack = pContainer.getItem(p_59379_);
         return itemstack.getCount() >= itemstack.getMaxStackSize();
      });
   }

   /**
    * @return whether the given Container is empty from the given face
    */
   private static boolean isEmptyContainer(Container pContainer, Direction pDirection) {
      return getSlots(pContainer, pDirection).allMatch((p_59319_) -> {
         return pContainer.getItem(p_59319_).isEmpty();
      });
   }

   public static boolean suckInItems(Level p_155553_, Hopper p_155554_) {
      Boolean ret = net.minecraftforge.items.VanillaInventoryCodeHooks.extractHook(p_155553_, p_155554_);
      if (ret != null) return ret;
      Container container = getSourceContainer(p_155553_, p_155554_);
      if (container != null) {
         Direction direction = Direction.DOWN;
         return isEmptyContainer(container, direction) ? false : getSlots(container, direction).anyMatch((p_59363_) -> {
            return tryTakeInItemFromSlot(p_155554_, container, p_59363_, direction);
         });
      } else {
         for(ItemEntity itementity : getItemsAtAndAbove(p_155553_, p_155554_)) {
            if (addItem(p_155554_, itementity)) {
               return true;
            }
         }

         return false;
      }
   }

   /**
    * Pulls from the specified slot in the container and places in any available slot in the hopper.
    * @return true if the entire stack was moved
    */
   private static boolean tryTakeInItemFromSlot(Hopper pHopper, Container pContainer, int pSlot, Direction pDirection) {
      ItemStack itemstack = pContainer.getItem(pSlot);
      if (!itemstack.isEmpty() && canTakeItemFromContainer(pContainer, itemstack, pSlot, pDirection)) {
         ItemStack itemstack1 = itemstack.copy();
         ItemStack itemstack2 = addItem(pContainer, pHopper, pContainer.removeItem(pSlot, 1), (Direction)null);
         if (itemstack2.isEmpty()) {
            pContainer.setChanged();
            return true;
         }

         pContainer.setItem(pSlot, itemstack1);
      }

      return false;
   }

   public static boolean addItem(Container p_59332_, ItemEntity p_59333_) {
      boolean flag = false;
      ItemStack itemstack = p_59333_.getItem().copy();
      ItemStack itemstack1 = addItem((Container)null, p_59332_, itemstack, (Direction)null);
      if (itemstack1.isEmpty()) {
         flag = true;
         p_59333_.discard();
      } else {
         p_59333_.setItem(itemstack1);
      }

      return flag;
   }

   /**
    * Attempts to place the passed stack in the container, using as many slots as required.
    * @return any leftover stack
    */
   public static ItemStack addItem(@Nullable Container pSource, Container pDestination, ItemStack pStack, @Nullable Direction pDirection) {
      if (pDestination instanceof WorldlyContainer && pDirection != null) {
         WorldlyContainer worldlycontainer = (WorldlyContainer)pDestination;
         int[] aint = worldlycontainer.getSlotsForFace(pDirection);

         for(int k = 0; k < aint.length && !pStack.isEmpty(); ++k) {
            pStack = tryMoveInItem(pSource, pDestination, pStack, aint[k], pDirection);
         }
      } else {
         int i = pDestination.getContainerSize();

         for(int j = 0; j < i && !pStack.isEmpty(); ++j) {
            pStack = tryMoveInItem(pSource, pDestination, pStack, j, pDirection);
         }
      }

      return pStack;
   }

   private static boolean canPlaceItemInContainer(Container pContainer, ItemStack pStack, int pSlot, @Nullable Direction pDirection) {
      if (!pContainer.canPlaceItem(pSlot, pStack)) {
         return false;
      } else {
         return !(pContainer instanceof WorldlyContainer) || ((WorldlyContainer)pContainer).canPlaceItemThroughFace(pSlot, pStack, pDirection);
      }
   }

   private static boolean canTakeItemFromContainer(Container pContainer, ItemStack pStack, int pSlot, Direction pDirection) {
      return !(pContainer instanceof WorldlyContainer) || ((WorldlyContainer)pContainer).canTakeItemThroughFace(pSlot, pStack, pDirection);
   }

   private static ItemStack tryMoveInItem(@Nullable Container pSource, Container pDestination, ItemStack pStack, int pSlot, @Nullable Direction pDirection) {
      ItemStack itemstack = pDestination.getItem(pSlot);
      if (canPlaceItemInContainer(pDestination, pStack, pSlot, pDirection)) {
         boolean flag = false;
         boolean flag1 = pDestination.isEmpty();
         if (itemstack.isEmpty()) {
            pDestination.setItem(pSlot, pStack);
            pStack = ItemStack.EMPTY;
            flag = true;
         } else if (canMergeItems(itemstack, pStack)) {
            int i = pStack.getMaxStackSize() - itemstack.getCount();
            int j = Math.min(pStack.getCount(), i);
            pStack.shrink(j);
            itemstack.grow(j);
            flag = j > 0;
         }

         if (flag) {
            if (flag1 && pDestination instanceof HopperBlockEntity) {
               HopperBlockEntity hopperblockentity1 = (HopperBlockEntity)pDestination;
               if (!hopperblockentity1.isOnCustomCooldown()) {
                  int k = 0;
                  if (pSource instanceof HopperBlockEntity) {
                     HopperBlockEntity hopperblockentity = (HopperBlockEntity)pSource;
                     if (hopperblockentity1.tickedGameTime >= hopperblockentity.tickedGameTime) {
                        k = 1;
                     }
                  }

                  hopperblockentity1.setCooldown(8 - k);
               }
            }

            pDestination.setChanged();
         }
      }

      return pStack;
   }

   @Nullable
   private static Container getAttachedContainer(Level p_155593_, BlockPos p_155594_, BlockState p_155595_) {
      Direction direction = p_155595_.getValue(HopperBlock.FACING);
      return getContainerAt(p_155593_, p_155594_.relative(direction));
   }

   @Nullable
   private static Container getSourceContainer(Level p_155597_, Hopper p_155598_) {
      return getContainerAt(p_155597_, p_155598_.getLevelX(), p_155598_.getLevelY() + 1.0D, p_155598_.getLevelZ());
   }

   public static List<ItemEntity> getItemsAtAndAbove(Level p_155590_, Hopper p_155591_) {
      return p_155591_.getSuckShape().toAabbs().stream().flatMap((p_155558_) -> {
         return p_155590_.getEntitiesOfClass(ItemEntity.class, p_155558_.move(p_155591_.getLevelX() - 0.5D, p_155591_.getLevelY() - 0.5D, p_155591_.getLevelZ() - 0.5D), EntitySelector.ENTITY_STILL_ALIVE).stream();
      }).collect(Collectors.toList());
   }

   @Nullable
   public static Container getContainerAt(Level p_59391_, BlockPos p_59392_) {
      return getContainerAt(p_59391_, (double)p_59392_.getX() + 0.5D, (double)p_59392_.getY() + 0.5D, (double)p_59392_.getZ() + 0.5D);
   }

   /**
    * @return the container for the given position or null if none was found
    */
   @Nullable
   private static Container getContainerAt(Level pLevel, double pX, double pY, double pZ) {
      Container container = null;
      BlockPos blockpos = new BlockPos(pX, pY, pZ);
      BlockState blockstate = pLevel.getBlockState(blockpos);
      Block block = blockstate.getBlock();
      if (block instanceof WorldlyContainerHolder) {
         container = ((WorldlyContainerHolder)block).getContainer(blockstate, pLevel, blockpos);
      } else if (blockstate.hasBlockEntity()) {
         BlockEntity blockentity = pLevel.getBlockEntity(blockpos);
         if (blockentity instanceof Container) {
            container = (Container)blockentity;
            if (container instanceof ChestBlockEntity && block instanceof ChestBlock) {
               container = ChestBlock.getContainer((ChestBlock)block, blockstate, pLevel, blockpos, true);
            }
         }
      }

      if (container == null) {
         List<Entity> list = pLevel.getEntities((Entity)null, new AABB(pX - 0.5D, pY - 0.5D, pZ - 0.5D, pX + 0.5D, pY + 0.5D, pZ + 0.5D), EntitySelector.CONTAINER_ENTITY_SELECTOR);
         if (!list.isEmpty()) {
            container = (Container)list.get(pLevel.random.nextInt(list.size()));
         }
      }

      return container;
   }

   private static boolean canMergeItems(ItemStack pStack1, ItemStack pStack2) {
      if (!pStack1.is(pStack2.getItem())) {
         return false;
      } else if (pStack1.getDamageValue() != pStack2.getDamageValue()) {
         return false;
      } else if (pStack1.getCount() > pStack1.getMaxStackSize()) {
         return false;
      } else {
         return ItemStack.tagMatches(pStack1, pStack2);
      }
   }

   /**
    * Gets the world X position for this hopper entity.
    */
   public double getLevelX() {
      return (double)this.worldPosition.getX() + 0.5D;
   }

   /**
    * Gets the world Y position for this hopper entity.
    */
   public double getLevelY() {
      return (double)this.worldPosition.getY() + 0.5D;
   }

   /**
    * Gets the world Z position for this hopper entity.
    */
   public double getLevelZ() {
      return (double)this.worldPosition.getZ() + 0.5D;
   }

   public void setCooldown(int pCooldownTime) {
      this.cooldownTime = pCooldownTime;
   }

   private boolean isOnCooldown() {
      return this.cooldownTime > 0;
   }

   public boolean isOnCustomCooldown() {
      return this.cooldownTime > 8;
   }

   protected NonNullList<ItemStack> getItems() {
      return this.items;
   }

   protected void setItems(NonNullList<ItemStack> pItems) {
      this.items = pItems;
   }

   public static void entityInside(Level pLevel, BlockPos pPos, BlockState pState, Entity pEntity, HopperBlockEntity pBlockEntity) {
      if (pEntity instanceof ItemEntity && Shapes.joinIsNotEmpty(Shapes.create(pEntity.getBoundingBox().move((double)(-pPos.getX()), (double)(-pPos.getY()), (double)(-pPos.getZ()))), pBlockEntity.getSuckShape(), BooleanOp.AND)) {
         tryMoveItems(pLevel, pPos, pState, pBlockEntity, () -> {
            return addItem(pBlockEntity, (ItemEntity)pEntity);
         });
      }

   }

   protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
      return new HopperMenu(pId, pPlayer, this);
   }

   @Override
   protected net.minecraftforge.items.IItemHandler createUnSidedHandler() {
      return new net.minecraftforge.items.VanillaHopperItemHandler(this);
   }

   public long getLastUpdateTime() {
      return this.tickedGameTime;
   }
}
