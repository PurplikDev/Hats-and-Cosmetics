package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsClientOutdatedScreen extends RealmsScreen {
   private static final Component OUTDATED_TITLE = new TranslatableComponent("mco.client.outdated.title");
   private static final Component[] OUTDATED_MESSAGES = new Component[]{new TranslatableComponent("mco.client.outdated.msg.line1"), new TranslatableComponent("mco.client.outdated.msg.line2")};
   private static final Component INCOMPATIBLE_TITLE = new TranslatableComponent("mco.client.incompatible.title");
   private static final Component[] INCOMPATIBLE_MESSAGES = new Component[]{new TranslatableComponent("mco.client.incompatible.msg.line1"), new TranslatableComponent("mco.client.incompatible.msg.line2"), new TranslatableComponent("mco.client.incompatible.msg.line3")};
   private final Screen lastScreen;
   private final boolean outdated;

   public RealmsClientOutdatedScreen(Screen pLastScreen, boolean pOutdated) {
      super(pOutdated ? OUTDATED_TITLE : INCOMPATIBLE_TITLE);
      this.lastScreen = pLastScreen;
      this.outdated = pOutdated;
   }

   public void init() {
      this.addRenderableWidget(new Button(this.width / 2 - 100, row(12), 200, 20, CommonComponents.GUI_BACK, (p_88378_) -> {
         this.minecraft.setScreen(this.lastScreen);
      }));
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, row(3), 16711680);
      Component[] acomponent = this.outdated ? INCOMPATIBLE_MESSAGES : OUTDATED_MESSAGES;

      for(int i = 0; i < acomponent.length; ++i) {
         drawCenteredString(pPoseStack, this.font, acomponent[i], this.width / 2, row(5) + i * 12, 16777215);
      }

      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode != 257 && pKeyCode != 335 && pKeyCode != 256) {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      } else {
         this.minecraft.setScreen(this.lastScreen);
         return true;
      }
   }
}