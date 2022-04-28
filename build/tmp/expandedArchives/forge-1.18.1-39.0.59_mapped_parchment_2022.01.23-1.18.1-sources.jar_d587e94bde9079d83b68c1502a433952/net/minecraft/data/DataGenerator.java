package net.minecraft.data;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.Bootstrap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataGenerator {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Collection<Path> inputFolders;
   private final Path outputFolder;
   private final List<DataProvider> providers = Lists.newArrayList();
   private final List<DataProvider> providerView = java.util.Collections.unmodifiableList(providers);

   public DataGenerator(Path pOutputFolder, Collection<Path> pInputFolders) {
      this.outputFolder = pOutputFolder;
      this.inputFolders = Lists.newArrayList(pInputFolders);
   }

   /**
    * Gets a collection of folders to look for data to convert in
    */
   public Collection<Path> getInputFolders() {
      return this.inputFolders;
   }

   /**
    * Gets the location to put generated data into
    */
   public Path getOutputFolder() {
      return this.outputFolder;
   }

   /**
    * Runs all the previously registered data providors.
    */
   public void run() throws IOException {
      HashCache hashcache = new HashCache(this.outputFolder, "cache");
      hashcache.keep(this.getOutputFolder().resolve("version.json"));
      Stopwatch stopwatch = Stopwatch.createStarted();
      Stopwatch stopwatch1 = Stopwatch.createUnstarted();

      for(DataProvider dataprovider : this.providers) {
         LOGGER.info("Starting provider: {}", (Object)dataprovider.getName());
         net.minecraftforge.fml.StartupMessageManager.addModMessage("Generating: " + dataprovider.getName());
         stopwatch1.start();
         dataprovider.run(hashcache);
         stopwatch1.stop();
         LOGGER.info("{} finished after {} ms", dataprovider.getName(), stopwatch1.elapsed(TimeUnit.MILLISECONDS));
         stopwatch1.reset();
      }

      LOGGER.info("All providers took: {} ms", (long)stopwatch.elapsed(TimeUnit.MILLISECONDS));
      hashcache.purgeStaleAndWrite();
   }

   /**
    * Adds a data provider to the list of providers to run
    */
   public void addProvider(DataProvider pProvider) {
      this.providers.add(pProvider);
   }

   public List<DataProvider> getProviders() {
       return this.providerView;
   }

   public void addInput(Path value) {
      this.inputFolders.add(value);
   }

   static {
      Bootstrap.bootStrap();
   }
}
