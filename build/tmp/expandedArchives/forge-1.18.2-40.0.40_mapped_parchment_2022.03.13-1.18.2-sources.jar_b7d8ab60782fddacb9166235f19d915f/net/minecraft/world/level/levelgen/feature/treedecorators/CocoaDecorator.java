package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class CocoaDecorator extends TreeDecorator {
   public static final Codec<CocoaDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(CocoaDecorator::new, (p_69989_) -> {
      return p_69989_.probability;
   }).codec();
   private final float probability;

   public CocoaDecorator(float p_69976_) {
      this.probability = p_69976_;
   }

   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.COCOA;
   }

   public void place(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, List<BlockPos> pLogPositions, List<BlockPos> pLeafPositions) {
      if (!(pRandom.nextFloat() >= this.probability)) {
         int i = pLogPositions.get(0).getY();
         pLogPositions.stream().filter((p_69980_) -> {
            return p_69980_.getY() - i <= 2;
         }).forEach((p_161728_) -> {
            for(Direction direction : Direction.Plane.HORIZONTAL) {
               if (pRandom.nextFloat() <= 0.25F) {
                  Direction direction1 = direction.getOpposite();
                  BlockPos blockpos = p_161728_.offset(direction1.getStepX(), 0, direction1.getStepZ());
                  if (Feature.isAir(pLevel, blockpos)) {
                     pBlockSetter.accept(blockpos, Blocks.COCOA.defaultBlockState().setValue(CocoaBlock.AGE, Integer.valueOf(pRandom.nextInt(3))).setValue(CocoaBlock.FACING, direction));
                  }
               }
            }

         });
      }
   }
}