package net.minecraft.client.gui.components;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Option;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OptionsList extends ContainerObjectSelectionList<OptionsList.Entry> {
   public OptionsList(Minecraft p_94465_, int p_94466_, int p_94467_, int p_94468_, int p_94469_, int p_94470_) {
      super(p_94465_, p_94466_, p_94467_, p_94468_, p_94469_, p_94470_);
      this.centerListVertically = false;
   }

   public int addBig(Option pOption) {
      return this.addEntry(OptionsList.Entry.big(this.minecraft.options, this.width, pOption));
   }

   public void addSmall(Option pLeftOption, @Nullable Option pRightOption) {
      this.addEntry(OptionsList.Entry.small(this.minecraft.options, this.width, pLeftOption, pRightOption));
   }

   public void addSmall(Option[] pOptions) {
      for(int i = 0; i < pOptions.length; i += 2) {
         this.addSmall(pOptions[i], i < pOptions.length - 1 ? pOptions[i + 1] : null);
      }

   }

   public int getRowWidth() {
      return 400;
   }

   protected int getScrollbarPosition() {
      return super.getScrollbarPosition() + 32;
   }

   @Nullable
   public AbstractWidget findOption(Option pOption) {
      for(OptionsList.Entry optionslist$entry : this.children()) {
         AbstractWidget abstractwidget = optionslist$entry.options.get(pOption);
         if (abstractwidget != null) {
            return abstractwidget;
         }
      }

      return null;
   }

   public Optional<AbstractWidget> getMouseOver(double pMouseX, double pMouseY) {
      for(OptionsList.Entry optionslist$entry : this.children()) {
         for(AbstractWidget abstractwidget : optionslist$entry.children) {
            if (abstractwidget.isMouseOver(pMouseX, pMouseY)) {
               return Optional.of(abstractwidget);
            }
         }
      }

      return Optional.empty();
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Entry extends ContainerObjectSelectionList.Entry<OptionsList.Entry> {
      final Map<Option, AbstractWidget> options;
      final List<AbstractWidget> children;

      private Entry(Map<Option, AbstractWidget> pOptions) {
         this.options = pOptions;
         this.children = ImmutableList.copyOf(pOptions.values());
      }

      /**
       * Creates an options row with button for the specified option
       */
      public static OptionsList.Entry big(Options pSettings, int pGuiWidth, Option pOption) {
         return new OptionsList.Entry(ImmutableMap.of(pOption, pOption.createButton(pSettings, pGuiWidth / 2 - 155, 0, 310)));
      }

      /**
       * Creates an options row with 1 or 2 buttons for specified options
       */
      public static OptionsList.Entry small(Options pSettings, int pGuiWidth, Option pLeftOption, @Nullable Option pRightOption) {
         AbstractWidget abstractwidget = pLeftOption.createButton(pSettings, pGuiWidth / 2 - 155, 0, 150);
         return pRightOption == null ? new OptionsList.Entry(ImmutableMap.of(pLeftOption, abstractwidget)) : new OptionsList.Entry(ImmutableMap.of(pLeftOption, abstractwidget, pRightOption, pRightOption.createButton(pSettings, pGuiWidth / 2 - 155 + 160, 0, 150)));
      }

      public void render(PoseStack pPoseStack, int pIndex, int pTop, int pLeft, int pWidth, int pHeight, int pMouseX, int pMouseY, boolean pIsMouseOver, float pPartialTick) {
         this.children.forEach((p_94494_) -> {
            p_94494_.y = pTop;
            p_94494_.render(pPoseStack, pMouseX, pMouseY, pPartialTick);
         });
      }

      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }
   }
}