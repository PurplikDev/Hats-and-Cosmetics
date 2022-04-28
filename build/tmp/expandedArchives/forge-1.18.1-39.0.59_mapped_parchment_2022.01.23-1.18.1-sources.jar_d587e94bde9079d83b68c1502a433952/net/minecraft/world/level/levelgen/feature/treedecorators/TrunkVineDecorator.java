package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class TrunkVineDecorator extends TreeDecorator {
   public static final Codec<TrunkVineDecorator> CODEC;
   public static final TrunkVineDecorator INSTANCE = new TrunkVineDecorator();

   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.TRUNK_VINE;
   }

   public void place(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, List<BlockPos> pLogPositions, List<BlockPos> pLeafPositions) {
      pLogPositions.forEach((p_161764_) -> {
         if (pRandom.nextInt(3) > 0) {
            BlockPos blockpos = p_161764_.west();
            if (Feature.isAir(pLevel, blockpos)) {
               placeVine(pBlockSetter, blockpos, VineBlock.EAST);
            }
         }

         if (pRandom.nextInt(3) > 0) {
            BlockPos blockpos1 = p_161764_.east();
            if (Feature.isAir(pLevel, blockpos1)) {
               placeVine(pBlockSetter, blockpos1, VineBlock.WEST);
            }
         }

         if (pRandom.nextInt(3) > 0) {
            BlockPos blockpos2 = p_161764_.north();
            if (Feature.isAir(pLevel, blockpos2)) {
               placeVine(pBlockSetter, blockpos2, VineBlock.SOUTH);
            }
         }

         if (pRandom.nextInt(3) > 0) {
            BlockPos blockpos3 = p_161764_.south();
            if (Feature.isAir(pLevel, blockpos3)) {
               placeVine(pBlockSetter, blockpos3, VineBlock.NORTH);
            }
         }

      });
   }

   static {
      CODEC = Codec.unit(() -> {
         return INSTANCE;
      });
   }
}