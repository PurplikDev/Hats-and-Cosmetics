package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public interface FeatureAccess {
   @Nullable
   StructureStart<?> getStartForFeature(StructureFeature<?> pStructure);

   void setStartForFeature(StructureFeature<?> pStructure, StructureStart<?> pStart);

   LongSet getReferencesForFeature(StructureFeature<?> pStructure);

   void addReferenceForFeature(StructureFeature<?> pStructure, long pReference);

   Map<StructureFeature<?>, LongSet> getAllReferences();

   void setAllReferences(Map<StructureFeature<?>, LongSet> pStructureReferences);
}