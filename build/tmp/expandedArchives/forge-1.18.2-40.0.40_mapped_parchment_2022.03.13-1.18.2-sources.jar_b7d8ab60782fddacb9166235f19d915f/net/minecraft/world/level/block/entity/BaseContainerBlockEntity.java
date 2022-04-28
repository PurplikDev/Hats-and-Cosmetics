package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BaseContainerBlockEntity extends BlockEntity implements Container, MenuProvider, Nameable {
   private LockCode lockKey = LockCode.NO_LOCK;
   private Component name;

   protected BaseContainerBlockEntity(BlockEntityType<?> p_155076_, BlockPos p_155077_, BlockState p_155078_) {
      super(p_155076_, p_155077_, p_155078_);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      this.lockKey = LockCode.fromTag(pTag);
      if (pTag.contains("CustomName", 8)) {
         this.name = Component.Serializer.fromJson(pTag.getString("CustomName"));
      }

   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      this.lockKey.addToTag(pTag);
      if (this.name != null) {
         pTag.putString("CustomName", Component.Serializer.toJson(this.name));
      }

   }

   public void setCustomName(Component pName) {
      this.name = pName;
   }

   public Component getName() {
      return this.name != null ? this.name : this.getDefaultName();
   }

   public Component getDisplayName() {
      return this.getName();
   }

   @Nullable
   public Component getCustomName() {
      return this.name;
   }

   protected abstract Component getDefaultName();

   public boolean canOpen(Player pPlayer) {
      return canUnlock(pPlayer, this.lockKey, this.getDisplayName());
   }

   public static boolean canUnlock(Player p_58630_, LockCode p_58631_, Component p_58632_) {
      if (!p_58630_.isSpectator() && !p_58631_.unlocksWith(p_58630_.getMainHandItem())) {
         p_58630_.displayClientMessage(new TranslatableComponent("container.isLocked", p_58632_), true);
         p_58630_.playNotifySound(SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
      return this.canOpen(pPlayer) ? this.createMenu(pContainerId, pInventory) : null;
   }

   protected abstract AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory);

   private net.minecraftforge.common.util.LazyOptional<?> itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> createUnSidedHandler());
   protected net.minecraftforge.items.IItemHandler createUnSidedHandler() {
      return new net.minecraftforge.items.wrapper.InvWrapper(this);
   }

   public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @javax.annotation.Nullable net.minecraft.core.Direction side) {
      if (!this.remove && cap == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY )
         return itemHandler.cast();
      return super.getCapability(cap, side);
   }

   @Override
   public void invalidateCaps() {
      super.invalidateCaps();
      itemHandler.invalidate();
   }

   @Override
   public void reviveCaps() {
      super.reviveCaps();
      itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> createUnSidedHandler());
   }
}
