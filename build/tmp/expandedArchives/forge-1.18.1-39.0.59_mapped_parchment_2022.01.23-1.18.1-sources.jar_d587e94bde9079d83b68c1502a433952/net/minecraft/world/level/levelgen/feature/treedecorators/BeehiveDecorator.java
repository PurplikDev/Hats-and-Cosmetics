package net.minecraft.world.level.levelgen.feature.treedecorators;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

public class BeehiveDecorator extends TreeDecorator {
   public static final Codec<BeehiveDecorator> CODEC = Codec.floatRange(0.0F, 1.0F).fieldOf("probability").xmap(BeehiveDecorator::new, (p_69971_) -> {
      return p_69971_.probability;
   }).codec();
   /** Probability to generate a beehive */
   private final float probability;

   public BeehiveDecorator(float p_69958_) {
      this.probability = p_69958_;
   }

   protected TreeDecoratorType<?> type() {
      return TreeDecoratorType.BEEHIVE;
   }

   public void place(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, List<BlockPos> pLogPositions, List<BlockPos> pLeafPositions) {
      if (!(pRandom.nextFloat() >= this.probability)) {
         Direction direction = BeehiveBlock.getRandomOffset(pRandom);
         int i = !pLeafPositions.isEmpty() ? Math.max(pLeafPositions.get(0).getY() - 1, pLogPositions.get(0).getY()) : Math.min(pLogPositions.get(0).getY() + 1 + pRandom.nextInt(3), pLogPositions.get(pLogPositions.size() - 1).getY());
         List<BlockPos> list = pLogPositions.stream().filter((p_69962_) -> {
            return p_69962_.getY() == i;
         }).collect(Collectors.toList());
         if (!list.isEmpty()) {
            BlockPos blockpos = list.get(pRandom.nextInt(list.size()));
            BlockPos blockpos1 = blockpos.relative(direction);
            if (Feature.isAir(pLevel, blockpos1) && Feature.isAir(pLevel, blockpos1.relative(Direction.SOUTH))) {
               pBlockSetter.accept(blockpos1, Blocks.BEE_NEST.defaultBlockState().setValue(BeehiveBlock.FACING, Direction.SOUTH));
               pLevel.getBlockEntity(blockpos1, BlockEntityType.BEEHIVE).ifPresent((p_161717_) -> {
                  int j = 2 + pRandom.nextInt(2);

                  for(int k = 0; k < j; ++k) {
                     CompoundTag compoundtag = new CompoundTag();
                     compoundtag.putString("id", Registry.ENTITY_TYPE.getKey(EntityType.BEE).toString());
                     p_161717_.storeBee(compoundtag, pRandom.nextInt(599), false);
                  }

               });
            }
         }
      }
   }
}