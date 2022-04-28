package net.minecraft.server.dedicated;

import com.mojang.authlib.GameProfile;
import java.io.IOException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DedicatedPlayerList extends PlayerList {
   private static final Logger LOGGER = LogManager.getLogger();

   public DedicatedPlayerList(DedicatedServer p_139574_, RegistryAccess.RegistryHolder p_139575_, PlayerDataStorage p_139576_) {
      super(p_139574_, p_139575_, p_139576_, p_139574_.getProperties().maxPlayers);
      DedicatedServerProperties dedicatedserverproperties = p_139574_.getProperties();
      this.setViewDistance(dedicatedserverproperties.viewDistance);
      this.setSimulationDistance(dedicatedserverproperties.simulationDistance);
      super.setUsingWhiteList(dedicatedserverproperties.whiteList.get());
      this.loadUserBanList();
      this.saveUserBanList();
      this.loadIpBanList();
      this.saveIpBanList();
      this.loadOps();
      this.loadWhiteList();
      this.saveOps();
      if (!this.getWhiteList().getFile().exists()) {
         this.saveWhiteList();
      }

   }

   public void setUsingWhiteList(boolean pWhitelistEnabled) {
      super.setUsingWhiteList(pWhitelistEnabled);
      this.getServer().storeUsingWhiteList(pWhitelistEnabled);
   }

   public void op(GameProfile pProfile) {
      super.op(pProfile);
      this.saveOps();
   }

   public void deop(GameProfile pProfile) {
      super.deop(pProfile);
      this.saveOps();
   }

   public void reloadWhiteList() {
      this.loadWhiteList();
   }

   private void saveIpBanList() {
      try {
         this.getIpBans().save();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to save ip banlist: ", (Throwable)ioexception);
      }

   }

   private void saveUserBanList() {
      try {
         this.getBans().save();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to save user banlist: ", (Throwable)ioexception);
      }

   }

   private void loadIpBanList() {
      try {
         this.getIpBans().load();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to load ip banlist: ", (Throwable)ioexception);
      }

   }

   private void loadUserBanList() {
      try {
         this.getBans().load();
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to load user banlist: ", (Throwable)ioexception);
      }

   }

   private void loadOps() {
      try {
         this.getOps().load();
      } catch (Exception exception) {
         LOGGER.warn("Failed to load operators list: ", (Throwable)exception);
      }

   }

   private void saveOps() {
      try {
         this.getOps().save();
      } catch (Exception exception) {
         LOGGER.warn("Failed to save operators list: ", (Throwable)exception);
      }

   }

   private void loadWhiteList() {
      try {
         this.getWhiteList().load();
      } catch (Exception exception) {
         LOGGER.warn("Failed to load white-list: ", (Throwable)exception);
      }

   }

   private void saveWhiteList() {
      try {
         this.getWhiteList().save();
      } catch (Exception exception) {
         LOGGER.warn("Failed to save white-list: ", (Throwable)exception);
      }

   }

   public boolean isWhiteListed(GameProfile pProfile) {
      return !this.isUsingWhitelist() || this.isOp(pProfile) || this.getWhiteList().isWhiteListed(pProfile);
   }

   public DedicatedServer getServer() {
      return (DedicatedServer)super.getServer();
   }

   public boolean canBypassPlayerLimit(GameProfile pProfile) {
      return this.getOps().canBypassPlayerLimit(pProfile);
   }
}