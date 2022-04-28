package net.minecraft.world.level.levelgen.feature.trunkplacers;

import com.mojang.datafixers.Products.P3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;

public abstract class TrunkPlacer {
   public static final Codec<TrunkPlacer> CODEC = Registry.TRUNK_PLACER_TYPES.byNameCodec().dispatch(TrunkPlacer::type, TrunkPlacerType::codec);
   private static final int MAX_BASE_HEIGHT = 32;
   private static final int MAX_RAND = 24;
   public static final int MAX_HEIGHT = 80;
   protected final int baseHeight;
   protected final int heightRandA;
   protected final int heightRandB;

   protected static <P extends TrunkPlacer> P3<Mu<P>, Integer, Integer, Integer> trunkPlacerParts(Instance<P> pInstance) {
      return pInstance.group(Codec.intRange(0, 32).fieldOf("base_height").forGetter((p_70314_) -> {
         return p_70314_.baseHeight;
      }), Codec.intRange(0, 24).fieldOf("height_rand_a").forGetter((p_70312_) -> {
         return p_70312_.heightRandA;
      }), Codec.intRange(0, 24).fieldOf("height_rand_b").forGetter((p_70308_) -> {
         return p_70308_.heightRandB;
      }));
   }

   public TrunkPlacer(int pBaseHeight, int pHeightRandA, int pHeightRandB) {
      this.baseHeight = pBaseHeight;
      this.heightRandA = pHeightRandA;
      this.heightRandB = pHeightRandB;
   }

   protected abstract TrunkPlacerType<?> type();

   public abstract List<FoliagePlacer.FoliageAttachment> placeTrunk(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, int pFreeTreeHeight, BlockPos pPos, TreeConfiguration pConfig);

   public int getTreeHeight(Random pRandom) {
      return this.baseHeight + pRandom.nextInt(this.heightRandA + 1) + pRandom.nextInt(this.heightRandB + 1);
   }

   private static boolean isDirt(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_70304_) -> {
         return Feature.isDirt(p_70304_) && !p_70304_.is(Blocks.GRASS_BLOCK) && !p_70304_.is(Blocks.MYCELIUM);
      });
   }

   protected static void setDirtAt(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, BlockPos pPos, TreeConfiguration pConfig) {
      if (pConfig.forceDirt || !isDirt(pLevel, pPos)) {
         pBlockSetter.accept(pPos, pConfig.dirtProvider.getState(pRandom, pPos));
      }

   }

   protected static boolean placeLog(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, BlockPos pPos, TreeConfiguration pConfig) {
      return placeLog(pLevel, pBlockSetter, pRandom, pPos, pConfig, Function.identity());
   }

   protected static boolean placeLog(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, BlockPos pPos, TreeConfiguration pConfig, Function<BlockState, BlockState> pPropertySetter) {
      if (TreeFeature.validTreePos(pLevel, pPos)) {
         pBlockSetter.accept(pPos, pPropertySetter.apply(pConfig.trunkProvider.getState(pRandom, pPos)));
         return true;
      } else {
         return false;
      }
   }

   protected static void placeLogIfFree(LevelSimulatedReader pLevel, BiConsumer<BlockPos, BlockState> pBlockSetter, Random pRandom, BlockPos.MutableBlockPos pPos, TreeConfiguration pConfig) {
      if (TreeFeature.isFree(pLevel, pPos)) {
         placeLog(pLevel, pBlockSetter, pRandom, pPos, pConfig);
      }

   }
}