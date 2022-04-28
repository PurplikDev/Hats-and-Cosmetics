package net.minecraft.client.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AlertScreen extends Screen {
   private final Runnable callback;
   protected final Component text;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   protected final Component okButton;

   public AlertScreen(Runnable pCallback, Component pTitle, Component pText) {
      this(pCallback, pTitle, pText, CommonComponents.GUI_BACK);
   }

   public AlertScreen(Runnable pCallback, Component pTitle, Component pText, Component pOkButton) {
      super(pTitle);
      this.callback = pCallback;
      this.text = pText;
      this.okButton = pOkButton;
   }

   protected void init() {
      super.init();
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height / 6 + 168, 200, 20, this.okButton, (p_95533_) -> {
         this.callback.run();
      }));
      this.message = MultiLineLabel.create(this.font, this.text, this.width - 50);
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 70, 16777215);
      this.message.renderCentered(pPoseStack, this.width / 2, 90);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}