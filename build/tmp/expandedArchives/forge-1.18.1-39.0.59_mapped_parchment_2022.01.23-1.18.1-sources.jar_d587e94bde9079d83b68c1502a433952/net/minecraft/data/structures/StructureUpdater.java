package net.minecraft.data.structures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StructureUpdater implements SnbtToNbt.Filter {
   private static final Logger LOGGER = LogManager.getLogger();

   public CompoundTag apply(String pStructureLocationPath, CompoundTag pTag) {
      return pStructureLocationPath.startsWith("data/minecraft/structures/") ? update(pStructureLocationPath, pTag) : pTag;
   }

   public static CompoundTag update(String pStructureLocationPath, CompoundTag pTag) {
      return updateStructure(pStructureLocationPath, patchVersion(pTag));
   }

   private static CompoundTag patchVersion(CompoundTag pTag) {
      if (!pTag.contains("DataVersion", 99)) {
         pTag.putInt("DataVersion", 500);
      }

      return pTag;
   }

   private static CompoundTag updateStructure(String pStructureLocationPath, CompoundTag pTag) {
      StructureTemplate structuretemplate = new StructureTemplate();
      int i = pTag.getInt("DataVersion");
      int j = 2830;
      if (i < 2830) {
         LOGGER.warn("SNBT Too old, do not forget to update: {} < {}: {}", i, 2830, pStructureLocationPath);
      }

      CompoundTag compoundtag = NbtUtils.update(DataFixers.getDataFixer(), DataFixTypes.STRUCTURE, pTag, i);
      structuretemplate.load(compoundtag);
      return structuretemplate.save(new CompoundTag());
   }
}