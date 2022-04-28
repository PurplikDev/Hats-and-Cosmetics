package net.minecraft.world.entity.vehicle;

import javax.annotation.Nullable;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

public abstract class AbstractMinecartContainer extends AbstractMinecart implements Container, MenuProvider {
   private NonNullList<ItemStack> itemStacks = NonNullList.withSize(36, ItemStack.EMPTY);
   @Nullable
   private ResourceLocation lootTable;
   private long lootTableSeed;

   protected AbstractMinecartContainer(EntityType<?> p_38213_, Level p_38214_) {
      super(p_38213_, p_38214_);
   }

   protected AbstractMinecartContainer(EntityType<?> p_38207_, double p_38208_, double p_38209_, double p_38210_, Level p_38211_) {
      super(p_38207_, p_38211_, p_38208_, p_38209_, p_38210_);
   }

   public void destroy(DamageSource pSource) {
      super.destroy(pSource);
      if (this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
         Containers.dropContents(this.level, this, this);
         if (!this.level.isClientSide) {
            Entity entity = pSource.getDirectEntity();
            if (entity != null && entity.getType() == EntityType.PLAYER) {
               PiglinAi.angerNearbyPiglins((Player)entity, true);
            }
         }
      }

   }

   public boolean isEmpty() {
      for(ItemStack itemstack : this.itemStacks) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   /**
    * Returns the stack in the given slot.
    */
   public ItemStack getItem(int pIndex) {
      this.unpackLootTable((Player)null);
      return this.itemStacks.get(pIndex);
   }

   /**
    * Removes up to a specified number of items from an inventory slot and returns them in a new stack.
    */
   public ItemStack removeItem(int pIndex, int pCount) {
      this.unpackLootTable((Player)null);
      return ContainerHelper.removeItem(this.itemStacks, pIndex, pCount);
   }

   /**
    * Removes a stack from the given slot and returns it.
    */
   public ItemStack removeItemNoUpdate(int pIndex) {
      this.unpackLootTable((Player)null);
      ItemStack itemstack = this.itemStacks.get(pIndex);
      if (itemstack.isEmpty()) {
         return ItemStack.EMPTY;
      } else {
         this.itemStacks.set(pIndex, ItemStack.EMPTY);
         return itemstack;
      }
   }

   /**
    * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
    */
   public void setItem(int pIndex, ItemStack pStack) {
      this.unpackLootTable((Player)null);
      this.itemStacks.set(pIndex, pStack);
      if (!pStack.isEmpty() && pStack.getCount() > this.getMaxStackSize()) {
         pStack.setCount(this.getMaxStackSize());
      }

   }

   public SlotAccess getSlot(final int pSlot) {
      return pSlot >= 0 && pSlot < this.getContainerSize() ? new SlotAccess() {
         public ItemStack get() {
            return AbstractMinecartContainer.this.getItem(pSlot);
         }

         public boolean set(ItemStack p_150265_) {
            AbstractMinecartContainer.this.setItem(pSlot, p_150265_);
            return true;
         }
      } : super.getSlot(pSlot);
   }

   /**
    * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it
    * hasn't changed and skip it.
    */
   public void setChanged() {
   }

   /**
    * Don't rename this method to canInteractWith due to conflicts with Container
    */
   public boolean stillValid(Player pPlayer) {
      if (this.isRemoved()) {
         return false;
      } else {
         return !(pPlayer.distanceToSqr(this) > 64.0D);
      }
   }

   public void remove(Entity.RemovalReason pReason) {
      if (!this.level.isClientSide && pReason.shouldDestroy()) {
         Containers.dropContents(this.level, this, this);
      }

      super.remove(pReason);
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      if (this.lootTable != null) {
         pCompound.putString("LootTable", this.lootTable.toString());
         if (this.lootTableSeed != 0L) {
            pCompound.putLong("LootTableSeed", this.lootTableSeed);
         }
      } else {
         ContainerHelper.saveAllItems(pCompound, this.itemStacks);
      }

   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   protected void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
      if (pCompound.contains("LootTable", 8)) {
         this.lootTable = new ResourceLocation(pCompound.getString("LootTable"));
         this.lootTableSeed = pCompound.getLong("LootTableSeed");
      } else {
         ContainerHelper.loadAllItems(pCompound, this.itemStacks);
      }

   }

   public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
      InteractionResult ret = super.interact(pPlayer, pHand);
      if (ret.consumesAction()) return ret;
      pPlayer.openMenu(this);
      if (!pPlayer.level.isClientSide) {
         this.gameEvent(GameEvent.CONTAINER_OPEN, pPlayer);
         PiglinAi.angerNearbyPiglins(pPlayer, true);
         return InteractionResult.CONSUME;
      } else {
         return InteractionResult.SUCCESS;
      }
   }

   protected void applyNaturalSlowdown() {
      float f = 0.98F;
      if (this.lootTable == null) {
         int i = 15 - AbstractContainerMenu.getRedstoneSignalFromContainer(this);
         f += (float)i * 0.001F;
      }

      if (this.isInWater()) {
         f *= 0.95F;
      }

      this.setDeltaMovement(this.getDeltaMovement().multiply((double)f, 0.0D, (double)f));
   }

   /**
    * Adds loot to the minecart's contents.
    */
   public void unpackLootTable(@Nullable Player pPlayer) {
      if (this.lootTable != null && this.level.getServer() != null) {
         LootTable loottable = this.level.getServer().getLootTables().get(this.lootTable);
         if (pPlayer instanceof ServerPlayer) {
            CriteriaTriggers.GENERATE_LOOT.trigger((ServerPlayer)pPlayer, this.lootTable);
         }

         this.lootTable = null;
         LootContext.Builder lootcontext$builder = (new LootContext.Builder((ServerLevel)this.level)).withParameter(LootContextParams.ORIGIN, this.position()).withOptionalRandomSeed(this.lootTableSeed);
         // Forge: add this entity to loot context, however, currently Vanilla uses 'this' for the player creating the chests. So we take over 'killer_entity' for this.
         lootcontext$builder.withParameter(LootContextParams.KILLER_ENTITY, this);
         if (pPlayer != null) {
            lootcontext$builder.withLuck(pPlayer.getLuck()).withParameter(LootContextParams.THIS_ENTITY, pPlayer);
         }

         loottable.fill(this, lootcontext$builder.create(LootContextParamSets.CHEST));
      }

   }

   public void clearContent() {
      this.unpackLootTable((Player)null);
      this.itemStacks.clear();
   }

   public void setLootTable(ResourceLocation pLootTable, long pLootTableSeed) {
      this.lootTable = pLootTable;
      this.lootTableSeed = pLootTableSeed;
   }

   @Nullable
   public AbstractContainerMenu createMenu(int pContainerId, Inventory pInventory, Player pPlayer) {
      if (this.lootTable != null && pPlayer.isSpectator()) {
         return null;
      } else {
         this.unpackLootTable(pInventory.player);
         return this.createMenu(pContainerId, pInventory);
      }
   }

   protected abstract AbstractContainerMenu createMenu(int pId, Inventory pPlayerInventory);

   // Forge Start
   private net.minecraftforge.common.util.LazyOptional<?> itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this));

   @Override
   public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable net.minecraft.core.Direction facing) {
      if (this.isAlive() && capability == net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
         return itemHandler.cast();
      return super.getCapability(capability, facing);
   }

   @Override
   public void invalidateCaps() {
      super.invalidateCaps();
      itemHandler.invalidate();
   }

   @Override
   public void reviveCaps() {
      super.reviveCaps();
      itemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.wrapper.InvWrapper(this));
   }
}
