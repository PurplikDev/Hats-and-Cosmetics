package net.minecraft.world.level;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface WorldGenLevel extends ServerLevelAccessor {
   /**
    * gets the random world seed
    */
   long getSeed();

   List<? extends StructureStart<?>> startsForFeature(SectionPos pPos, StructureFeature<?> pStructure);

   default boolean ensureCanWrite(BlockPos pPos) {
      return true;
   }

   default void setCurrentlyGenerating(@Nullable Supplier<String> p_186618_) {
   }
}