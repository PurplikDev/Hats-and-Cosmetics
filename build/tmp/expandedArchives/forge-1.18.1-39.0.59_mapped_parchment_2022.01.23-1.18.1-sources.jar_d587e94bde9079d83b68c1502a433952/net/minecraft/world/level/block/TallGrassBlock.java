package net.minecraft.world.level.block;

import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TallGrassBlock extends BushBlock implements BonemealableBlock, net.minecraftforge.common.IForgeShearable {
   protected static final float AABB_OFFSET = 6.0F;
   protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

   public TallGrassBlock(BlockBehaviour.Properties p_57318_) {
      super(p_57318_);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   /**
    * @return whether bonemeal can be used on this block
    */
   public boolean isValidBonemealTarget(BlockGetter pLevel, BlockPos pPos, BlockState pState, boolean pIsClient) {
      return true;
   }

   public boolean isBonemealSuccess(Level pLevel, Random pRandom, BlockPos pPos, BlockState pState) {
      return true;
   }

   public void performBonemeal(ServerLevel pLevel, Random pRandom, BlockPos pPos, BlockState pState) {
      DoublePlantBlock doubleplantblock = (DoublePlantBlock)(pState.is(Blocks.FERN) ? Blocks.LARGE_FERN : Blocks.TALL_GRASS);
      if (doubleplantblock.defaultBlockState().canSurvive(pLevel, pPos) && pLevel.isEmptyBlock(pPos.above())) {
         DoublePlantBlock.placeAt(pLevel, doubleplantblock.defaultBlockState(), pPos, 2);
      }

   }

   /**
    * Get the OffsetType for this Block. Determines if the model is rendered slightly offset.
    */
   public BlockBehaviour.OffsetType getOffsetType() {
      return BlockBehaviour.OffsetType.XYZ;
   }
}
