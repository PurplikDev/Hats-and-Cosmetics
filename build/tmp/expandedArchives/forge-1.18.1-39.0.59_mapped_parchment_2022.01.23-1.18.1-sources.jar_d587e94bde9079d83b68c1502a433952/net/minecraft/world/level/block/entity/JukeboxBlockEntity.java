package net.minecraft.world.level.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class JukeboxBlockEntity extends BlockEntity implements Clearable {
   private ItemStack record = ItemStack.EMPTY;

   public JukeboxBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
      super(BlockEntityType.JUKEBOX, pWorldPosition, pBlockState);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      if (pTag.contains("RecordItem", 10)) {
         this.setRecord(ItemStack.of(pTag.getCompound("RecordItem")));
      }

   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      if (!this.getRecord().isEmpty()) {
         pTag.put("RecordItem", this.getRecord().save(new CompoundTag()));
      }

   }

   public ItemStack getRecord() {
      return this.record;
   }

   public void setRecord(ItemStack pRecord) {
      this.record = pRecord;
      this.setChanged();
   }

   public void clearContent() {
      this.setRecord(ItemStack.EMPTY);
   }
}