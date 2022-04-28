package net.minecraft.client.gui.screens.social;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SocialInteractionsPlayerList extends ContainerObjectSelectionList<PlayerEntry> {
   private final SocialInteractionsScreen socialInteractionsScreen;
   private final List<PlayerEntry> players = Lists.newArrayList();
   @Nullable
   private String filter;

   public SocialInteractionsPlayerList(SocialInteractionsScreen pSocialInteractionsScreen, Minecraft pMinecraft, int pWidth, int pHeight, int pY0, int pY1, int pItemHeight) {
      super(pMinecraft, pWidth, pHeight, pY0, pY1, pItemHeight);
      this.socialInteractionsScreen = pSocialInteractionsScreen;
      this.setRenderBackground(false);
      this.setRenderTopAndBottom(false);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      double d0 = this.minecraft.getWindow().getGuiScale();
      RenderSystem.enableScissor((int)((double)this.getRowLeft() * d0), (int)((double)(this.height - this.y1) * d0), (int)((double)(this.getScrollbarPosition() + 6) * d0), (int)((double)(this.height - (this.height - this.y1) - this.y0 - 4) * d0));
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
      RenderSystem.disableScissor();
   }

   public void updatePlayerList(Collection<UUID> pIds, double pScrollAmount) {
      this.players.clear();

      for(UUID uuid : pIds) {
         PlayerInfo playerinfo = this.minecraft.player.connection.getPlayerInfo(uuid);
         if (playerinfo != null) {
            this.players.add(new PlayerEntry(this.minecraft, this.socialInteractionsScreen, playerinfo.getProfile().getId(), playerinfo.getProfile().getName(), playerinfo::getSkinLocation));
         }
      }

      this.updateFilteredPlayers();
      this.players.sort((p_100712_, p_100713_) -> {
         return p_100712_.getPlayerName().compareToIgnoreCase(p_100713_.getPlayerName());
      });
      this.replaceEntries(this.players);
      this.setScrollAmount(pScrollAmount);
   }

   private void updateFilteredPlayers() {
      if (this.filter != null) {
         this.players.removeIf((p_100710_) -> {
            return !p_100710_.getPlayerName().toLowerCase(Locale.ROOT).contains(this.filter);
         });
         this.replaceEntries(this.players);
      }

   }

   public void setFilter(String pFilter) {
      this.filter = pFilter;
   }

   public boolean isEmpty() {
      return this.players.isEmpty();
   }

   public void addPlayer(PlayerInfo pPlayerInfo, SocialInteractionsScreen.Page pPage) {
      UUID uuid = pPlayerInfo.getProfile().getId();

      for(PlayerEntry playerentry : this.players) {
         if (playerentry.getPlayerId().equals(uuid)) {
            playerentry.setRemoved(false);
            return;
         }
      }

      if ((pPage == SocialInteractionsScreen.Page.ALL || this.minecraft.getPlayerSocialManager().shouldHideMessageFrom(uuid)) && (Strings.isNullOrEmpty(this.filter) || pPlayerInfo.getProfile().getName().toLowerCase(Locale.ROOT).contains(this.filter))) {
         PlayerEntry playerentry1 = new PlayerEntry(this.minecraft, this.socialInteractionsScreen, pPlayerInfo.getProfile().getId(), pPlayerInfo.getProfile().getName(), pPlayerInfo::getSkinLocation);
         this.addEntry(playerentry1);
         this.players.add(playerentry1);
      }

   }

   public void removePlayer(UUID pId) {
      for(PlayerEntry playerentry : this.players) {
         if (playerentry.getPlayerId().equals(pId)) {
            playerentry.setRemoved(true);
            return;
         }
      }

   }
}