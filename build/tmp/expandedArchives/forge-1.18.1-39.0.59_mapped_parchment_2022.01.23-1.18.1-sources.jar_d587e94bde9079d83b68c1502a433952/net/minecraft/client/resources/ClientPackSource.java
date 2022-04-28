package net.minecraft.client.resources;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.ProgressScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.FolderPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.util.HttpUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientPackSource implements RepositorySource {
   public static final PackMetadataSection BUILT_IN = new PackMetadataSection(new TranslatableComponent("resourcePack.vanilla.description"), PackType.CLIENT_RESOURCES.getVersion(SharedConstants.getCurrentVersion()));
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Pattern SHA1 = Pattern.compile("^[a-fA-F0-9]{40}$");
   private static final int MAX_PACK_SIZE_BYTES = 262144000;
   private static final int MAX_KEPT_PACKS = 10;
   private static final String VANILLA_ID = "vanilla";
   private static final String SERVER_ID = "server";
   private static final String PROGRAMMER_ART_ID = "programer_art";
   private static final String PROGRAMMER_ART_NAME = "Programmer Art";
   private static final Component APPLYING_PACK_TEXT = new TranslatableComponent("multiplayer.applyingPack");
   private final VanillaPackResources vanillaPack;
   private final File serverPackDir;
   private final ReentrantLock downloadLock = new ReentrantLock();
   private final AssetIndex assetIndex;
   @Nullable
   private CompletableFuture<?> currentDownload;
   @Nullable
   private Pack serverPack;

   public ClientPackSource(File pServerPackDir, AssetIndex pAssetIndex) {
      this.serverPackDir = pServerPackDir;
      this.assetIndex = pAssetIndex;
      this.vanillaPack = new DefaultClientPackResources(BUILT_IN, pAssetIndex);
   }

   public void loadPacks(Consumer<Pack> pInfoConsumer, Pack.PackConstructor pInfoFactory) {
      Pack pack = Pack.create("vanilla", true, () -> {
         return this.vanillaPack;
      }, pInfoFactory, Pack.Position.BOTTOM, PackSource.BUILT_IN);
      if (pack != null) {
         pInfoConsumer.accept(pack);
      }

      if (this.serverPack != null) {
         pInfoConsumer.accept(this.serverPack);
      }

      Pack pack1 = this.createProgrammerArtPack(pInfoFactory);
      if (pack1 != null) {
         pInfoConsumer.accept(pack1);
      }

   }

   public VanillaPackResources getVanillaPack() {
      return this.vanillaPack;
   }

   private static Map<String, String> getDownloadHeaders() {
      Map<String, String> map = Maps.newHashMap();
      map.put("X-Minecraft-Username", Minecraft.getInstance().getUser().getName());
      map.put("X-Minecraft-UUID", Minecraft.getInstance().getUser().getUuid());
      map.put("X-Minecraft-Version", SharedConstants.getCurrentVersion().getName());
      map.put("X-Minecraft-Version-ID", SharedConstants.getCurrentVersion().getId());
      map.put("X-Minecraft-Pack-Format", String.valueOf(PackType.CLIENT_RESOURCES.getVersion(SharedConstants.getCurrentVersion())));
      map.put("User-Agent", "Minecraft Java/" + SharedConstants.getCurrentVersion().getName());
      return map;
   }

   public CompletableFuture<?> downloadAndSelectResourcePack(String p_174814_, String p_174815_, boolean p_174816_) {
      String s = Hashing.sha1().hashString(p_174814_, StandardCharsets.UTF_8).toString();
      String s1 = SHA1.matcher(p_174815_).matches() ? p_174815_ : "";
      this.downloadLock.lock();

      CompletableFuture completablefuture1;
      try {
         this.clearServerPack();
         this.clearOldDownloads();
         File file1 = new File(this.serverPackDir, s);
         CompletableFuture<?> completablefuture;
         if (file1.exists()) {
            completablefuture = CompletableFuture.completedFuture("");
         } else {
            ProgressScreen progressscreen = new ProgressScreen(p_174816_);
            Map<String, String> map = getDownloadHeaders();
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.executeBlocking(() -> {
               minecraft.setScreen(progressscreen);
            });
            completablefuture = HttpUtil.downloadTo(file1, p_174814_, map, 262144000, progressscreen, minecraft.getProxy());
         }

         this.currentDownload = completablefuture.thenCompose((p_174812_) -> {
            if (!this.checkHash(s1, file1)) {
               return Util.failedFuture(new RuntimeException("Hash check failure for file " + file1 + ", see log"));
            } else {
               Minecraft minecraft1 = Minecraft.getInstance();
               minecraft1.execute(() -> {
                  if (!p_174816_) {
                     minecraft1.setScreen(new GenericDirtMessageScreen(APPLYING_PACK_TEXT));
                  }

               });
               return this.setServerPack(file1, PackSource.SERVER);
            }
         }).whenComplete((p_174806_, p_174807_) -> {
            if (p_174807_ != null) {
               LOGGER.warn("Pack application failed: {}, deleting file {}", p_174807_.getMessage(), file1);
               deleteQuietly(file1);
               Minecraft minecraft1 = Minecraft.getInstance();
               minecraft1.execute(() -> {
                  minecraft1.setScreen(new ConfirmScreen((p_174803_) -> {
                     if (p_174803_) {
                        minecraft1.setScreen((Screen)null);
                     } else {
                        ClientPacketListener clientpacketlistener = minecraft1.getConnection();
                        if (clientpacketlistener != null) {
                           clientpacketlistener.getConnection().disconnect(new TranslatableComponent("connect.aborted"));
                        }
                     }

                  }, new TranslatableComponent("multiplayer.texturePrompt.failure.line1"), new TranslatableComponent("multiplayer.texturePrompt.failure.line2"), CommonComponents.GUI_PROCEED, new TranslatableComponent("menu.disconnect")));
               });
            }

         });
         completablefuture1 = this.currentDownload;
      } finally {
         this.downloadLock.unlock();
      }

      return completablefuture1;
   }

   private static void deleteQuietly(File pFile) {
      try {
         Files.delete(pFile.toPath());
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to delete file {}: {}", pFile, ioexception.getMessage());
      }

   }

   public void clearServerPack() {
      this.downloadLock.lock();

      try {
         if (this.currentDownload != null) {
            this.currentDownload.cancel(true);
         }

         this.currentDownload = null;
         if (this.serverPack != null) {
            this.serverPack = null;
            Minecraft.getInstance().delayTextureReload();
         }
      } finally {
         this.downloadLock.unlock();
      }

   }

   private boolean checkHash(String pExpectedHash, File pFile) {
      try {
         String s = com.google.common.io.Files.asByteSource(pFile).hash(Hashing.sha1()).toString();
         if (pExpectedHash.isEmpty()) {
            LOGGER.info("Found file {} without verification hash", (Object)pFile);
            return true;
         }

         if (s.toLowerCase(Locale.ROOT).equals(pExpectedHash.toLowerCase(Locale.ROOT))) {
            LOGGER.info("Found file {} matching requested hash {}", pFile, pExpectedHash);
            return true;
         }

         LOGGER.warn("File {} had wrong hash (expected {}, found {}).", pFile, pExpectedHash, s);
      } catch (IOException ioexception) {
         LOGGER.warn("File {} couldn't be hashed.", pFile, ioexception);
      }

      return false;
   }

   private void clearOldDownloads() {
      if (this.serverPackDir.isDirectory()) {
         try {
            List<File> list = Lists.newArrayList(FileUtils.listFiles(this.serverPackDir, TrueFileFilter.TRUE, (IOFileFilter)null));
            list.sort(LastModifiedFileComparator.LASTMODIFIED_REVERSE);
            int i = 0;

            for(File file1 : list) {
               if (i++ >= 10) {
                  LOGGER.info("Deleting old server resource pack {}", (Object)file1.getName());
                  FileUtils.deleteQuietly(file1);
               }
            }
         } catch (Exception exception) {
            LOGGER.error("Error while deleting old server resource pack : {}", (Object)exception.getMessage());
         }

      }
   }

   public CompletableFuture<Void> setServerPack(File pFile, PackSource pPackSource) {
      PackMetadataSection packmetadatasection;
      try {
         FilePackResources filepackresources = new FilePackResources(pFile);

         try {
            packmetadatasection = filepackresources.getMetadataSection(PackMetadataSection.SERIALIZER);
         } catch (Throwable throwable1) {
            try {
               filepackresources.close();
            } catch (Throwable throwable) {
               throwable1.addSuppressed(throwable);
            }

            throw throwable1;
         }

         filepackresources.close();
      } catch (IOException ioexception) {
         return Util.failedFuture(new IOException(String.format("Invalid resourcepack at %s", pFile), ioexception));
      }

      LOGGER.info("Applying server pack {}", (Object)pFile);
      this.serverPack = new Pack("server", true, () -> {
         return new FilePackResources(pFile);
      }, new TranslatableComponent("resourcePack.server.name"), packmetadatasection.getDescription(), PackCompatibility.forMetadata(packmetadatasection, PackType.CLIENT_RESOURCES), Pack.Position.TOP, true, pPackSource);
      return Minecraft.getInstance().delayTextureReload();
   }

   @Nullable
   private Pack createProgrammerArtPack(Pack.PackConstructor pPackConstructor) {
      Pack pack = null;
      File file1 = this.assetIndex.getFile(new ResourceLocation("resourcepacks/programmer_art.zip"));
      if (file1 != null && file1.isFile()) {
         pack = createProgrammerArtPack(pPackConstructor, () -> {
            return createProgrammerArtZipPack(file1);
         });
      }

      if (pack == null && SharedConstants.IS_RUNNING_IN_IDE) {
         File file2 = this.assetIndex.getRootFile("../resourcepacks/programmer_art");
         if (file2 != null && file2.isDirectory()) {
            pack = createProgrammerArtPack(pPackConstructor, () -> {
               return createProgrammerArtDirPack(file2);
            });
         }
      }

      return pack;
   }

   @Nullable
   private static Pack createProgrammerArtPack(Pack.PackConstructor p_118559_, Supplier<PackResources> p_118560_) {
      return Pack.create("programer_art", false, p_118560_, p_118559_, Pack.Position.TOP, PackSource.BUILT_IN);
   }

   private static FolderPackResources createProgrammerArtDirPack(File pFile) {
      return new FolderPackResources(pFile) {
         public String getName() {
            return "Programmer Art";
         }
      };
   }

   private static PackResources createProgrammerArtZipPack(File pFile) {
      return new FilePackResources(pFile) {
         public String getName() {
            return "Programmer Art";
         }
      };
   }
}