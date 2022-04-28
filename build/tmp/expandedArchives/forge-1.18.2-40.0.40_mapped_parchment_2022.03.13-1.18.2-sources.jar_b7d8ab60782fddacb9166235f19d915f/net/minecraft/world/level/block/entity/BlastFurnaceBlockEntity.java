package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;

public class BlastFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
   public BlastFurnaceBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
      super(BlockEntityType.BLAST_FURNACE, pWorldPosition, pBlockState, RecipeType.BLASTING);
   }

   protected Component getDefaultName() {
      return new TranslatableComponent("container.blast_furnace");
   }

   protected int getBurnDuration(ItemStack pFuel) {
      return super.getBurnDuration(pFuel) / 2;
   }

   protected AbstractContainerMenu createMenu(int pId, Inventory pPlayer) {
      return new BlastFurnaceMenu(pId, pPlayer, this, this.dataAccess);
   }
}