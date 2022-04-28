package net.minecraft.network.chat;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;
import java.lang.reflect.Type;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * A Style for {@link Component}.
 * Stores color, text formatting (bold, etc.) as well as possible HoverEvent/ClickEvent.
 */
public class Style {
   public static final Style EMPTY = new Style((TextColor)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (Boolean)null, (ClickEvent)null, (HoverEvent)null, (String)null, (ResourceLocation)null);
   public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");
   @Nullable
   final TextColor color;
   @Nullable
   final Boolean bold;
   @Nullable
   final Boolean italic;
   @Nullable
   final Boolean underlined;
   @Nullable
   final Boolean strikethrough;
   @Nullable
   final Boolean obfuscated;
   @Nullable
   final ClickEvent clickEvent;
   @Nullable
   final HoverEvent hoverEvent;
   @Nullable
   final String insertion;
   @Nullable
   final ResourceLocation font;

   Style(@Nullable TextColor pColor, @Nullable Boolean pBold, @Nullable Boolean pItalic, @Nullable Boolean pUnderlined, @Nullable Boolean pStrikethrough, @Nullable Boolean pObfuscated, @Nullable ClickEvent pClickEvent, @Nullable HoverEvent pHoverEvent, @Nullable String pInsertion, @Nullable ResourceLocation pFont) {
      this.color = pColor;
      this.bold = pBold;
      this.italic = pItalic;
      this.underlined = pUnderlined;
      this.strikethrough = pStrikethrough;
      this.obfuscated = pObfuscated;
      this.clickEvent = pClickEvent;
      this.hoverEvent = pHoverEvent;
      this.insertion = pInsertion;
      this.font = pFont;
   }

   @Nullable
   public TextColor getColor() {
      return this.color;
   }

   /**
    * Whether or not text of this ChatStyle should be in bold.
    */
   public boolean isBold() {
      return this.bold == Boolean.TRUE;
   }

   /**
    * Whether or not text of this ChatStyle should be italicized.
    */
   public boolean isItalic() {
      return this.italic == Boolean.TRUE;
   }

   /**
    * Whether or not to format text of this ChatStyle using strikethrough.
    */
   public boolean isStrikethrough() {
      return this.strikethrough == Boolean.TRUE;
   }

   /**
    * Whether or not text of this ChatStyle should be underlined.
    */
   public boolean isUnderlined() {
      return this.underlined == Boolean.TRUE;
   }

   /**
    * Whether or not text of this ChatStyle should be obfuscated.
    */
   public boolean isObfuscated() {
      return this.obfuscated == Boolean.TRUE;
   }

   /**
    * Whether or not this style is empty (inherits everything from the parent).
    */
   public boolean isEmpty() {
      return this == EMPTY;
   }

   /**
    * The effective chat click event.
    */
   @Nullable
   public ClickEvent getClickEvent() {
      return this.clickEvent;
   }

   /**
    * The effective chat hover event.
    */
   @Nullable
   public HoverEvent getHoverEvent() {
      return this.hoverEvent;
   }

   /**
    * Get the text to be inserted into Chat when the component is shift-clicked
    */
   @Nullable
   public String getInsertion() {
      return this.insertion;
   }

   /**
    * The font to use for this Style
    */
   public ResourceLocation getFont() {
      return this.font != null ? this.font : DEFAULT_FONT;
   }

