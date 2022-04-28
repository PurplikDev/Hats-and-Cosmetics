package net.minecraft.client;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CycleOption<T> extends Option {
   private final CycleOption.OptionSetter<T> setter;
   private final Function<Options, T> getter;
   private final Supplier<CycleButton.Builder<T>> buttonSetup;
   private Function<Minecraft, CycleButton.TooltipSupplier<T>> tooltip = (p_167722_) -> {
      return (p_167728_) -> {
         return ImmutableList.of();
      };
   };

   private CycleOption(String pCaptionKey, Function<Options, T> pGetter, CycleOption.OptionSetter<T> pSetter, Supplier<CycleButton.Builder<T>> pButtonSetup) {
      super(pCaptionKey);
      this.getter = pGetter;
      this.setter = pSetter;
      this.buttonSetup = pButtonSetup;
   }

   public static <T> CycleOption<T> create(String pCaptionKey, List<T> pValues, Function<T, Component> pValueStringifier, Function<Options, T> pGetter, CycleOption.OptionSetter<T> pSetter) {
      return new CycleOption<>(pCaptionKey, pGetter, pSetter, () -> {
         return CycleButton.builder(pValueStringifier).withValues(pValues);
      });
   }

   public static <T> CycleOption<T> create(String pCaptionKey, Supplier<List<T>> pValues, Function<T, Component> pValueStringifier, Function<Options, T> pGetter, CycleOption.OptionSetter<T> pSetter) {
      return new CycleOption<>(pCaptionKey, pGetter, pSetter, () -> {
         return CycleButton.builder(pValueStringifier).withValues(pValues.get());
      });
   }

   public static <T> CycleOption<T> create(String pCaptionKey, List<T> pDefaultList, List<T> pSelectedList, BooleanSupplier pAltListSelector, Function<T, Component> pValueStringifier, Function<Options, T> pGetter, CycleOption.OptionSetter<T> pSetter) {
      return new CycleOption<>(pCaptionKey, pGetter, pSetter, () -> {
         return CycleButton.builder(pValueStringifier).withValues(pAltListSelector, pDefaultList, pSelectedList);
      });
   }

   public static <T> CycleOption<T> create(String pCaptionKey, T[] pValues, Function<T, Component> pValueStringifier, Function<Options, T> pGetter, CycleOption.OptionSetter<T> pSetter) {
      return new CycleOption<>(pCaptionKey, pGetter, pSetter, () -> {
         return CycleButton.builder(pValueStringifier).withValues(pValues);
      });
   }

   public static CycleOption<Boolean> createBinaryOption(String pCaptionKey, Component pDefaultValue, Component pSelectedValue, Function<Options, Boolean> pGetter, CycleOption.OptionSetter<Boolean> pSetter) {
      return new CycleOption<>(pCaptionKey, pGetter, pSetter, () -> {
         return CycleButton.booleanBuilder(pDefaultValue, pSelectedValue);
      });
   }

   public static CycleOption<Boolean> createOnOff(String pCaptionKey, Function<Options, Boolean> pGetter, CycleOption.OptionSetter<Boolean> pSetter) {
      return new CycleOption<>(pCaptionKey, pGetter, pSetter, CycleButton::onOffBuilder);
   }

   public static CycleOption<Boolean> createOnOff(String pCaptionKey, Component pDefaultValue, Function<Options, Boolean> pGetter, CycleOption.OptionSetter<Boolean> pSetter) {
      return createOnOff(pCaptionKey, pGetter, pSetter).setTooltip((p_167791_) -> {
         List<FormattedCharSequence> list = p_167791_.font.split(pDefaultValue, 200);
         return (p_167772_) -> {
            return list;
         };
      });
   }

   public CycleOption<T> setTooltip(Function<Minecraft, CycleButton.TooltipSupplier<T>> pTooltip) {
      this.tooltip = pTooltip;
      return this;
   }

   public AbstractWidget createButton(Options pOptions, int pX, int pY, int pWidth) {
      CycleButton.TooltipSupplier<T> tooltipsupplier = this.tooltip.apply(Minecraft.getInstance());
      return this.buttonSetup.get().withTooltip(tooltipsupplier).withInitialValue(this.getter.apply(pOptions)).create(pX, pY, pWidth, 20, this.getCaption(), (p_167725_, p_167726_) -> {
         this.setter.accept(pOptions, this, p_167726_);
         pOptions.save();
      });
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   public interface OptionSetter<T> {
      void accept(Options pOptions, Option pOption, T pValue);
   }
}