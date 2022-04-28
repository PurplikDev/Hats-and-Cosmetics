package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StructureManager {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String STRUCTURE_DIRECTORY_NAME = "structures";
   private static final String STRUCTURE_FILE_EXTENSION = ".nbt";
   private static final String STRUCTURE_TEXT_FILE_EXTENSION = ".snbt";
   private final Map<ResourceLocation, Optional<StructureTemplate>> structureRepository = Maps.newConcurrentMap();
   private final DataFixer fixerUpper;
   private ResourceManager resourceManager;
   private final Path generatedDir;

   public StructureManager(ResourceManager pResourceManager, LevelStorageSource.LevelStorageAccess pLevelStorage, DataFixer pFixerUpper) {
      this.resourceManager = pResourceManager;
      this.fixerUpper = pFixerUpper;
      this.generatedDir = pLevelStorage.getLevelPath(LevelResource.GENERATED_DIR).normalize();
   }

   public StructureTemplate getOrCreate(ResourceLocation pId) {
      Optional<StructureTemplate> optional = this.get(pId);
      if (optional.isPresent()) {
         return optional.get();
      } else {
         StructureTemplate structuretemplate = new StructureTemplate();
         this.structureRepository.put(pId, Optional.of(structuretemplate));
         return structuretemplate;
      }
   }

   public Optional<StructureTemplate> get(ResourceLocation pId) {
      return this.structureRepository.computeIfAbsent(pId, (p_163781_) -> {
         Optional<StructureTemplate> optional = this.loadFromGenerated(p_163781_);
         return optional.isPresent() ? optional : this.loadFromResource(p_163781_);
      });
   }

   public void onResourceManagerReload(ResourceManager pResourceManager) {
      this.resourceManager = pResourceManager;
      this.structureRepository.clear();
   }

   private Optional<StructureTemplate> loadFromResource(ResourceLocation pId) {
      ResourceLocation resourcelocation = new ResourceLocation(pId.getNamespace(), "structures/" + pId.getPath() + ".nbt");

      try {
         Resource resource = this.resourceManager.getResource(resourcelocation);

         Optional optional;
         try {
            optional = Optional.of(this.readStructure(resource.getInputStream()));
         } catch (Throwable throwable1) {
            if (resource != null) {
               try {
                  resource.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (resource != null) {
            resource.close();
         }

         return optional;
      } catch (FileNotFoundException filenotfoundexception) {
         return Optional.empty();
      } catch (Throwable throwable2) {
         LOGGER.error("Couldn't load structure {}: {}", pId, throwable2.toString());
         return Optional.empty();
      }
   }

   private Optional<StructureTemplate> loadFromGenerated(ResourceLocation pId) {
      if (!this.generatedDir.toFile().isDirectory()) {
         return Optional.empty();
      } else {
         Path path = this.createAndValidatePathToStructure(pId, ".nbt");

         try {
            InputStream inputstream = new FileInputStream(path.toFile());

            Optional optional;
            try {
               optional = Optional.of(this.readStructure(inputstream));
            } catch (Throwable throwable1) {
               try {
                  inputstream.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }

               throw throwable1;
            }

            inputstream.close();
            return optional;
         } catch (FileNotFoundException filenotfoundexception) {
            return Optional.empty();
         } catch (IOException ioexception) {
            LOGGER.error("Couldn't load structure from {}", path, ioexception);
            return Optional.empty();
         }
      }
   }

   private StructureTemplate readStructure(InputStream pStream) throws IOException {
      CompoundTag compoundtag = NbtIo.readCompressed(pStream);
      return this.readStructure(compoundtag);
   }

   public StructureTemplate readStructure(CompoundTag pTag) {
      if (!pTag.contains("DataVersion", 99)) {
         pTag.putInt("DataVersion", 500);
      }

      StructureTemplate structuretemplate = new StructureTemplate();
      structuretemplate.load(NbtUtils.update(this.fixerUpper, DataFixTypes.STRUCTURE, pTag, pTag.getInt("DataVersion")));
      return structuretemplate;
   }

   public boolean save(ResourceLocation pId) {
      Optional<StructureTemplate> optional = this.structureRepository.get(pId);
      if (!optional.isPresent()) {
         return false;
      } else {
         StructureTemplate structuretemplate = optional.get();
         Path path = this.createAndValidatePathToStructure(pId, ".nbt");
         Path path1 = path.getParent();
         if (path1 == null) {
            return false;
         } else {
            try {
               Files.createDirectories(Files.exists(path1) ? path1.toRealPath() : path1);
            } catch (IOException ioexception) {
               LOGGER.error("Failed to create parent directory: {}", (Object)path1);
               return false;
            }

            CompoundTag compoundtag = structuretemplate.save(new CompoundTag());

            try {
               OutputStream outputstream = new FileOutputStream(path.toFile());

               try {
                  NbtIo.writeCompressed(compoundtag, outputstream);
               } catch (Throwable throwable1) {
                  try {
                     outputstream.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }

                  throw throwable1;
               }

               outputstream.close();
               return true;
            } catch (Throwable throwable2) {
               return false;
            }
         }
      }
   }

   public Path createPathToStructure(ResourceLocation pId, String pExtension) {
      try {
         Path path = this.generatedDir.resolve(pId.getNamespace());
         Path path1 = path.resolve("structures");
         return FileUtil.createPathToResource(path1, pId.getPath(), pExtension);
      } catch (InvalidPathException invalidpathexception) {
         throw new ResourceLocationException("Invalid resource path: " + pId, invalidpathexception);
      }
   }

   private Path createAndValidatePathToStructure(ResourceLocation pId, String pExtension) {
      if (pId.getPath().contains("//")) {
         throw new ResourceLocationException("Invalid resource path: " + pId);
      } else {
         Path path = this.createPathToStructure(pId, pExtension);
         if (path.startsWith(this.generatedDir) && FileUtil.isPathNormalized(path) && FileUtil.isPathPortable(path)) {
            return path;
         } else {
            throw new ResourceLocationException("Invalid resource path: " + path);
         }
      }
   }

   public void remove(ResourceLocation pId) {
      this.structureRepository.remove(pId);
   }
}