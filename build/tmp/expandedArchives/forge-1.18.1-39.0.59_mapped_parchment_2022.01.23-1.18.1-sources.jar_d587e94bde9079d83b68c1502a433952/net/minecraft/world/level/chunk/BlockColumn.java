package net.minecraft.world.level.chunk;

import net.minecraft.world.level.block.state.BlockState;

public interface BlockColumn {
   BlockState getBlock(int pPos);

   void setBlock(int p_187574_, BlockState p_187575_);
}