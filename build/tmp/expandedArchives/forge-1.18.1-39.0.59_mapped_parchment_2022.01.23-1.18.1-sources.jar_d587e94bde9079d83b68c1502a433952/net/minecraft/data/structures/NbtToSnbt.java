package net.minecraft.data.structures;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NbtToSnbt implements DataProvider {
   private static final Logger LOGGER = LogManager.getLogger();
   private final DataGenerator generator;

   public NbtToSnbt(DataGenerator pGenerator) {
      this.generator = pGenerator;
   }

   /**
    * Performs this provider's action.
    */
   public void run(HashCache pCache) throws IOException {
      Path path = this.generator.getOutputFolder();

      for(Path path1 : this.generator.getInputFolders()) {
         Files.walk(path1).filter((p_126430_) -> {
            return p_126430_.toString().endsWith(".nbt");
         }).forEach((p_126441_) -> {
            convertStructure(p_126441_, this.getName(path1, p_126441_), path);
         });
      }

   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "NBT to SNBT";
   }

   /**
    * Gets the name of the given NBT file, based on its path and the input directory. The result does not have the
    * ".nbt" extension.
    */
   private String getName(Path pInputFolder, Path pFile) {
      String s = pInputFolder.relativize(pFile).toString().replaceAll("\\\\", "/");
      return s.substring(0, s.length() - ".nbt".length());
   }

   @Nullable
   public static Path convertStructure(Path pSnbtPath, String pName, Path pNbtPath) {
      try {
         writeSnbt(pNbtPath.resolve(pName + ".snbt"), NbtUtils.structureToSnbt(NbtIo.readCompressed(Files.newInputStream(pSnbtPath))));
         LOGGER.info("Converted {} from NBT to SNBT", (Object)pName);
         return pNbtPath.resolve(pName + ".snbt");
      } catch (IOException ioexception) {
         LOGGER.error("Couldn't convert {} from NBT to SNBT at {}", pName, pSnbtPath, ioexception);
         return null;
      }
   }

   public static void writeSnbt(Path pPath, String pContents) throws IOException {
      Files.createDirectories(pPath.getParent());
      BufferedWriter bufferedwriter = Files.newBufferedWriter(pPath);

      try {
         bufferedwriter.write(pContents);
         bufferedwriter.write(10);
      } catch (Throwable throwable1) {
         if (bufferedwriter != null) {
            try {
               bufferedwriter.close();
            } catch (Throwable throwable) {
               throwable1.addSuppressed(throwable);
            }
         }

         throw throwable1;
      }

      if (bufferedwriter != null) {
         bufferedwriter.close();
      }

   }
}