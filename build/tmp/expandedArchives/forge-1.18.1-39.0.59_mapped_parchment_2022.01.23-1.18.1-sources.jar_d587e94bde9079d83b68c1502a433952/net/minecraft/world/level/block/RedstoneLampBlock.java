package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class RedstoneLampBlock extends Block {
   public static final BooleanProperty LIT = RedstoneTorchBlock.LIT;

   public RedstoneLampBlock(BlockBehaviour.Properties p_55657_) {
      super(p_55657_);
      this.registerDefaultState(this.defaultBlockState().setValue(LIT, Boolean.valueOf(false)));
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      return this.defaultBlockState().setValue(LIT, Boolean.valueOf(pContext.getLevel().hasNeighborSignal(pContext.getClickedPos())));
   }

   public void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
      if (!pLevel.isClientSide) {
         boolean flag = pState.getValue(LIT);
         if (flag != pLevel.hasNeighborSignal(pPos)) {
            if (flag) {
               pLevel.scheduleTick(pPos, this, 4);
            } else {
               pLevel.setBlock(pPos, pState.cycle(LIT), 2);
            }
         }

      }
   }

   public void tick(BlockState pState, ServerLevel pLevel, BlockPos pPos, Random pRand) {
      if (pState.getValue(LIT) && !pLevel.hasNeighborSignal(pPos)) {
         pLevel.setBlock(pPos, pState.cycle(LIT), 2);
      }

   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      pBuilder.add(LIT);
   }
}