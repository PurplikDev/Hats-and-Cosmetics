package net.minecraft.client.gui.screens;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConfirmScreen extends Screen {
   private static final int LABEL_Y = 90;
   private final Component title2;
   private MultiLineLabel message = MultiLineLabel.EMPTY;
   /** The text shown for the first button in GuiYesNo */
   protected Component yesButton;
   /** The text shown for the second button in GuiYesNo */
   protected Component noButton;
   private int delayTicker;
   protected final BooleanConsumer callback;
   private final List<Button> exitButtons = Lists.newArrayList();

   public ConfirmScreen(BooleanConsumer pCallback, Component pTitle, Component pTitle2) {
      this(pCallback, pTitle, pTitle2, CommonComponents.GUI_YES, CommonComponents.GUI_NO);
   }

   public ConfirmScreen(BooleanConsumer pCallback, Component pTitle, Component pTitle2, Component pYesButton, Component pNoButton) {
      super(pTitle);
      this.callback = pCallback;
      this.title2 = pTitle2;
      this.yesButton = pYesButton;
      this.noButton = pNoButton;
   }

   public Component getNarrationMessage() {
      return CommonComponents.joinForNarration(super.getNarrationMessage(), this.title2);
   }

   protected void init() {
      super.init();
      this.message = MultiLineLabel.create(this.font, this.title2, this.width - 50);
      int i = this.message.getLineCount() * 9;
      int j = Mth.clamp(90 + i + 12, this.height / 6 + 96, this.height - 24);
      this.exitButtons.clear();
      this.addButtons(j);
   }

   protected void addButtons(int p_169252_) {
      this.addExitButton(new Button(this.width / 2 - 155, p_169252_, 150, 20, this.yesButton, (p_169259_) -> {
         this.callback.accept(true);
      }));
      this.addExitButton(new Button(this.width / 2 - 155 + 160, p_169252_, 150, 20, this.noButton, (p_169257_) -> {
         this.callback.accept(false);
      }));
   }

   protected void addExitButton(Button p_169254_) {
      this.exitButtons.add(this.addRenderableWidget(p_169254_));
   }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.title, this.width / 2, 70, 16777215);
      this.message.renderCentered(pPoseStack, this.width / 2, 90);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }

   /**
    * Sets the number of ticks to wait before enabling the buttons.
    */
   public void setDelay(int pTicksUntilEnable) {
      this.delayTicker = pTicksUntilEnable;

      for(Button button : this.exitButtons) {
         button.active = false;
      }

   }

   public void tick() {
      super.tick();
      if (--this.delayTicker == 0) {
         for(Button button : this.exitButtons) {
            button.active = true;
         }
      }

   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (pKeyCode == 256) {
         this.callback.accept(false);
         return true;
      } else {
         return super.keyPressed(pKeyCode, pScanCode, pModifiers);
      }
   }
}