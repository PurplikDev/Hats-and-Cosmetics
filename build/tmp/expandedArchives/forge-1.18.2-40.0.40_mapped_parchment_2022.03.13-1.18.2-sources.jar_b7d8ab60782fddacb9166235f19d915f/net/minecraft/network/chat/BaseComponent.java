package net.minecraft.network.chat;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.locale.Language;
import net.minecraft.util.FormattedCharSequence;

public abstract class BaseComponent implements MutableComponent {
   /**
    * The later siblings of this component. If this component turns the text bold, that will apply to all the siblings
    * until a later sibling turns the text something else.
    */
   protected final List<Component> siblings = Lists.newArrayList();
   private FormattedCharSequence visualOrderText = FormattedCharSequence.EMPTY;
   @Nullable
   private Language decomposedWith;
   private Style style = Style.EMPTY;

   /**
    * Add the given component to this component's siblings.
    * 
    * Note: If this component turns the text bold, that will apply to all the siblings until a later sibling turns the
    * text something else.
    */
   public MutableComponent append(Component pSibling) {
      this.siblings.add(pSibling);
      return this;
   }

   /**
    * Gets the raw content of this component (but not its sibling components), without any formatting codes. For
    * example, this is the raw text in a {@link TextComponentString}, but it's the translated text for a {@link
    * TextComponentTranslation} and it's the score value for a {@link TextComponentScore}.
    */
   public String getContents() {
      return "";
   }

   /**
    * Gets the sibling components of this one.
    */
   public List<Component> getSiblings() {
      return this.siblings;
   }

   /**
    * Sets the style for this component and returns the component itself.
    */
   public MutableComponent setStyle(Style pStyle) {
      this.style = pStyle;
      return this;
   }

   /**
    * Gets the style of this component.
    */
   public Style getStyle() {
      return this.style;
   }

   /**
    * Creates a copy of this component, losing any style or siblings.
    */
   public abstract BaseComponent plainCopy();

   /**
    * Creates a copy of this component and also copies the style and siblings. Note that the siblings are copied
    * shallowly, meaning the siblings themselves are not copied.
    */
   public final MutableComponent copy() {
      BaseComponent basecomponent = this.plainCopy();
      basecomponent.siblings.addAll(this.siblings);
      basecomponent.setStyle(this.style);
      return basecomponent;
   }

   public FormattedCharSequence getVisualOrderText() {
      Language language = Language.getInstance();
      if (this.decomposedWith != language) {
         this.visualOrderText = language.getVisualOrder(this);
         this.decomposedWith = language;
      }

      return this.visualOrderText;
   }

   public boolean equals(Object p_130593_) {
      if (this == p_130593_) {
         return true;
      } else if (!(p_130593_ instanceof BaseComponent)) {
         return false;
      } else {
         BaseComponent basecomponent = (BaseComponent)p_130593_;
         return this.siblings.equals(basecomponent.siblings) && Objects.equals(this.getStyle(), basecomponent.getStyle());
      }
   }

   public int hashCode() {
      return Objects.hash(this.getStyle(), this.siblings);
   }

   public String toString() {
      return "BaseComponent{style=" + this.style + ", siblings=" + this.siblings + "}";
   }
}