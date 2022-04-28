package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface FeatureAccess {
   @Nullable
   StructureStart getStartForFeature(ConfiguredStructureFeature<?, ?> pStructure);

   void setStartForFeature(ConfiguredStructureFeature<?, ?> pStructure, StructureStart pStart);

   LongSet getReferencesForFeature(ConfiguredStructureFeature<?, ?> pStructure);

   void addReferenceForFeature(ConfiguredStructureFeature<?, ?> pStructure, long pReference);

   Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllReferences();

   void setAllReferences(Map<ConfiguredStructureFeature<?, ?>, LongSet> pStructureReferences);
}