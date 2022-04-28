package net.minecraft.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DeadBushBlock extends BushBlock implements net.minecraftforge.common.IForgeShearable {
   protected static final float AABB_OFFSET = 6.0F;
   protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

   public DeadBushBlock(BlockBehaviour.Properties p_52417_) {
      super(p_52417_);
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return SHAPE;
   }

   protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return pState.is(Blocks.SAND) || pState.is(Blocks.RED_SAND) || pState.is(Blocks.TERRACOTTA) || pState.is(Blocks.WHITE_TERRACOTTA) || pState.is(Blocks.ORANGE_TERRACOTTA) || pState.is(Blocks.MAGENTA_TERRACOTTA) || pState.is(Blocks.LIGHT_BLUE_TERRACOTTA) || pState.is(Blocks.YELLOW_TERRACOTTA) || pState.is(Blocks.LIME_TERRACOTTA) || pState.is(Blocks.PINK_TERRACOTTA) || pState.is(Blocks.GRAY_TERRACOTTA) || pState.is(Blocks.LIGHT_GRAY_TERRACOTTA) || pState.is(Blocks.CYAN_TERRACOTTA) || pState.is(Blocks.PURPLE_TERRACOTTA) || pState.is(Blocks.BLUE_TERRACOTTA) || pState.is(Blocks.BROWN_TERRACOTTA) || pState.is(Blocks.GREEN_TERRACOTTA) || pState.is(Blocks.RED_TERRACOTTA) || pState.is(Blocks.BLACK_TERRACOTTA) || pState.is(BlockTags.DIRT);
   }
}
