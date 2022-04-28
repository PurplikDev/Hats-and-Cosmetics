package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReceivingLevelScreen extends Screen {
   private static final Component DOWNLOADING_TERRAIN_TEXT = new TranslatableComponent("multiplayer.downloadingTerrain");

   public ReceivingLevelScreen() {
      super(NarratorChatListener.NO_TITLE);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderDirtBackground(0);
      drawCenteredString(pPoseStack, this.font, DOWNLOADING_TERRAIN_TEXT, this.width / 2, this.height / 2 - 50, 16777215);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public boolean isPauseScreen() {
      return false;
   }
}