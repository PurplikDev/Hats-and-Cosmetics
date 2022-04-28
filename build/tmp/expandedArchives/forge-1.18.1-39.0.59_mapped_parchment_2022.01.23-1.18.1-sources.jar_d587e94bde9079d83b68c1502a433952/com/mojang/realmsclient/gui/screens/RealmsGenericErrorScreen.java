package com.mojang.realmsclient.gui.screens;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.realmsclient.exception.RealmsServiceException;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.realms.RealmsScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsGenericErrorScreen extends RealmsScreen {
   private final Screen nextScreen;
   private final Pair<Component, Component> lines;
   private MultiLineLabel line2Split = MultiLineLabel.EMPTY;

   public RealmsGenericErrorScreen(RealmsServiceException pServiceException, Screen pNextScreen) {
      super(NarratorChatListener.NO_TITLE);
      this.nextScreen = pNextScreen;
      this.lines = errorMessage(pServiceException);
   }

   public RealmsGenericErrorScreen(Component pServiceException, Screen pNextScreen) {
      super(NarratorChatListener.NO_TITLE);
      this.nextScreen = pNextScreen;
      this.lines = errorMessage(pServiceException);
   }

   public RealmsGenericErrorScreen(Component pLine1, Component pLine2, Screen pNextScreen) {
      super(NarratorChatListener.NO_TITLE);
      this.nextScreen = pNextScreen;
      this.lines = errorMessage(pLine1, pLine2);
   }

   private static Pair<Component, Component> errorMessage(RealmsServiceException pException) {
      if (pException.realmsError == null) {
         return Pair.of(new TextComponent("An error occurred (" + pException.httpResultCode + "):"), new TextComponent(pException.rawResponse));
      } else {
         String s = "mco.errorMessage." + pException.realmsError.getErrorCode();
         return Pair.of(new TextComponent("Realms (" + pException.realmsError + "):"), (Component)(I18n.exists(s) ? new TranslatableComponent(s) : Component.nullToEmpty(pException.realmsError.getErrorMessage())));
      }
   }

   private static Pair<Component, Component> errorMessage(Component pLine2) {
      return Pair.of(new TextComponent("An error occurred: "), pLine2);
   }

   private static Pair<Component, Component> errorMessage(Component pLine1, Component pLine2) {
      return Pair.of(pLine1, pLine2);
   }

   public void init() {
      this.addRenderableWidget(new Button(this.width / 2 - 100, this.height - 52, 200, 20, new TextComponent("Ok"), (p_88686_) -> {
         this.minecraft.setScreen(this.nextScreen);
      }));
      this.line2Split = MultiLineLabel.create(this.font, this.lines.getSecond(), this.width * 3 / 4);
   }

   public Component getNarrationMessage() {
      return (new TextComponent("")).append(this.lines.getFirst()).append(": ").append(this.lines.getSecond());
   }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
       if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
          minecraft.setScreen(this.nextScreen);
          return true;
       }
       return super.keyPressed(key, scanCode, modifiers);
    }

   public void render(PoseStack pPoseStack, int pMouseX, int pMouseY, float pPartialTick) {
      this.renderBackground(pPoseStack);
      drawCenteredString(pPoseStack, this.font, this.lines.getFirst(), this.width / 2, 80, 16777215);
      this.line2Split.renderCentered(pPoseStack, this.width / 2, 100, 9, 16711680);
      super.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
   }
}
