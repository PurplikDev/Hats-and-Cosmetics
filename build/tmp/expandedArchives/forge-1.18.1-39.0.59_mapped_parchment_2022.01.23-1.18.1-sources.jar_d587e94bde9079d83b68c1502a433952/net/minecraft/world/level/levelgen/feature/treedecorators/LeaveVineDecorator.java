package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.feature.Feature;

public class LeaveVineDecorator extends TreeDecorator {
   public static final Codec<LeaveVineDecorator> CODEC;
   public static final LeaveVineDecorator INSTANCE = new LeaveVineDecorator();

   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.LEAVE_VINE;
   }

   public void place(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, List<BlockPos> pLogPositions, List<BlockPos> pLeafPositions) {
      pLeafPositions.forEach((p_161744_) -> {
         if (pRandom.nextInt(4) == 0) {
            BlockPos blockpos = p_161744_.west();
            if (Feature.isAir(pLevel, blockpos)) {
               addHangingVine(pLevel, blockpos, VineBlock.EAST, pBlockSetter);
            }
         }

         if (pRandom.nextInt(4) == 0) {
            BlockPos blockpos1 = p_161744_.east();
            if (Feature.isAir(pLevel, blockpos1)) {
               addHangingVine(pLevel, blockpos1, VineBlock.WEST, pBlockSetter);
            }
         }

         if (pRandom.nextInt(4) == 0) {
            BlockPos blockpos2 = p_161744_.north();
            if (Feature.isAir(pLevel, blockpos2)) {
               addHangingVine(pLevel, blockpos2, VineBlock.SOUTH, pBlockSetter);
            }
         }

         if (pRandom.nextInt(4) == 0) {
            BlockPos blockpos3 = p_161744_.south();
            if (Feature.isAir(pLevel, blockpos3)) {
               addHangingVine(pLevel, blockpos3, VineBlock.NORTH, pBlockSetter);
            }
         }

      });
   }

   private static void addHangingVine(LevelSimulatedReader pLevel, BlockPos pPos, BooleanProperty pSideProperty, BiConsumer<BlockPos, BlockState> pBlockSetter) {
      placeVine(pBlockSetter, pPos, pSideProperty);
      int i = 4;

      for(BlockPos blockpos = pPos.below(); Feature.isAir(pLevel, blockpos) && i > 0; --i) {
         placeVine(pBlockSetter, blockpos, pSideProperty);
         blockpos = blockpos.below();
      }

   }

   static {
      CODEC = Codec.unit(() -> {
         return INSTANCE;
      });
   }
}