package net.minecraft.client.resources.language;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientLanguage extends Language {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Map<String, String> storage;
   private final boolean defaultRightToLeft;

   private ClientLanguage(Map<String, String> pStorage, boolean pDefaultRightToLeft) {
      this.storage = pStorage;
      this.defaultRightToLeft = pDefaultRightToLeft;
   }

   public static ClientLanguage loadFrom(ResourceManager pResourceManager, List<LanguageInfo> pLanguageInfo) {
      Map<String, String> map = Maps.newHashMap();
      boolean flag = false;

      for(LanguageInfo languageinfo : pLanguageInfo) {
         flag |= languageinfo.isBidirectional();
         String s = String.format("lang/%s.json", languageinfo.getCode());

         for(String s1 : pResourceManager.getNamespaces()) {
            try {
               ResourceLocation resourcelocation = new ResourceLocation(s1, s);
               appendFrom(pResourceManager.getResources(resourcelocation), map);
            } catch (FileNotFoundException filenotfoundexception) {
            } catch (Exception exception) {
               LOGGER.warn("Skipped language file: {}:{} ({})", s1, s, exception.toString());
            }
         }
      }

      return new ClientLanguage(ImmutableMap.copyOf(map), flag);
   }

   private static void appendFrom(List<Resource> pResources, Map<String, String> pDestinationMap) {
      for(Resource resource : pResources) {
         try {
            InputStream inputstream = resource.getInputStream();

            try {
               Language.loadFromJson(inputstream, pDestinationMap::put);
            } catch (Throwable throwable1) {
               if (inputstream != null) {
                  try {
                     inputstream.close();
                  } catch (Throwable throwable) {
                     throwable1.addSuppressed(throwable);
                  }
               }

               throw throwable1;
            }

            if (inputstream != null) {
               inputstream.close();
            }
         } catch (IOException ioexception) {
            LOGGER.warn("Failed to load translations from {}", resource, ioexception);
         }
      }

   }

   public String getOrDefault(String p_118920_) {
      return this.storage.getOrDefault(p_118920_, p_118920_);
   }

   public boolean has(String p_118928_) {
      return this.storage.containsKey(p_118928_);
   }

   public boolean isDefaultRightToLeft() {
      return this.defaultRightToLeft;
   }

   public FormattedCharSequence getVisualOrder(FormattedText p_118925_) {
      return FormattedBidiReorder.reorder(p_118925_, this.defaultRightToLeft);
   }

   @Override
   public Map<String, String> getLanguageData() {
      return storage;
   }
}
