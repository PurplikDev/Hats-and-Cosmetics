package net.minecraft.data.info;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.core.DefaultedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.HashCache;
import net.minecraft.resources.ResourceLocation;

public class RegistryDumpReport implements DataProvider {
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private final DataGenerator generator;

   public RegistryDumpReport(DataGenerator pGenerator) {
      this.generator = pGenerator;
   }

   /**
    * Performs this provider's action.
    */
   public void run(HashCache pCache) throws IOException {
      JsonObject jsonobject = new JsonObject();
      Registry.REGISTRY.keySet().forEach((p_124057_) -> {
         jsonobject.add(p_124057_.toString(), dumpRegistry(Registry.REGISTRY.get(p_124057_)));
      });
      Path path = this.generator.getOutputFolder().resolve("reports/registries.json");
      DataProvider.save(GSON, pCache, jsonobject, path);
   }

   private static <T> JsonElement dumpRegistry(Registry<T> pRegistry) {
      JsonObject jsonobject = new JsonObject();
      if (pRegistry instanceof DefaultedRegistry) {
         ResourceLocation resourcelocation = ((DefaultedRegistry)pRegistry).getDefaultKey();
         jsonobject.addProperty("default", resourcelocation.toString());
      }

      int j = ((Registry)Registry.REGISTRY).getId(pRegistry);
      jsonobject.addProperty("protocol_id", j);
      JsonObject jsonobject1 = new JsonObject();

      for(ResourceLocation resourcelocation1 : pRegistry.keySet()) {
         T t = pRegistry.get(resourcelocation1);
         int i = pRegistry.getId(t);
         JsonObject jsonobject2 = new JsonObject();
         jsonobject2.addProperty("protocol_id", i);
         jsonobject1.add(resourcelocation1.toString(), jsonobject2);
      }

      jsonobject.add("entries", jsonobject1);
      return jsonobject;
   }

   /**
    * Gets a name for this provider, to use in logging.
    */
   public String getName() {
      return "Registry Dump";
   }
}