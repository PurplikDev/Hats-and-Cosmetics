package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FallbackResourceManager implements ResourceManager {
   static final Logger LOGGER = LogManager.getLogger();
   public final List<PackResources> fallbacks = Lists.newArrayList();
   private final PackType type;
   private final String namespace;

   public FallbackResourceManager(PackType p_10605_, String p_10606_) {
      this.type = p_10605_;
      this.namespace = p_10606_;
   }

   public void add(PackResources pResourcePack) {
      this.fallbacks.add(pResourcePack);
   }

   public Set<String> getNamespaces() {
      return ImmutableSet.of(this.namespace);
   }

   public Resource getResource(ResourceLocation p_10614_) throws IOException {
      this.validateLocation(p_10614_);
      PackResources packresources = null;
      ResourceLocation resourcelocation = getMetadataLocation(p_10614_);

      for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
         PackResources packresources1 = this.fallbacks.get(i);
         if (packresources == null && packresources1.hasResource(this.type, resourcelocation)) {
            packresources = packresources1;
         }

         if (packresources1.hasResource(this.type, p_10614_)) {
            InputStream inputstream = null;
            if (packresources != null) {
               inputstream = this.getWrappedResource(resourcelocation, packresources);
            }

            return new SimpleResource(packresources1.getName(), p_10614_, this.getWrappedResource(p_10614_, packresources1), inputstream);
         }
      }

      throw new FileNotFoundException(p_10614_.toString());
   }

   public boolean hasResource(ResourceLocation pPath) {
      if (!this.isValidLocation(pPath)) {
         return false;
      } else {
         for(int i = this.fallbacks.size() - 1; i >= 0; --i) {
            PackResources packresources = this.fallbacks.get(i);
            if (packresources.hasResource(this.type, pPath)) {
               return true;
            }
         }

         return false;
      }
   }

   protected InputStream getWrappedResource(ResourceLocation pLocation, PackResources pResourcePack) throws IOException {
      InputStream inputstream = pResourcePack.getResource(this.type, pLocation);
      return (InputStream)(LOGGER.isDebugEnabled() ? new FallbackResourceManager.LeakedResourceWarningInputStream(inputstream, pLocation, pResourcePack.getName()) : inputstream);
   }

   private void validateLocation(ResourceLocation pLocation) throws IOException {
      if (!this.isValidLocation(pLocation)) {
         throw new IOException("Invalid relative path to resource: " + pLocation);
      }
   }

   private boolean isValidLocation(ResourceLocation p_10629_) {
      return !p_10629_.getPath().contains("..");
   }

   public List<Resource> getResources(ResourceLocation pResourceLocation) throws IOException {
      this.validateLocation(pResourceLocation);
      List<Resource> list = Lists.newArrayList();
      ResourceLocation resourcelocation = getMetadataLocation(pResourceLocation);

      for(PackResources packresources : this.fallbacks) {
         if (packresources.hasResource(this.type, pResourceLocation)) {
            InputStream inputstream = packresources.hasResource(this.type, resourcelocation) ? this.getWrappedResource(resourcelocation, packresources) : null;
            list.add(new SimpleResource(packresources.getName(), pResourceLocation, this.getWrappedResource(pResourceLocation, packresources), inputstream));
         }
      }

      if (list.isEmpty()) {
         throw new FileNotFoundException(pResourceLocation.toString());
      } else {
         return list;
      }
   }

   public Collection<ResourceLocation> listResources(String pPath, Predicate<String> pFilter) {
      List<ResourceLocation> list = Lists.newArrayList();

      for(PackResources packresources : this.fallbacks) {
         list.addAll(packresources.getResources(this.type, this.namespace, pPath, Integer.MAX_VALUE, pFilter));
      }

      Collections.sort(list);
      return list;
   }

   public Stream<PackResources> listPacks() {
      return this.fallbacks.stream();
   }

   static ResourceLocation getMetadataLocation(ResourceLocation pLocation) {
      return new ResourceLocation(pLocation.getNamespace(), pLocation.getPath() + ".mcmeta");
   }

   static class LeakedResourceWarningInputStream extends FilterInputStream {
      private final String message;
      private boolean closed;

      public LeakedResourceWarningInputStream(InputStream p_10633_, ResourceLocation p_10634_, String p_10635_) {
         super(p_10633_);
         ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
         (new Exception()).printStackTrace(new PrintStream(bytearrayoutputstream));
         this.message = "Leaked resource: '" + p_10634_ + "' loaded from pack: '" + p_10635_ + "'\n" + bytearrayoutputstream;
      }

      public void close() throws IOException {
         super.close();
         this.closed = true;
      }

      protected void finalize() throws Throwable {
         if (!this.closed) {
            FallbackResourceManager.LOGGER.warn(this.message);
         }

         super.finalize();
      }
   }
}