   public Style withColor(@Nullable TextColor pColor) {
      return new Style(pColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style withColor(@Nullable ChatFormatting pFormatting) {
      return this.withColor(pFormatting != null ? TextColor.fromLegacyFormat(pFormatting) : null);
   }

   public Style withColor(int pRgb) {
      return this.withColor(TextColor.fromRgb(pRgb));
   }

   public Style withBold(@Nullable Boolean pBold) {
      return new Style(this.color, pBold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style withItalic(@Nullable Boolean pItalic) {
      return new Style(this.color, this.bold, pItalic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style withUnderlined(@Nullable Boolean pUnderlined) {
      return new Style(this.color, this.bold, this.italic, pUnderlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style withStrikethrough(@Nullable Boolean pStrikethrough) {
      return new Style(this.color, this.bold, this.italic, this.underlined, pStrikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style withObfuscated(@Nullable Boolean pObfuscated) {
      return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, pObfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style setUnderlined(@Nullable Boolean underlined) {
      return new Style(this.color, this.bold, this.italic, underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style setStrikethrough(@Nullable Boolean strikethrough) {
      return new Style(this.color, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style setObfuscated(@Nullable Boolean obfuscated) {
      return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style withClickEvent(@Nullable ClickEvent pClickEvent) {
      return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, pClickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style withHoverEvent(@Nullable HoverEvent pHoverEvent) {
      return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, pHoverEvent, this.insertion, this.font);
   }

   public Style withInsertion(@Nullable String pInsertion) {
      return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, pInsertion, this.font);
   }

   public Style withFont(@Nullable ResourceLocation pFontId) {
      return new Style(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion, pFontId);
   }

   public Style applyFormat(ChatFormatting pFormatting) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;
      switch(pFormatting) {
      case OBFUSCATED:
         obool4 = true;
         break;
      case BOLD:
         obool = true;
         break;
      case STRIKETHROUGH:
         obool2 = true;
         break;
      case UNDERLINE:
         obool3 = true;
         break;
      case ITALIC:
         obool1 = true;
         break;
      case RESET:
         return EMPTY;
      default:
         textcolor = TextColor.fromLegacyFormat(pFormatting);
      }

      return new Style(textcolor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyLegacyFormat(ChatFormatting pFormatting) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;
      switch(pFormatting) {
      case OBFUSCATED:
         obool4 = true;
         break;
      case BOLD:
         obool = true;
         break;
      case STRIKETHROUGH:
         obool2 = true;
         break;
      case UNDERLINE:
         obool3 = true;
         break;
      case ITALIC:
         obool1 = true;
         break;
      case RESET:
         return EMPTY;
      default:
         obool4 = false;
         obool = false;
         obool2 = false;
         obool3 = false;
         obool1 = false;
         textcolor = TextColor.fromLegacyFormat(pFormatting);
      }

      return new Style(textcolor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   public Style applyFormats(ChatFormatting... pFormats) {
      TextColor textcolor = this.color;
      Boolean obool = this.bold;
      Boolean obool1 = this.italic;
      Boolean obool2 = this.strikethrough;
      Boolean obool3 = this.underlined;
      Boolean obool4 = this.obfuscated;

      for(ChatFormatting chatformatting : pFormats) {
         switch(chatformatting) {
         case OBFUSCATED:
            obool4 = true;
            break;
         case BOLD:
            obool = true;
            break;
         case STRIKETHROUGH:
            obool2 = true;
            break;
         case UNDERLINE:
            obool3 = true;
            break;
         case ITALIC:
            obool1 = true;
            break;
         case RESET:
            return EMPTY;
         default:
            textcolor = TextColor.fromLegacyFormat(chatformatting);
         }
      }

      return new Style(textcolor, obool, obool1, obool3, obool2, obool4, this.clickEvent, this.hoverEvent, this.insertion, this.font);
   }

   /**
    * Merges the style with another one. If either styles are empty the other will be returned. If a value already
    * exists on the current style it will not be overriden.
    */
   public Style applyTo(Style pStyle) {
      if (this == EMPTY) {
         return pStyle;
      } else {
         return pStyle == EMPTY ? this : new Style(this.color != null ? this.color : pStyle.color, this.bold != null ? this.bold : pStyle.bold, this.italic != null ? this.italic : pStyle.italic, this.underlined != null ? this.underlined : pStyle.underlined, this.strikethrough != null ? this.strikethrough : pStyle.strikethrough, this.obfuscated != null ? this.obfuscated : pStyle.obfuscated, this.clickEvent != null ? this.clickEvent : pStyle.clickEvent, this.hoverEvent != null ? this.hoverEvent : pStyle.hoverEvent, this.insertion != null ? this.insertion : pStyle.insertion, this.font != null ? this.font : pStyle.font);
      }
   }

   public String toString() {
      return "Style{ color=" + this.color + ", bold=" + this.bold + ", italic=" + this.italic + ", underlined=" + this.underlined + ", strikethrough=" + this.strikethrough + ", obfuscated=" + this.obfuscated + ", clickEvent=" + this.getClickEvent() + ", hoverEvent=" + this.getHoverEvent() + ", insertion=" + this.getInsertion() + ", font=" + this.getFont() + "}";
   }

   public boolean equals(Object p_131175_) {
      if (this == p_131175_) {
         return true;
      } else if (!(p_131175_ instanceof Style)) {
         return false;
      } else {
         Style style = (Style)p_131175_;
         return this.isBold() == style.isBold() && Objects.equals(this.getColor(), style.getColor()) && this.isItalic() == style.isItalic() && this.isObfuscated() == style.isObfuscated() && this.isStrikethrough() == style.isStrikethrough() && this.isUnderlined() == style.isUnderlined() && Objects.equals(this.getClickEvent(), style.getClickEvent()) && Objects.equals(this.getHoverEvent(), style.getHoverEvent()) && Objects.equals(this.getInsertion(), style.getInsertion()) && Objects.equals(this.getFont(), style.getFont());
      }
   }

   public int hashCode() {
      return Objects.hash(this.color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.clickEvent, this.hoverEvent, this.insertion);
   }

   public static class Serializer implements JsonDeserializer<Style>, JsonSerializer<Style> {
      @Nullable
      public Style deserialize(JsonElement p_131200_, Type p_131201_, JsonDeserializationContext p_131202_) throws JsonParseException {
         if (p_131200_.isJsonObject()) {
            JsonObject jsonobject = p_131200_.getAsJsonObject();
            if (jsonobject == null) {
               return null;
            } else {
               Boolean obool = getOptionalFlag(jsonobject, "bold");
               Boolean obool1 = getOptionalFlag(jsonobject, "italic");
               Boolean obool2 = getOptionalFlag(jsonobject, "underlined");
               Boolean obool3 = getOptionalFlag(jsonobject, "strikethrough");
               Boolean obool4 = getOptionalFlag(jsonobject, "obfuscated");
               TextColor textcolor = getTextColor(jsonobject);
               String s = getInsertion(jsonobject);
               ClickEvent clickevent = getClickEvent(jsonobject);
               HoverEvent hoverevent = getHoverEvent(jsonobject);
               ResourceLocation resourcelocation = getFont(jsonobject);
               return new Style(textcolor, obool, obool1, obool2, obool3, obool4, clickevent, hoverevent, s, resourcelocation);
            }
         } else {
            return null;
         }
      }

      @Nullable
      private static ResourceLocation getFont(JsonObject pJson) {
         if (pJson.has("font")) {
            String s = GsonHelper.getAsString(pJson, "font");

            try {
               return new ResourceLocation(s);
            } catch (ResourceLocationException resourcelocationexception) {
               throw new JsonSyntaxException("Invalid font name: " + s);
            }
         } else {
            return null;
         }
      }

      @Nullable
      private static HoverEvent getHoverEvent(JsonObject pJson) {
         if (pJson.has("hoverEvent")) {
            JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "hoverEvent");
            HoverEvent hoverevent = HoverEvent.deserialize(jsonobject);
            if (hoverevent != null && hoverevent.getAction().isAllowedFromServer()) {
               return hoverevent;
            }
         }

         return null;
      }

      @Nullable
      private static ClickEvent getClickEvent(JsonObject pJson) {
         if (pJson.has("clickEvent")) {
            JsonObject jsonobject = GsonHelper.getAsJsonObject(pJson, "clickEvent");
            String s = GsonHelper.getAsString(jsonobject, "action", (String)null);
            ClickEvent.Action clickevent$action = s == null ? null : ClickEvent.Action.getByName(s);
            String s1 = GsonHelper.getAsString(jsonobject, "value", (String)null);
            if (clickevent$action != null && s1 != null && clickevent$action.isAllowedFromServer()) {
               return new ClickEvent(clickevent$action, s1);
            }
         }

         return null;
      }

      @Nullable
      private static String getInsertion(JsonObject pJson) {
         return GsonHelper.getAsString(pJson, "insertion", (String)null);
      }

      @Nullable
      private static TextColor getTextColor(JsonObject pJson) {
         if (pJson.has("color")) {
            String s = GsonHelper.getAsString(pJson, "color");
            return TextColor.parseColor(s);
         } else {
            return null;
         }
      }

      @Nullable
      private static Boolean getOptionalFlag(JsonObject pJson, String pMemberName) {
         return pJson.has(pMemberName) ? pJson.get(pMemberName).getAsBoolean() : null;
      }

      @Nullable
      public JsonElement serialize(Style p_131209_, Type p_131210_, JsonSerializationContext p_131211_) {
         if (p_131209_.isEmpty()) {
            return null;
         } else {
            JsonObject jsonobject = new JsonObject();
            if (p_131209_.bold != null) {
               jsonobject.addProperty("bold", p_131209_.bold);
            }

            if (p_131209_.italic != null) {
               jsonobject.addProperty("italic", p_131209_.italic);
            }

            if (p_131209_.underlined != null) {
               jsonobject.addProperty("underlined", p_131209_.underlined);
            }

            if (p_131209_.strikethrough != null) {
               jsonobject.addProperty("strikethrough", p_131209_.strikethrough);
            }

            if (p_131209_.obfuscated != null) {
               jsonobject.addProperty("obfuscated", p_131209_.obfuscated);
            }

            if (p_131209_.color != null) {
               jsonobject.addProperty("color", p_131209_.color.serialize());
            }

            if (p_131209_.insertion != null) {
               jsonobject.add("insertion", p_131211_.serialize(p_131209_.insertion));
            }

            if (p_131209_.clickEvent != null) {
               JsonObject jsonobject1 = new JsonObject();
               jsonobject1.addProperty("action", p_131209_.clickEvent.getAction().getName());
               jsonobject1.addProperty("value", p_131209_.clickEvent.getValue());
               jsonobject.add("clickEvent", jsonobject1);
            }

            if (p_131209_.hoverEvent != null) {
               jsonobject.add("hoverEvent", p_131209_.hoverEvent.serialize());
            }

            if (p_131209_.font != null) {
               jsonobject.addProperty("font", p_131209_.font.toString());
            }

            return jsonobject;
         }
      }
   }
}
