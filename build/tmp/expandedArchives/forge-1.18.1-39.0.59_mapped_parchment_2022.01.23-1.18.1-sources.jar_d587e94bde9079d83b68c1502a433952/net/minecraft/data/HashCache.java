package net.minecraft.data;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class HashCache {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Path path;
   private final Path cachePath;
   private int hits;
   private final Map<Path, String> oldCache = Maps.newHashMap();
   private final Map<Path, String> newCache = Maps.newHashMap();
   private final Set<Path> keep = Sets.newHashSet();

   public HashCache(Path pPath, String pCacheFileName) throws IOException {
      this.path = pPath;
      Path path = pPath.resolve(".cache");
      Files.createDirectories(path);
      this.cachePath = path.resolve(pCacheFileName);
      this.walkOutputFiles().forEach((p_123959_) -> {
         this.oldCache.put(p_123959_, "");
      });
      if (Files.isReadable(this.cachePath)) {
         IOUtils.readLines(Files.newInputStream(this.cachePath), Charsets.UTF_8).forEach((p_123950_) -> {
            int i = p_123950_.indexOf(32);
            this.oldCache.put(pPath.resolve(p_123950_.substring(i + 1)), p_123950_.substring(0, i));
         });
      }

   }

   /**
    * Writes the cache file containing the hashes of newly created files to the disk, and deletes any stale files.
    */
   public void purgeStaleAndWrite() throws IOException {
      this.removeStale();

      Writer writer;
      try {
         writer = Files.newBufferedWriter(this.cachePath);
      } catch (IOException ioexception) {
         LOGGER.warn("Unable write cachefile {}: {}", this.cachePath, ioexception.toString());
         return;
      }

      IOUtils.writeLines(this.newCache.entrySet().stream().map((p_123944_) -> {
         return (String)p_123944_.getValue() + ' ' + this.path.relativize(p_123944_.getKey()).toString().replace('\\', '/'); //Forge: Standardize file paths.
      }).sorted(java.util.Comparator.comparing(a -> a.split(" ")[1])).collect(Collectors.toList()), System.lineSeparator(), writer);
      writer.close();
      LOGGER.debug("Caching: cache hits: {}, created: {} removed: {}", this.hits, this.newCache.size() - this.hits, this.oldCache.size());
   }

   /**
    * Gets the previous hash of a file, so that it doesn't need to be written to disk. Only meaningful before {@link
    * recordHash} has been called.
    * 
    * @return The hash that was recorded when {@link recordHash} was called on the previous run, or <code>null</code> if
    * the file does not exist, or an empty string if the file exists but was not recorded. Note that the hash is
    * <em>not</em> based on the current bytes on disk.
    */
   @Nullable
   public String getHash(Path pFilePath) {
      return this.oldCache.get(pFilePath);
   }

   /**
    * Inform the cache that a file has been written to {@code fileIn} with contents hashing to {@code hash}.
    */
   public void putNew(Path pFilePath, String pHash) {
      this.newCache.put(pFilePath, pHash);
      if (Objects.equals(this.oldCache.remove(pFilePath), pHash)) {
         ++this.hits;
      }

   }

   public boolean had(Path pFilePath) {
      return this.oldCache.containsKey(pFilePath);
   }

   public void keep(Path pFilePath) {
      this.keep.add(pFilePath);
   }

   private void removeStale() throws IOException {
      this.walkOutputFiles().forEach((p_123957_) -> {
         if (this.had(p_123957_) && !this.keep.contains(p_123957_)) {
            try {
               Files.delete(p_123957_);
            } catch (IOException ioexception) {
               LOGGER.debug("Unable to delete: {} ({})", p_123957_, ioexception.toString());
            }
         }

      });
   }

   /**
    * Gets all files in the directory, other than the cache file itself.
    */
   private Stream<Path> walkOutputFiles() throws IOException {
      return Files.walk(this.path).filter((p_123955_) -> {
         return !Objects.equals(this.cachePath, p_123955_) && !Files.isDirectory(p_123955_);
      });
   }
}
