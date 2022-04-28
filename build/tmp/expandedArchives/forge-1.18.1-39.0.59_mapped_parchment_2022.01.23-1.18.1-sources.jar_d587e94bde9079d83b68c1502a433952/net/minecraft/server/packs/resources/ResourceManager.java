package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;

public interface ResourceManager extends ResourceProvider {
   Set<String> getNamespaces();

   boolean hasResource(ResourceLocation pPath);

   List<Resource> getResources(ResourceLocation pResourceLocation) throws IOException;

   Collection<ResourceLocation> listResources(String pPath, Predicate<String> pFilter);

   Stream<PackResources> listPacks();

   public static enum Empty implements ResourceManager {
      INSTANCE;

      public Set<String> getNamespaces() {
         return ImmutableSet.of();
      }

      public Resource getResource(ResourceLocation p_10742_) throws IOException {
         throw new FileNotFoundException(p_10742_.toString());
      }

      public boolean hasResource(ResourceLocation pPath) {
         return false;
      }

      public List<Resource> getResources(ResourceLocation pResourceLocation) {
         return ImmutableList.of();
      }

      public Collection<ResourceLocation> listResources(String pPath, Predicate<String> pFilter) {
         return ImmutableSet.of();
      }

      public Stream<PackResources> listPacks() {
         return Stream.of();
      }
   }
}