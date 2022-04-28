package net.minecraft.client.gui.font.providers;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum GlyphProviderBuilderType {
   BITMAP("bitmap", BitmapProvider.Builder::fromJson),
   TTF("ttf", TrueTypeGlyphProviderBuilder::fromJson),
   LEGACY_UNICODE("legacy_unicode", LegacyUnicodeBitmapsProvider.Builder::fromJson);

   private static final Map<String, GlyphProviderBuilderType> BY_NAME = Util.make(Maps.newHashMap(), (p_95418_) -> {
      for(GlyphProviderBuilderType glyphproviderbuildertype : values()) {
         p_95418_.put(glyphproviderbuildertype.name, glyphproviderbuildertype);
      }

   });
   private final String name;
   private final Function<JsonObject, GlyphProviderBuilder> factory;

   private GlyphProviderBuilderType(String p_95411_, Function<JsonObject, GlyphProviderBuilder> p_95412_) {
      this.name = p_95411_;
      this.factory = p_95412_;
   }

   public static GlyphProviderBuilderType byName(String pType) {
      GlyphProviderBuilderType glyphproviderbuildertype = BY_NAME.get(pType);
      if (glyphproviderbuildertype == null) {
         throw new IllegalArgumentException("Invalid type: " + pType);
      } else {
         return glyphproviderbuildertype;
      }
   }

   public GlyphProviderBuilder create(JsonObject pJson) {
      return this.factory.apply(pJson);
   }
}