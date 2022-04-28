package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractButton extends AbstractWidget {
   public AbstractButton(int p_93365_, int p_93366_, int p_93367_, int p_93368_, Component p_93369_) {
      super(p_93365_, p_93366_, p_93367_, p_93368_, p_93369_);
   }

   public abstract void onPress();

   public void onClick(double pMouseX, double pMouseY) {
      this.onPress();
   }

   public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
      if (this.active && this.visible) {
         if (pKeyCode != 257 && pKeyCode != 32 && pKeyCode != 335) {
            return false;
         } else {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress();
            return true;
         }
      } else {
         return false;
      }
   }
}