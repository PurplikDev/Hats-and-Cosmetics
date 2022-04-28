package net.minecraft.data;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public interface DataProvider {
   HashFunction SHA1 = Hashing.sha1();

   /**
    * Performs this provider's action.
    */
   void run(HashCache pCache) throws IOException;

   /**
    * Gets a name for this provider, to use in logging.
    */
   String getName();

   static void save(Gson pGson, HashCache pCache, JsonElement pJsonElement, Path pPath) throws IOException {
      String s = pGson.toJson(pJsonElement);
      String s1 = SHA1.hashUnencodedChars(s).toString();
      if (!Objects.equals(pCache.getHash(pPath), s1) || !Files.exists(pPath)) {
         Files.createDirectories(pPath.getParent());
         BufferedWriter bufferedwriter = Files.newBufferedWriter(pPath);

         try {
            bufferedwriter.write(s);
         } catch (Throwable throwable1) {
            if (bufferedwriter != null) {
               try {
                  bufferedwriter.close();
               } catch (Throwable throwable) {
                  throwable1.addSuppressed(throwable);
               }
            }

            throw throwable1;
         }

         if (bufferedwriter != null) {
            bufferedwriter.close();
         }
      }

      pCache.putNew(pPath, s1);
   }
}