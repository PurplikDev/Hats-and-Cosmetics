package net.minecraft.server.packs.resources;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.server.packs.PackResources;
import net.minecraft.util.Unit;

public interface ReloadableResourceManager extends ResourceManager, AutoCloseable {
   default CompletableFuture<Unit> reload(Executor pBackgroundExecutor, Executor pGameExecutor, List<PackResources> pResourcePacks, CompletableFuture<Unit> pWaitingFor) {
      return this.createReload(pBackgroundExecutor, pGameExecutor, pWaitingFor, pResourcePacks).done();
   }

   ReloadInstance createReload(Executor p_143930_, Executor p_143931_, CompletableFuture<Unit> p_143932_, List<PackResources> p_143933_);

   void registerReloadListener(PreparableReloadListener pListener);

   void close();
}