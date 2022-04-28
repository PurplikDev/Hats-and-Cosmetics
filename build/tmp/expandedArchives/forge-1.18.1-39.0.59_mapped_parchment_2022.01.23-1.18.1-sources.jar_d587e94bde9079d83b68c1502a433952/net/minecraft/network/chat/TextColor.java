package net.minecraft.network.chat;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;

public final class TextColor {
   private static final String CUSTOM_COLOR_PREFIX = "#";
   private static final Map<ChatFormatting, TextColor> LEGACY_FORMAT_TO_COLOR = Stream.of(ChatFormatting.values()).filter(ChatFormatting::isColor).collect(ImmutableMap.toImmutableMap(Function.identity(), (p_131276_) -> {
      return new TextColor(p_131276_.getColor(), p_131276_.getName());
   }));
   private static final Map<String, TextColor> NAMED_COLORS = LEGACY_FORMAT_TO_COLOR.values().stream().collect(ImmutableMap.toImmutableMap((p_131273_) -> {
      return p_131273_.name;
   }, Function.identity()));
   private final int value;
   @Nullable
   private final String name;

   private TextColor(int pValue, String pName) {
      this.value = pValue;
      this.name = pName;
   }

   private TextColor(int pValue) {
      this.value = pValue;
      this.name = null;
   }

   public int getValue() {
      return this.value;
   }

   public String serialize() {
      return this.name != null ? this.name : this.formatValue();
   }

   private String formatValue() {
      return String.format("#%06X", this.value);
   }

   public boolean equals(Object p_131279_) {
      if (this == p_131279_) {
         return true;
      } else if (p_131279_ != null && this.getClass() == p_131279_.getClass()) {
         TextColor textcolor = (TextColor)p_131279_;
         return this.value == textcolor.value;
      } else {
         return false;
      }
   }

   public int hashCode() {
      return Objects.hash(this.value, this.name);
   }

   public String toString() {
      return this.name != null ? this.name : this.formatValue();
   }

   @Nullable
   public static TextColor fromLegacyFormat(ChatFormatting pFormatting) {
      return LEGACY_FORMAT_TO_COLOR.get(pFormatting);
   }

   public static TextColor fromRgb(int pColor) {
      return new TextColor(pColor);
   }

   @Nullable
   public static TextColor parseColor(String pHexString) {
      if (pHexString.startsWith("#")) {
         try {
            int i = Integer.parseInt(pHexString.substring(1), 16);
            return fromRgb(i);
         } catch (NumberFormatException numberformatexception) {
            return null;
         }
      } else {
         return NAMED_COLORS.get(pHexString);
      }
   }
}