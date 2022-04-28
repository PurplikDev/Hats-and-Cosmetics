package net.minecraft.client.sounds;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.resources.sounds.SoundEventRegistrationSerializer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SoundManager extends SimplePreparableReloadListener<SoundManager.Preparations> {
   public static final Sound EMPTY_SOUND = new Sound("meta:missing_sound", 1.0F, 1.0F, 1, Sound.Type.FILE, false, false, 16);
   static final Logger LOGGER = LogManager.getLogger();
   private static final String SOUNDS_PATH = "sounds.json";
   private static final Gson GSON = (new GsonBuilder()).registerTypeHierarchyAdapter(Component.class, new Component.Serializer()).registerTypeAdapter(SoundEventRegistration.class, new SoundEventRegistrationSerializer()).create();
   private static final TypeToken<Map<String, SoundEventRegistration>> SOUND_EVENT_REGISTRATION_TYPE = new TypeToken<Map<String, SoundEventRegistration>>() {
   };
   private final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();
   private final SoundEngine soundEngine;

   public SoundManager(ResourceManager pResourceManager, Options pOptions) {
      this.soundEngine = new SoundEngine(this, pOptions, pResourceManager);
   }

   /**
    * Performs any reloading that can be done off-thread, such as file IO
    */
   protected SoundManager.Preparations prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      SoundManager.Preparations soundmanager$preparations = new SoundManager.Preparations();
      pProfiler.startTick();

      for(String s : pResourceManager.getNamespaces()) {
         pProfiler.push(s);

         try {
            for(Resource resource : pResourceManager.getResources(new ResourceLocation(s, "sounds.json"))) {
               pProfiler.push(resource.getSourceName());

               try {
                  InputStream inputstream = resource.getInputStream();

                  try {
                     Reader reader = new InputStreamReader(inputstream, StandardCharsets.UTF_8);

                     try {
                        pProfiler.push("parse");
                        Map<String, SoundEventRegistration> map = GsonHelper.fromJson(GSON, reader, SOUND_EVENT_REGISTRATION_TYPE);
                        pProfiler.popPush("register");

                        for(Entry<String, SoundEventRegistration> entry : map.entrySet()) {
                           soundmanager$preparations.handleRegistration(new ResourceLocation(s, entry.getKey()), entry.getValue(), pResourceManager);
                        }

                        pProfiler.pop();
                     } catch (Throwable throwable2) {
                        try {
                           reader.close();
                        } catch (Throwable throwable1) {
                           throwable2.addSuppressed(throwable1);
                        }

                        throw throwable2;
                     }

                     reader.close();
                  } catch (Throwable throwable3) {
                     if (inputstream != null) {
                        try {
                           inputstream.close();
                        } catch (Throwable throwable) {
                           throwable3.addSuppressed(throwable);
                        }
                     }

                     throw throwable3;
                  }

                  if (inputstream != null) {
                     inputstream.close();
                  }
               } catch (RuntimeException runtimeexception) {
                  LOGGER.warn("Invalid {} in resourcepack: '{}'", "sounds.json", resource.getSourceName(), runtimeexception);
               }

               pProfiler.pop();
            }
         } catch (IOException ioexception) {
         }

         pProfiler.pop();
      }

      pProfiler.endTick();
      return soundmanager$preparations;
   }

   protected void apply(SoundManager.Preparations pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
      pObject.apply(this.registry, this.soundEngine);
      if (SharedConstants.IS_RUNNING_IN_IDE) {
         for(ResourceLocation resourcelocation : this.registry.keySet()) {
            WeighedSoundEvents weighedsoundevents = this.registry.get(resourcelocation);
            if (weighedsoundevents.getSubtitle() instanceof TranslatableComponent) {
               String s = ((TranslatableComponent)weighedsoundevents.getSubtitle()).getKey();
               if (!I18n.exists(s) && Registry.SOUND_EVENT.containsKey(resourcelocation)) {
                  LOGGER.error("Missing subtitle {} for sound event: {}", s, resourcelocation);
               }
            }
         }
      }

      if (LOGGER.isDebugEnabled()) {
         for(ResourceLocation resourcelocation1 : this.registry.keySet()) {
            if (!Registry.SOUND_EVENT.containsKey(resourcelocation1)) {
               LOGGER.debug("Not having sound event for: {}", (Object)resourcelocation1);
            }
         }
      }

      this.soundEngine.reload();
   }

   public List<String> getAvailableSoundDevices() {
      return this.soundEngine.getAvailableSoundDevices();
   }

   static boolean validateSoundResource(Sound pSound, ResourceLocation pSoundLocation, ResourceManager pResourceManager) {
      ResourceLocation resourcelocation = pSound.getPath();
      if (!pResourceManager.hasResource(resourcelocation)) {
         LOGGER.warn("File {} does not exist, cannot add it to event {}", resourcelocation, pSoundLocation);
         return false;
      } else {
         return true;
      }
   }

   @Nullable
   public WeighedSoundEvents getSoundEvent(ResourceLocation pLocation) {
      return this.registry.get(pLocation);
   }

   public Collection<ResourceLocation> getAvailableSounds() {
      return this.registry.keySet();
   }

   public void queueTickingSound(TickableSoundInstance pTickableSound) {
      this.soundEngine.queueTickingSound(pTickableSound);
   }

   /**
    * Play a sound
    */
   public void play(SoundInstance pSound) {
      this.soundEngine.play(pSound);
   }

   /**
    * Plays the sound in n ticks
    */
   public void playDelayed(SoundInstance pSound, int pDelay) {
      this.soundEngine.playDelayed(pSound, pDelay);
   }

   public void updateSource(Camera pActiveRenderInfo) {
      this.soundEngine.updateSource(pActiveRenderInfo);
   }

   public void pause() {
      this.soundEngine.pause();
   }

   public void stop() {
      this.soundEngine.stopAll();
   }

   public void destroy() {
      this.soundEngine.destroy();
   }

   public void tick(boolean pIsGamePaused) {
      this.soundEngine.tick(pIsGamePaused);
   }

   public void resume() {
      this.soundEngine.resume();
   }

   public void updateSourceVolume(SoundSource pCategory, float pVolume) {
      if (pCategory == SoundSource.MASTER && pVolume <= 0.0F) {
         this.stop();
      }

      this.soundEngine.updateCategoryVolume(pCategory, pVolume);
   }

   public void stop(SoundInstance pSound) {
      this.soundEngine.stop(pSound);
   }

   public boolean isActive(SoundInstance pSound) {
      return this.soundEngine.isActive(pSound);
   }

   public void addListener(SoundEventListener pListener) {
      this.soundEngine.addEventListener(pListener);
   }

   public void removeListener(SoundEventListener pListener) {
      this.soundEngine.removeEventListener(pListener);
   }

   public void stop(@Nullable ResourceLocation pId, @Nullable SoundSource pCategory) {
      this.soundEngine.stop(pId, pCategory);
   }

   public String getDebugString() {
      return this.soundEngine.getDebugString();
   }

   public void reload() {
      this.soundEngine.reload();
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Preparations {
      final Map<ResourceLocation, WeighedSoundEvents> registry = Maps.newHashMap();

      void handleRegistration(ResourceLocation pSoundLocation, SoundEventRegistration pSoundList, ResourceManager pResourceManager) {
         WeighedSoundEvents weighedsoundevents = this.registry.get(pSoundLocation);
         boolean flag = weighedsoundevents == null;
         if (flag || pSoundList.isReplace()) {
            if (!flag) {
               SoundManager.LOGGER.debug("Replaced sound event location {}", (Object)pSoundLocation);
            }

            weighedsoundevents = new WeighedSoundEvents(pSoundLocation, pSoundList.getSubtitle());
            this.registry.put(pSoundLocation, weighedsoundevents);
         }

         for(final Sound sound : pSoundList.getSounds()) {
            final ResourceLocation resourcelocation = sound.getLocation();
            Weighted<Sound> weighted;
            switch(sound.getType()) {
            case FILE:
               if (!SoundManager.validateSoundResource(sound, pSoundLocation, pResourceManager)) {
                  continue;
               }

               weighted = sound;
               break;
            case SOUND_EVENT:
               weighted = new Weighted<Sound>() {
                  public int getWeight() {
                     WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(resourcelocation);
                     return weighedsoundevents1 == null ? 0 : weighedsoundevents1.getWeight();
                  }

                  public Sound getSound() {
                     WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(resourcelocation);
                     if (weighedsoundevents1 == null) {
                        return SoundManager.EMPTY_SOUND;
                     } else {
                        Sound sound1 = weighedsoundevents1.getSound();
                        return new Sound(sound1.getLocation().toString(), sound1.getVolume() * sound.getVolume(), sound1.getPitch() * sound.getPitch(), sound.getWeight(), Sound.Type.FILE, sound1.shouldStream() || sound.shouldStream(), sound1.shouldPreload(), sound1.getAttenuationDistance());
                     }
                  }

                  public void preloadIfRequired(SoundEngine p_120438_) {
                     WeighedSoundEvents weighedsoundevents1 = Preparations.this.registry.get(resourcelocation);
                     if (weighedsoundevents1 != null) {
                        weighedsoundevents1.preloadIfRequired(p_120438_);
                     }
                  }
               };
               break;
            default:
               throw new IllegalStateException("Unknown SoundEventRegistration type: " + sound.getType());
            }

            weighedsoundevents.addSound(weighted);
         }

      }

      public void apply(Map<ResourceLocation, WeighedSoundEvents> pSoundRegistry, SoundEngine pSoundManager) {
         pSoundRegistry.clear();

         for(Entry<ResourceLocation, WeighedSoundEvents> entry : this.registry.entrySet()) {
            pSoundRegistry.put(entry.getKey(), entry.getValue());
            entry.getValue().preloadIfRequired(pSoundManager);
         }

      }
   }
}