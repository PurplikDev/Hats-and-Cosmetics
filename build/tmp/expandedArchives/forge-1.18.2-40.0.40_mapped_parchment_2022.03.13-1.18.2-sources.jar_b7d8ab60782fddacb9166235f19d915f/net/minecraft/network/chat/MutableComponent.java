package net.minecraft.network.chat;

import java.util.function.UnaryOperator;
import net.minecraft.ChatFormatting;

/**
 * A Component which can have its Style and siblings modified.
 */
public interface MutableComponent extends Component {
   /**
    * Sets the style for this component and returns the component itself.
    */
   MutableComponent setStyle(Style pStyle);

   /**
    * Add the given text to this component's siblings.
    * 
    * Note: If this component turns the text bold, that will apply to all the siblings until a later sibling turns the
    * text something else.
    */
   default MutableComponent append(String pString) {
      return this.append(new TextComponent(pString));
   }

   /**
    * Add the given component to this component's siblings.
    * 
    * Note: If this component turns the text bold, that will apply to all the siblings until a later sibling turns the
    * text something else.
    */
   MutableComponent append(Component pSibling);

   default MutableComponent withStyle(UnaryOperator<Style> pModifyFunc) {
      this.setStyle(pModifyFunc.apply(this.getStyle()));
      return this;
   }

   default MutableComponent withStyle(Style pStyle) {
      this.setStyle(pStyle.applyTo(this.getStyle()));
      return this;
   }

   default MutableComponent withStyle(ChatFormatting... pFormats) {
      this.setStyle(this.getStyle().applyFormats(pFormats));
      return this;
   }

   default MutableComponent withStyle(ChatFormatting pFormat) {
      this.setStyle(this.getStyle().applyFormat(pFormat));
      return this;
   }
}