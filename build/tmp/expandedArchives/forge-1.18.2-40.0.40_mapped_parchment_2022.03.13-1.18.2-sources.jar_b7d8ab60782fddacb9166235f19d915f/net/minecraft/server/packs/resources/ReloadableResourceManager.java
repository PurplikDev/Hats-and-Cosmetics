package net.minecraft.server.packs.resources;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.util.Unit;
import org.slf4j.Logger;

public class ReloadableResourceManager implements ResourceManager, AutoCloseable {
   private static final Logger LOGGER = LogUtils.getLogger();
   private CloseableResourceManager resources;
   private final List<PreparableReloadListener> listeners = Lists.newArrayList();
   private final PackType type;

   public ReloadableResourceManager(PackType p_203820_) {
      this.type = p_203820_;
      this.resources = new MultiPackResourceManager(p_203820_, List.of());
   }

   public void close() {
      this.resources.close();
   }

   public void registerReloadListener(PreparableReloadListener pListener) {
      this.listeners.add(pListener);
   }

   public ReloadInstance createReload(Executor pBackgroundExecutor, Executor pGameExecutor, CompletableFuture<Unit> pWaitingFor, List<PackResources> pResourcePacks) {
      LOGGER.info("Reloading ResourceManager: {}", LogUtils.defer(() -> {
         return pResourcePacks.stream().map(PackResources::getName).collect(Collectors.joining(", "));
      }));
      this.resources.close();
      this.resources = new MultiPackResourceManager(this.type, pResourcePacks);
      return SimpleReloadInstance.create(this.resources, this.listeners, pBackgroundExecutor, pGameExecutor, pWaitingFor, LOGGER.isDebugEnabled());
   }

   public Resource getResource(ResourceLocation pResourceLocation) throws IOException {
      return this.resources.getResource(pResourceLocation);
   }

   public Set<String> getNamespaces() {
      return this.resources.getNamespaces();
   }

   public boolean hasResource(ResourceLocation pPath) {
      return this.resources.hasResource(pPath);
   }

   public List<Resource> getResources(ResourceLocation pResourceLocation) throws IOException {
      return this.resources.getResources(pResourceLocation);
   }

   public Collection<ResourceLocation> listResources(String pPath, Predicate<String> pFilter) {
      return this.resources.listResources(pPath, pFilter);
   }

   public Stream<PackResources> listPacks() {
      return this.resources.listPacks();
   }
}