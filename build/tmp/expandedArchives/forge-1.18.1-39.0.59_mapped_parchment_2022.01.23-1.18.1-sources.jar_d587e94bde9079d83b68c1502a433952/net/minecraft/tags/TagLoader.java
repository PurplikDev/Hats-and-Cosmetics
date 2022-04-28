package net.minecraft.tags;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TagLoader<T> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = new Gson();
   private static final String PATH_SUFFIX = ".json";
   private static final int PATH_SUFFIX_LENGTH = ".json".length();
   private final Function<ResourceLocation, Optional<T>> idToValue;
   private final String directory;

   public TagLoader(Function<ResourceLocation, Optional<T>> pIdToValue, String pDirectory) {
      this.idToValue = pIdToValue;
      this.directory = pDirectory;
   }

   public Map<ResourceLocation, Tag.Builder> load(ResourceManager pResourceManager) {
      Map<ResourceLocation, Tag.Builder> map = Maps.newHashMap();

      for(ResourceLocation resourcelocation : pResourceManager.listResources(this.directory, (p_144506_) -> {
         return p_144506_.endsWith(".json");
      })) {
         String s = resourcelocation.getPath();
         ResourceLocation resourcelocation1 = new ResourceLocation(resourcelocation.getNamespace(), s.substring(this.directory.length() + 1, s.length() - PATH_SUFFIX_LENGTH));

         try {
            for(Resource resource : pResourceManager.getResources(resourcelocation)) {
               try {
                  InputStream inputstream = resource.getInputStream();

                  try {
                     Reader reader = new BufferedReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));

                     try {
                        JsonObject jsonobject = GsonHelper.fromJson(GSON, reader, JsonObject.class);
                        if (jsonobject == null) {
                           LOGGER.error("Couldn't load tag list {} from {} in data pack {} as it is empty or null", resourcelocation1, resourcelocation, resource.getSourceName());
                        } else {
                           map.computeIfAbsent(resourcelocation1, (p_144555_) -> {
                              return Tag.Builder.tag();
                           }).addFromJson(jsonobject, resource.getSourceName());
                        }
                     } catch (Throwable throwable2) {
                        try {
                           reader.close();
                        } catch (Throwable throwable1) {
                           throwable2.addSuppressed(throwable1);
                        }

                        throw throwable2;
                     }

                     reader.close();
                  } catch (Throwable throwable3) {
                     if (inputstream != null) {
                        try {
                           inputstream.close();
                        } catch (Throwable throwable) {
                           throwable3.addSuppressed(throwable);
                        }
                     }

                     throw throwable3;
                  }

                  if (inputstream != null) {
                     inputstream.close();
                  }
               } catch (RuntimeException | IOException ioexception) {
                  LOGGER.error("Couldn't read tag list {} from {} in data pack {}", resourcelocation1, resourcelocation, resource.getSourceName(), ioexception);
               } finally {
                  IOUtils.closeQuietly((Closeable)resource);
               }
            }
         } catch (IOException ioexception1) {
            LOGGER.error("Couldn't read tag list {} from {}", resourcelocation1, resourcelocation, ioexception1);
         }
      }

      return map;
   }

   private static void visitDependenciesAndElement(Map<ResourceLocation, Tag.Builder> pBuilders, Multimap<ResourceLocation, ResourceLocation> pDependencyNames, Set<ResourceLocation> pNames, ResourceLocation pName, BiConsumer<ResourceLocation, Tag.Builder> pVisitor) {
      if (pNames.add(pName)) {
         pDependencyNames.get(pName).forEach((p_144514_) -> {
            visitDependenciesAndElement(pBuilders, pDependencyNames, pNames, p_144514_, pVisitor);
         });
         Tag.Builder tag$builder = pBuilders.get(pName);
         if (tag$builder != null) {
            pVisitor.accept(pName, tag$builder);
         }

      }
   }

   private static boolean isCyclic(Multimap<ResourceLocation, ResourceLocation> pDependencyNames, ResourceLocation pName, ResourceLocation pDependencyName) {
      Collection<ResourceLocation> collection = pDependencyNames.get(pDependencyName);
      return collection.contains(pName) ? true : collection.stream().anyMatch((p_144567_) -> {
         return isCyclic(pDependencyNames, pName, p_144567_);
      });
   }

   private static void addDependencyIfNotCyclic(Multimap<ResourceLocation, ResourceLocation> pDependencyNames, ResourceLocation pName, ResourceLocation pDependencyName) {
      if (!isCyclic(pDependencyNames, pName, pDependencyName)) {
         pDependencyNames.put(pName, pDependencyName);
      }

   }

   public TagCollection<T> build(Map<ResourceLocation, Tag.Builder> pBuilders) {
      Map<ResourceLocation, Tag<T>> map = Maps.newHashMap();
      Function<ResourceLocation, Tag<T>> function = map::get;
      Function<ResourceLocation, T> function1 = (p_144540_) -> {
         return this.idToValue.apply(p_144540_).orElse((T)null);
      };
      Multimap<ResourceLocation, ResourceLocation> multimap = HashMultimap.create();
      pBuilders.forEach((p_144548_, p_144549_) -> {
         p_144549_.visitRequiredDependencies((p_144563_) -> {
            addDependencyIfNotCyclic(multimap, p_144548_, p_144563_);
         });
      });
      pBuilders.forEach((p_144499_, p_144500_) -> {
         p_144500_.visitOptionalDependencies((p_144559_) -> {
            addDependencyIfNotCyclic(multimap, p_144499_, p_144559_);
         });
      });
      Set<ResourceLocation> set = Sets.newHashSet();
      pBuilders.keySet().forEach((p_144522_) -> {
         visitDependenciesAndElement(pBuilders, multimap, set, p_144522_, (p_144537_, p_144538_) -> {
            p_144538_.build(function, function1).ifLeft((p_144543_) -> {
               LOGGER.error("Couldn't load tag {} as it is missing following references: {}", p_144537_, p_144543_.stream().map(Objects::toString).collect(Collectors.joining(",")));
            }).ifRight((p_144532_) -> {
               map.put(p_144537_, p_144532_);
            });
         });
      });
      return TagCollection.of(map);
   }

   public TagCollection<T> loadAndBuild(ResourceManager pResourceManager) {
      return this.build(this.load(pResourceManager));
   }
}