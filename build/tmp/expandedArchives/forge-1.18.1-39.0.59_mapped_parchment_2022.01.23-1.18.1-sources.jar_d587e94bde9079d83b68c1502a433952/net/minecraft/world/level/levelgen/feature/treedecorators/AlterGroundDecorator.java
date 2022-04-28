package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

public class AlterGroundDecorator extends TreeDecorator {
   public static final Codec<AlterGroundDecorator> CODEC = BlockStateProvider.CODEC.fieldOf("provider").xmap(AlterGroundDecorator::new, (p_69327_) -> {
      return p_69327_.provider;
   }).codec();
   private final BlockStateProvider provider;

   public AlterGroundDecorator(BlockStateProvider p_69306_) {
      this.provider = p_69306_;
   }

   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.ALTER_GROUND;
   }

   public void place(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, List<BlockPos> pLogPositions, List<BlockPos> pLeafPositions) {
      if (!pLogPositions.isEmpty()) {
         int i = pLogPositions.get(0).getY();
         pLogPositions.stream().filter((p_69310_) -> {
            return p_69310_.getY() == i;
         }).forEach((p_161708_) -> {
            this.placeCircle(pLevel, pBlockSetter, pRandom, p_161708_.west().north());
            this.placeCircle(pLevel, pBlockSetter, pRandom, p_161708_.east(2).north());
            this.placeCircle(pLevel, pBlockSetter, pRandom, p_161708_.west().south(2));
            this.placeCircle(pLevel, pBlockSetter, pRandom, p_161708_.east(2).south(2));

            for(int j = 0; j < 5; ++j) {
               int k = pRandom.nextInt(64);
               int l = k % 8;
               int i1 = k / 8;
               if (l == 0 || l == 7 || i1 == 0 || i1 == 7) {
                  this.placeCircle(pLevel, pBlockSetter, pRandom, p_161708_.offset(-3 + l, 0, -3 + i1));
               }
            }

         });
      }
   }

   private void placeCircle(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, BlockPos pPos) {
      for(int i = -2; i <= 2; ++i) {
         for(int j = -2; j <= 2; ++j) {
            if (Math.abs(i) != 2 || Math.abs(j) != 2) {
               this.placeBlockAt(pLevel, pBlockSetter, pRandom, pPos.offset(i, 0, j));
            }
         }
      }

   }

   private void placeBlockAt(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, BlockPos pPos) {
      for(int i = 2; i >= -3; --i) {
         BlockPos blockpos = pPos.above(i);
         if (Feature.isGrassOrDirt(pLevel, blockpos)) {
            pBlockSetter.accept(blockpos, this.provider.getState(pRandom, pPos));
            break;
         }

         if (!Feature.isAir(pLevel, blockpos) && i < 0) {
            break;
         }
      }

   }
}