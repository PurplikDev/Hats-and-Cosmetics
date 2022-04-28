package net.minecraft.client.gui.components;

import java.util.List;
import net.minecraft.client.Options;
import net.minecraft.client.ProgressOption;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SliderButton extends AbstractOptionSliderButton implements TooltipAccessor {
   private final ProgressOption option;
   private final List<FormattedCharSequence> tooltip;

   public SliderButton(Options pOptions, int pX, int pY, int pWidth, int pHeight, ProgressOption pProgressOption, List<FormattedCharSequence> pTooltip) {
      super(pOptions, pX, pY, pWidth, pHeight, (double)((float)pProgressOption.toPct(pProgressOption.get(pOptions))));
      this.option = pProgressOption;
      this.tooltip = pTooltip;
      this.updateMessage();
   }

   protected void applyValue() {
      this.option.set(this.options, this.option.toValue(this.value));
      this.options.save();
   }

   protected void updateMessage() {
      this.setMessage(this.option.getMessage(this.options));
   }

   public List<FormattedCharSequence> getTooltip() {
      return this.tooltip;
   }
}