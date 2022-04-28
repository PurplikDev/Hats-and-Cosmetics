package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddVibrationSignalPacket;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.players.SleepStatus;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.CsvOutput;
import net.minecraft.util.Mth;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ReputationEventHandler;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.entity.LevelCallback;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListenerRegistrar;
import net.minecraft.world.level.gameevent.vibrations.VibrationPath;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.PortalForcer;
import net.minecraft.world.level.saveddata.maps.MapIndex;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.LevelTicks;
import org.slf4j.Logger;

public class ServerLevel extends Level implements WorldGenLevel {
   public static final BlockPos END_SPAWN_POINT = new BlockPos(100, 50, 0);
   private static final int MIN_RAIN_DELAY_TIME = 12000;
   private static final int MAX_RAIN_DELAY_TIME = 180000;
   private static final int MIN_RAIN_TIME = 12000;
   private static final int MAX_RAIN_TIME = 24000;
   private static final int MIN_THUNDER_DELAY_TIME = 12000;
   private static final int MAX_THUNDER_DELAY_TIME = 180000;
   private static final int MIN_THUNDER_TIME = 3600;
   private static final int MAX_THUNDER_TIME = 15600;
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int EMPTY_TIME_NO_TICK = 300;
   private static final int MAX_SCHEDULED_TICKS_PER_TICK = 65536;
   final List<ServerPlayer> players = Lists.newArrayList();
   private final ServerChunkCache chunkSource;
   private final MinecraftServer server;
   private final ServerLevelData serverLevelData;
   final EntityTickList entityTickList = new EntityTickList();
   private final PersistentEntitySectionManager<Entity> entityManager;
   public boolean noSave;
   private final SleepStatus sleepStatus;
   private int emptyTime;
   private final PortalForcer portalForcer;
   private final LevelTicks<Block> blockTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
   private final LevelTicks<Fluid> fluidTicks = new LevelTicks<>(this::isPositionTickingWithEntitiesLoaded, this.getProfilerSupplier());
   final Set<Mob> navigatingMobs = new ObjectOpenHashSet<>();
   volatile boolean isUpdatingNavigations;
   protected final Raids raids;
   private final ObjectLinkedOpenHashSet<BlockEventData> blockEvents = new ObjectLinkedOpenHashSet<>();
   private final List<BlockEventData> blockEventsToReschedule = new ArrayList<>(64);
   private boolean handlingTick;
   private final List<CustomSpawner> customSpawners;
   @Nullable
   private final EndDragonFight dragonFight;
   final Int2ObjectMap<net.minecraftforge.entity.PartEntity<?>> dragonParts = new Int2ObjectOpenHashMap<>();
   private final StructureFeatureManager structureFeatureManager;
   private final StructureCheck structureCheck;
   private final boolean tickTime;
   private net.minecraftforge.common.util.WorldCapabilityData capabilityData;

   public ServerLevel(MinecraftServer p_203762_, Executor p_203763_, LevelStorageSource.LevelStorageAccess p_203764_, ServerLevelData p_203765_, ResourceKey<Level> p_203766_, Holder<DimensionType> p_203767_, ChunkProgressListener p_203768_, ChunkGenerator p_203769_, boolean p_203770_, long p_203771_, List<CustomSpawner> p_203772_, boolean p_203773_) {
      super(p_203765_, p_203766_, p_203767_, p_203762_::getProfiler, false, p_203770_, p_203771_);
      this.tickTime = p_203773_;
      this.server = p_203762_;
      this.customSpawners = p_203772_;
      this.serverLevelData = p_203765_;
      p_203769_.ensureStructuresGenerated();
      boolean flag = p_203762_.forceSynchronousWrites();
      DataFixer datafixer = p_203762_.getFixerUpper();
      EntityPersistentStorage<Entity> entitypersistentstorage = new EntityStorage(this, p_203764_.getDimensionPath(p_203766_).resolve("entities"), datafixer, flag, p_203762_);
      this.entityManager = new PersistentEntitySectionManager<>(Entity.class, new ServerLevel.EntityCallbacks(), entitypersistentstorage);
      this.chunkSource = new ServerChunkCache(this, p_203764_, datafixer, p_203762_.getStructureManager(), p_203763_, p_203769_, p_203762_.getPlayerList().getViewDistance(), p_203762_.getPlayerList().getSimulationDistance(), flag, p_203768_, this.entityManager::updateChunkStatus, () -> {
         return p_203762_.overworld().getDataStorage();
      });
      this.portalForcer = new PortalForcer(this);
      this.updateSkyBrightness();
      this.prepareWeather();
      this.getWorldBorder().setAbsoluteMaxSize(p_203762_.getAbsoluteMaxWorldSize());
      this.raids = this.getDataStorage().computeIfAbsent((p_184095_) -> {
         return Raids.load(this, p_184095_);
      }, () -> {
         return new Raids(this);
      }, Raids.getFileId(this.dimensionTypeRegistration()));
      if (!p_203762_.isSingleplayer()) {
         p_203765_.setGameType(p_203762_.getDefaultGameType());
      }

      long i = p_203762_.getWorldData().worldGenSettings().seed();
      this.structureCheck = new StructureCheck(this.chunkSource.chunkScanner(), this.registryAccess(), p_203762_.getStructureManager(), p_203766_, p_203769_, this, p_203769_.getBiomeSource(), i, datafixer);
      this.structureFeatureManager = new StructureFeatureManager(this, p_203762_.getWorldData().worldGenSettings(), this.structureCheck);
      if (this.dimensionType().createDragonFight()) {
         this.dragonFight = new EndDragonFight(this, i, p_203762_.getWorldData().endDragonFightData());
      } else {
         this.dragonFight = null;
      }

      this.sleepStatus = new SleepStatus();
      this.initCapabilities();
   }

   public void setWeatherParameters(int pClearTime, int pWeatherTime, boolean pIsRaining, boolean pIsThundering) {
      this.serverLevelData.setClearWeatherTime(pClearTime);
      this.serverLevelData.setRainTime(pWeatherTime);
      this.serverLevelData.setThunderTime(pWeatherTime);
      this.serverLevelData.setRaining(pIsRaining);
      this.serverLevelData.setThundering(pIsThundering);
   }

   public Holder<Biome> getUncachedNoiseBiome(int pX, int pY, int pZ) {
      return this.getChunkSource().getGenerator().getNoiseBiome(pX, pY, pZ);
   }

   public StructureFeatureManager structureFeatureManager() {
      return this.structureFeatureManager;
   }

   /**
    * Runs a single tick for the world
    */
   public void tick(BooleanSupplier pHasTimeLeft) {
      ProfilerFiller profilerfiller = this.getProfiler();
      this.handlingTick = true;
      profilerfiller.push("world border");
      this.getWorldBorder().tick();
      profilerfiller.popPush("weather");
      this.advanceWeatherCycle();
      int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
      if (this.sleepStatus.areEnoughSleeping(i) && this.sleepStatus.areEnoughDeepSleeping(i, this.players)) {
         if (this.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            long j = this.getDayTime() + 24000L;
            this.setDayTime(net.minecraftforge.event.ForgeEventFactory.onSleepFinished(this, j - j % 24000L, this.getDayTime()));
         }

         this.wakeUpAllPlayers();
         if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE) && this.isRaining()) {
            this.resetWeatherCycle();
         }
      }

      this.updateSkyBrightness();
      this.tickTime();
      profilerfiller.popPush("tickPending");
      if (!this.isDebug()) {
         long k = this.getGameTime();
         profilerfiller.push("blockTicks");
         this.blockTicks.tick(k, 65536, this::tickBlock);
         profilerfiller.popPush("fluidTicks");
         this.fluidTicks.tick(k, 65536, this::tickFluid);
         profilerfiller.pop();
      }

      profilerfiller.popPush("raid");
      this.raids.tick();
      profilerfiller.popPush("chunkSource");
      this.getChunkSource().tick(pHasTimeLeft, true);
      profilerfiller.popPush("blockEvents");
      this.runBlockEvents();
      this.handlingTick = false;
      profilerfiller.pop();
      boolean flag = !this.players.isEmpty() || net.minecraftforge.common.world.ForgeChunkManager.hasForcedChunks(this); //Forge: Replace vanilla's has forced chunk check with forge's that checks both the vanilla and forge added ones
      if (flag) {
         this.resetEmptyTime();
      }

      if (flag || this.emptyTime++ < 300) {
         profilerfiller.push("entities");
         if (this.dragonFight != null) {
            profilerfiller.push("dragonFight");
            this.dragonFight.tick();
            profilerfiller.pop();
         }

         this.entityTickList.forEach((p_184065_) -> {
            if (!p_184065_.isRemoved()) {
               if (this.shouldDiscardEntity(p_184065_)) {
                  p_184065_.discard();
               } else {
                  profilerfiller.push("checkDespawn");
                  p_184065_.checkDespawn();
                  profilerfiller.pop();
                  if (this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(p_184065_.chunkPosition().toLong())) {
                     Entity entity = p_184065_.getVehicle();
                     if (entity != null) {
                        if (!entity.isRemoved() && entity.hasPassenger(p_184065_)) {
                           return;
                        }

                        p_184065_.stopRiding();
                     }

                     profilerfiller.push("tick");
                     if (!p_184065_.isRemoved() && !(p_184065_ instanceof net.minecraftforge.entity.PartEntity)) {
                        this.guardEntityTick(this::tickNonPassenger, p_184065_);
                     }
                     profilerfiller.pop();
                  }
               }
            }
         });
         profilerfiller.pop();
         this.tickBlockEntities();
      }

      profilerfiller.push("entityManagement");
      this.entityManager.tick();
      profilerfiller.pop();
   }

   public boolean shouldTickBlocksAt(long p_184059_) {
      return this.chunkSource.chunkMap.getDistanceManager().inBlockTickingRange(p_184059_);
   }

   protected void tickTime() {
      if (this.tickTime) {
         long i = this.levelData.getGameTime() + 1L;
         this.serverLevelData.setGameTime(i);
         this.serverLevelData.getScheduledEvents().tick(this.server, i);
         if (this.levelData.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT)) {
            this.setDayTime(this.levelData.getDayTime() + 1L);
         }

      }
   }

   public void setDayTime(long pTime) {
      this.serverLevelData.setDayTime(pTime);
   }

   public void tickCustomSpawners(boolean pSpawnEnemies, boolean pSpawnFriendlies) {
      for(CustomSpawner customspawner : this.customSpawners) {
         customspawner.tick(this, pSpawnEnemies, pSpawnFriendlies);
      }

   }

   private boolean shouldDiscardEntity(Entity p_143343_) {
      if (this.server.isSpawningAnimals() || !(p_143343_ instanceof Animal) && !(p_143343_ instanceof WaterAnimal)) {
         return !this.server.areNpcsEnabled() && p_143343_ instanceof Npc;
      } else {
         return true;
      }
   }

   private void wakeUpAllPlayers() {
      this.sleepStatus.removeAllSleepers();
      this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList()).forEach((p_184116_) -> {
         p_184116_.stopSleepInBed(false, false);
      });
   }

   public void tickChunk(LevelChunk pChunk, int pRandomTickSpeed) {
      ChunkPos chunkpos = pChunk.getPos();
      boolean flag = this.isRaining();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      ProfilerFiller profilerfiller = this.getProfiler();
      profilerfiller.push("thunder");
      if (flag && this.isThundering() && this.random.nextInt(100000) == 0) {
         BlockPos blockpos = this.findLightningTargetAround(this.getBlockRandomPos(i, 0, j, 15));
         if (this.isRainingAt(blockpos)) {
            DifficultyInstance difficultyinstance = this.getCurrentDifficultyAt(blockpos);
            boolean flag1 = this.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING) && this.random.nextDouble() < (double)difficultyinstance.getEffectiveDifficulty() * 0.01D && !this.getBlockState(blockpos.below()).is(Blocks.LIGHTNING_ROD);
            if (flag1) {
               SkeletonHorse skeletonhorse = EntityType.SKELETON_HORSE.create(this);
               skeletonhorse.setTrap(true);
               skeletonhorse.setAge(0);
               skeletonhorse.setPos((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
               this.addFreshEntity(skeletonhorse);
            }

            LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(this);
            lightningbolt.moveTo(Vec3.atBottomCenterOf(blockpos));
            lightningbolt.setVisualOnly(flag1);
            this.addFreshEntity(lightningbolt);
         }
      }

      profilerfiller.popPush("iceandsnow");
      if (this.random.nextInt(16) == 0) {
         BlockPos blockpos2 = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, this.getBlockRandomPos(i, 0, j, 15));
         BlockPos blockpos3 = blockpos2.below();
         Biome biome = this.getBiome(blockpos2).value();
         if (this.isAreaLoaded(blockpos2, 1)) // Forge: check area to avoid loading neighbors in unloaded chunks
         if (biome.shouldFreeze(this, blockpos3)) {
            this.setBlockAndUpdate(blockpos3, Blocks.ICE.defaultBlockState());
         }

         if (flag) {
            if (biome.shouldSnow(this, blockpos2)) {
               this.setBlockAndUpdate(blockpos2, Blocks.SNOW.defaultBlockState());
            }

            BlockState blockstate1 = this.getBlockState(blockpos3);
            Biome.Precipitation biome$precipitation = biome.getPrecipitation();
            if (biome$precipitation == Biome.Precipitation.RAIN && biome.coldEnoughToSnow(blockpos3)) {
               biome$precipitation = Biome.Precipitation.SNOW;
            }

            blockstate1.getBlock().handlePrecipitation(blockstate1, this, blockpos3, biome$precipitation);
         }
      }

      profilerfiller.popPush("tickBlocks");
      if (pRandomTickSpeed > 0) {
         for(LevelChunkSection levelchunksection : pChunk.getSections()) {
            if (levelchunksection.isRandomlyTicking()) {
               int l = levelchunksection.bottomBlockY();

               for(int k = 0; k < pRandomTickSpeed; ++k) {
                  BlockPos blockpos1 = this.getBlockRandomPos(i, l, j, 15);
                  profilerfiller.push("randomTick");
                  BlockState blockstate = levelchunksection.getBlockState(blockpos1.getX() - i, blockpos1.getY() - l, blockpos1.getZ() - j);
                  if (blockstate.isRandomlyTicking()) {
                     blockstate.randomTick(this, blockpos1, this.random);
                  }

                  FluidState fluidstate = blockstate.getFluidState();
                  if (fluidstate.isRandomlyTicking()) {
                     fluidstate.randomTick(this, blockpos1, this.random);
                  }

                  profilerfiller.pop();
               }
            }
         }
      }

      profilerfiller.pop();
   }

   private Optional<BlockPos> findLightningRod(BlockPos p_143249_) {
      Optional<BlockPos> optional = this.getPoiManager().findClosest((p_184069_) -> {
         return p_184069_ == PoiType.LIGHTNING_ROD;
      }, (p_184055_) -> {
         return p_184055_.getY() == this.getLevel().getHeight(Heightmap.Types.WORLD_SURFACE, p_184055_.getX(), p_184055_.getZ()) - 1;
      }, p_143249_, 128, PoiManager.Occupancy.ANY);
      return optional.map((p_184053_) -> {
         return p_184053_.above(1);
      });
   }

   protected BlockPos findLightningTargetAround(BlockPos p_143289_) {
      BlockPos blockpos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p_143289_);
      Optional<BlockPos> optional = this.findLightningRod(blockpos);
      if (optional.isPresent()) {
         return optional.get();
      } else {
         AABB aabb = (new AABB(blockpos, new BlockPos(blockpos.getX(), this.getMaxBuildHeight(), blockpos.getZ()))).inflate(3.0D);
         List<LivingEntity> list = this.getEntitiesOfClass(LivingEntity.class, aabb, (p_184067_) -> {
            return p_184067_ != null && p_184067_.isAlive() && this.canSeeSky(p_184067_.blockPosition());
         });
         if (!list.isEmpty()) {
            return list.get(this.random.nextInt(list.size())).blockPosition();
         } else {
            if (blockpos.getY() == this.getMinBuildHeight() - 1) {
               blockpos = blockpos.above(2);
            }

            return blockpos;
         }
      }
   }

   public boolean isHandlingTick() {
      return this.handlingTick;
   }

   public boolean canSleepThroughNights() {
      return this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE) <= 100;
   }

   private void announceSleepStatus() {
      if (this.canSleepThroughNights()) {
         if (!this.getServer().isSingleplayer() || this.getServer().isPublished()) {
            int i = this.getGameRules().getInt(GameRules.RULE_PLAYERS_SLEEPING_PERCENTAGE);
            Component component;
            if (this.sleepStatus.areEnoughSleeping(i)) {
               component = new TranslatableComponent("sleep.skipping_night");
            } else {
               component = new TranslatableComponent("sleep.players_sleeping", this.sleepStatus.amountSleeping(), this.sleepStatus.sleepersNeeded(i));
            }

            for(ServerPlayer serverplayer : this.players) {
               serverplayer.displayClientMessage(component, true);
            }

         }
      }
   }

   /**
    * Updates the flag that indicates whether or not all players in the world are sleeping.
    */
   public void updateSleepingPlayerList() {
      if (!this.players.isEmpty() && this.sleepStatus.update(this.players)) {
         this.announceSleepStatus();
      }

   }

   public ServerScoreboard getScoreboard() {
      return this.server.getScoreboard();
   }

   private void advanceWeatherCycle() {
      boolean flag = this.isRaining();
      if (this.dimensionType().hasSkyLight()) {
         if (this.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
            int i = this.serverLevelData.getClearWeatherTime();
            int j = this.serverLevelData.getThunderTime();
            int k = this.serverLevelData.getRainTime();
            boolean flag1 = this.levelData.isThundering();
            boolean flag2 = this.levelData.isRaining();
            if (i > 0) {
               --i;
               j = flag1 ? 0 : 1;
               k = flag2 ? 0 : 1;
               flag1 = false;
               flag2 = false;
            } else {
               if (j > 0) {
                  --j;
                  if (j == 0) {
                     flag1 = !flag1;
                  }
               } else if (flag1) {
                  j = Mth.randomBetweenInclusive(this.random, 3600, 15600);
               } else {
                  j = Mth.randomBetweenInclusive(this.random, 12000, 180000);
               }

               if (k > 0) {
                  --k;
                  if (k == 0) {
                     flag2 = !flag2;
                  }
               } else if (flag2) {
                  k = Mth.randomBetweenInclusive(this.random, 12000, 24000);
               } else {
                  k = Mth.randomBetweenInclusive(this.random, 12000, 180000);
               }
            }

            this.serverLevelData.setThunderTime(j);
            this.serverLevelData.setRainTime(k);
            this.serverLevelData.setClearWeatherTime(i);
            this.serverLevelData.setThundering(flag1);
            this.serverLevelData.setRaining(flag2);
         }

         this.oThunderLevel = this.thunderLevel;
         if (this.levelData.isThundering()) {
            this.thunderLevel += 0.01F;
         } else {
            this.thunderLevel -= 0.01F;
         }

         this.thunderLevel = Mth.clamp(this.thunderLevel, 0.0F, 1.0F);
         this.oRainLevel = this.rainLevel;
         if (this.levelData.isRaining()) {
            this.rainLevel += 0.01F;
         } else {
            this.rainLevel -= 0.01F;
         }

         this.rainLevel = Mth.clamp(this.rainLevel, 0.0F, 1.0F);
      }

      if (this.oRainLevel != this.rainLevel) {
         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
      }

      if (this.oThunderLevel != this.thunderLevel) {
         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
      }

      /* The function in use here has been replaced in order to only send the weather info to players in the correct dimension,
       * rather than to all players on the server. This is what causes the client-side rain, as the
       * client believes that it has started raining locally, rather than in another dimension.
       */
      if (flag != this.isRaining()) {
         if (flag) {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F), this.dimension());
         } else {
            this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F), this.dimension());
         }

         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.rainLevel), this.dimension());
         this.server.getPlayerList().broadcastAll(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, this.thunderLevel), this.dimension());
      }

   }

   private void resetWeatherCycle() {
      this.serverLevelData.setRainTime(0);
      this.serverLevelData.setRaining(false);
      this.serverLevelData.setThunderTime(0);
      this.serverLevelData.setThundering(false);
   }

   /**
    * Resets the updateEntityTick field to 0
    */
   public void resetEmptyTime() {
      this.emptyTime = 0;
   }

   private void tickFluid(BlockPos p_184077_, Fluid p_184078_) {
      FluidState fluidstate = this.getFluidState(p_184077_);
      if (fluidstate.is(p_184078_)) {
         fluidstate.tick(this, p_184077_);
      }

   }

   private void tickBlock(BlockPos p_184113_, Block p_184114_) {
      BlockState blockstate = this.getBlockState(p_184113_);
      if (blockstate.is(p_184114_)) {
         blockstate.tick(this, p_184113_, this.random);
      }

   }

   public void tickNonPassenger(Entity p_8648_) {
      p_8648_.setOldPosAndRot();
      ProfilerFiller profilerfiller = this.getProfiler();
      ++p_8648_.tickCount;
      this.getProfiler().push(() -> {
         return Registry.ENTITY_TYPE.getKey(p_8648_.getType()).toString();
      });
      profilerfiller.incrementCounter("tickNonPassenger");
      p_8648_.tick();
      this.getProfiler().pop();

      for(Entity entity : p_8648_.getPassengers()) {
         this.tickPassenger(p_8648_, entity);
      }

   }

   private void tickPassenger(Entity pRidingEntity, Entity pPassengerEntity) {
      if (!pPassengerEntity.isRemoved() && pPassengerEntity.getVehicle() == pRidingEntity) {
         if (pPassengerEntity instanceof Player || this.entityTickList.contains(pPassengerEntity)) {
            pPassengerEntity.setOldPosAndRot();
            ++pPassengerEntity.tickCount;
            ProfilerFiller profilerfiller = this.getProfiler();
            profilerfiller.push(() -> {
               return pPassengerEntity.getType().getRegistryName() == null ? pPassengerEntity.getType().toString() : pPassengerEntity.getType().getRegistryName().toString();
            });
            profilerfiller.incrementCounter("tickPassenger");
            if (pPassengerEntity.canUpdate())
            pPassengerEntity.rideTick();
            profilerfiller.pop();

            for(Entity entity : pPassengerEntity.getPassengers()) {
               this.tickPassenger(pPassengerEntity, entity);
            }

         }
      } else {
         pPassengerEntity.stopRiding();
      }
   }

   public boolean mayInteract(Player pPlayer, BlockPos pPos) {
      return !this.server.isUnderSpawnProtection(this, pPos, pPlayer) && this.getWorldBorder().isWithinBounds(pPos);
   }

   public void save(@Nullable ProgressListener pProgress, boolean pFlush, boolean pSkipSave) {
      ServerChunkCache serverchunkcache = this.getChunkSource();
      if (!pSkipSave) {
         if (pProgress != null) {
            pProgress.progressStartNoAbort(new TranslatableComponent("menu.savingLevel"));
         }

         this.saveLevelData();
         if (pProgress != null) {
            pProgress.progressStage(new TranslatableComponent("menu.savingChunks"));
         }

         serverchunkcache.save(pFlush);
         if (pFlush) {
            this.entityManager.saveAll();
         } else {
            this.entityManager.autoSave();
         }

         net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Save(this));
      }
   }

   /**
    * Saves the chunks to disk.
    */
   private void saveLevelData() {
      if (this.dragonFight != null) {
         this.server.getWorldData().setEndDragonFightData(this.dragonFight.saveData());
      }

      this.getChunkSource().getDataStorage().save();
   }

   public <T extends Entity> List<? extends T> getEntities(EntityTypeTest<Entity, T> p_143281_, Predicate<? super T> p_143282_) {
      List<T> list = Lists.newArrayList();
      this.getEntities().get(p_143281_, (p_184091_) -> {
         if (p_143282_.test(p_184091_)) {
            list.add(p_184091_);
         }

      });
      return list;
   }

   public List<? extends EnderDragon> getDragons() {
      return this.getEntities(EntityType.ENDER_DRAGON, LivingEntity::isAlive);
   }

   public List<ServerPlayer> getPlayers(Predicate<? super ServerPlayer> pPredicate) {
      List<ServerPlayer> list = Lists.newArrayList();

      for(ServerPlayer serverplayer : this.players) {
         if (pPredicate.test(serverplayer)) {
            list.add(serverplayer);
         }
      }

      return list;
   }

   @Nullable
   public ServerPlayer getRandomPlayer() {
      List<ServerPlayer> list = this.getPlayers(LivingEntity::isAlive);
      return list.isEmpty() ? null : list.get(this.random.nextInt(list.size()));
   }

   public boolean addFreshEntity(Entity pEntity) {
      return this.addEntity(pEntity);
   }

   /**
    * Used for "unnatural" ways of entities appearing in the world, e.g. summon command, interdimensional teleports
    */
   public boolean addWithUUID(Entity pEntity) {
      return this.addEntity(pEntity);
   }

   public void addDuringTeleport(Entity p_143335_) {
      this.addEntity(p_143335_);
   }

   public void addDuringCommandTeleport(ServerPlayer pPlayer) {
      this.addPlayer(pPlayer);
   }

   public void addDuringPortalTeleport(ServerPlayer pPlayer) {
      this.addPlayer(pPlayer);
   }

   public void addNewPlayer(ServerPlayer pPlayer) {
      this.addPlayer(pPlayer);
   }

   public void addRespawnedPlayer(ServerPlayer pPlayer) {
      this.addPlayer(pPlayer);
   }

   /** @deprecated To be removed in 1.19. See {@link #removePlayerImmediately(ServerPlayer, Entity.RemovalReason)} as a possible replacement. */
   @Deprecated(forRemoval = true, since = "1.18.1")
   public void removePlayer(ServerPlayer p_8850_, boolean keepData) {
      p_8850_.discard();
      this.removeEntity(p_8850_, keepData);
   }

   /** @deprecated To be removed in 1.19. See {@link Entity#setRemoved(Entity.RemovalReason)} as a possible replacement. */
   @Deprecated(forRemoval = true, since = "1.18.1")
   public void removeEntityComplete(Entity p_8865_, boolean keepData) {
      if(p_8865_.isMultipartEntity()) {
         for(net.minecraftforge.entity.PartEntity<?> parts : p_8865_.getParts()) {
            parts.discard();
         }
      }

      this.getChunkSource().removeEntity(p_8865_);
      if (p_8865_ instanceof ServerPlayer) {
         ServerPlayer serverplayerentity = (ServerPlayer)p_8865_;
         this.players.remove(serverplayerentity);
      }

      this.getScoreboard().entityRemoved(p_8865_);
      if (p_8865_ instanceof Mob) {
         this.navigatingMobs.remove(((Mob)p_8865_).getNavigation());
      }

      p_8865_.discard();
      // onRemovedFromWorld and EntityLeaveWorldEvent are called by discard, so no need to do it ourselves here lest we double-call
   }

   /** @deprecated To be removed in 1.19. See {@link Entity#setRemoved(Entity.RemovalReason)} as a possible replacement. */
   @Deprecated(forRemoval = true, since = "1.18.1")
   public void removeEntity(Entity entity) {
      removeEntity(entity, false);
   }

   /** @deprecated To be removed in 1.19. See {@link Entity#setRemoved(Entity.RemovalReason)} as a possible replacement. */
   @Deprecated(forRemoval = true, since = "1.18.1")
   public void removeEntity(Entity p_8868_, boolean keepData) {
      if (this.handlingTick) {
         throw (IllegalStateException) net.minecraft.Util.pauseInIde(new IllegalStateException("Removing entity while ticking!"));
      } else {
         removeEntityComplete(p_8868_, keepData);
      }
   }

   private void addPlayer(ServerPlayer pPlayer) {
      if (net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityJoinWorldEvent(pPlayer, this))) return;
      Entity entity = this.getEntities().get(pPlayer.getUUID());
      if (entity != null) {
         LOGGER.warn("Force-added player with duplicate UUID {}", (Object)pPlayer.getUUID().toString());
         entity.unRide();
         this.removePlayerImmediately((ServerPlayer)entity, Entity.RemovalReason.DISCARDED);
      }

      this.entityManager.addNewEntityWithoutEvent(pPlayer);
      pPlayer.onAddedToWorld();
   }

   /**
    * Called when an entity is spawned in the world. This includes players.
    */
   private boolean addEntity(Entity pEntity) {
      if (pEntity.isRemoved()) {
         LOGGER.warn("Tried to add entity {} but it was marked as removed already", (Object)EntityType.getKey(pEntity.getType()));
         return false;
      } else {
         if (this.entityManager.addNewEntity(pEntity)) {
            pEntity.onAddedToWorld();
            return true;
         } else {
            return false;
         }
      }
   }

   /**
    * Attempts to summon an entity and it's passangers. They will only be summoned if all entities are unique and not
    * already in queue to be summoned.
    */
   public boolean tryAddFreshEntityWithPassengers(Entity pEntity) {
      if (pEntity.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
         return false;
      } else {
         this.addFreshEntityWithPassengers(pEntity);
         return true;
      }
   }

   public void unload(LevelChunk pChunk) {
      pChunk.clearAllBlockEntities();
      pChunk.unregisterTickContainerFromLevel(this);
   }

   public void removePlayerImmediately(ServerPlayer p_143262_, Entity.RemovalReason p_143263_) {
      p_143262_.remove(p_143263_);
   }

   public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
      for(ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
         if (serverplayer != null && serverplayer.level == this && serverplayer.getId() != pBreakerId) {
            double d0 = (double)pPos.getX() - serverplayer.getX();
            double d1 = (double)pPos.getY() - serverplayer.getY();
            double d2 = (double)pPos.getZ() - serverplayer.getZ();
            if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0D) {
               serverplayer.connection.send(new ClientboundBlockDestructionPacket(pBreakerId, pPos, pProgress));
            }
         }
      }

   }

   public void playSound(@Nullable Player pPlayer, double pX, double pY, double pZ, SoundEvent pSound, SoundSource pCategory, float pVolume, float pPitch) {
      net.minecraftforge.event.entity.PlaySoundAtEntityEvent event = net.minecraftforge.event.ForgeEventFactory.onPlaySoundAtEntity(pPlayer, pSound, pCategory, pVolume, pPitch);
      if (event.isCanceled() || event.getSound() == null) return;
      pSound = event.getSound();
      pCategory = event.getCategory();
      pVolume = event.getVolume();
      this.server.getPlayerList().broadcast(pPlayer, pX, pY, pZ, pVolume > 1.0F ? (double)(16.0F * pVolume) : 16.0D, this.dimension(), new ClientboundSoundPacket(pSound, pCategory, pX, pY, pZ, pVolume, pPitch));
   }

   public void playSound(@Nullable Player pPlayer, Entity pEntity, SoundEvent pEvent, SoundSource pCategory, float pVolume, float pPitch) {
      net.minecraftforge.event.entity.PlaySoundAtEntityEvent event = net.minecraftforge.event.ForgeEventFactory.onPlaySoundAtEntity(pPlayer, pEvent, pCategory, pVolume, pPitch);
      if (event.isCanceled() || event.getSound() == null) return;
      pEvent = event.getSound();
      pCategory = event.getCategory();
      pVolume = event.getVolume();
      this.server.getPlayerList().broadcast(pPlayer, pEntity.getX(), pEntity.getY(), pEntity.getZ(), pVolume > 1.0F ? (double)(16.0F * pVolume) : 16.0D, this.dimension(), new ClientboundSoundEntityPacket(pEvent, pCategory, pEntity, pVolume, pPitch));
   }

   public void globalLevelEvent(int pId, BlockPos pPos, int pData) {
      this.server.getPlayerList().broadcastAll(new ClientboundLevelEventPacket(pId, pPos, pData, true));
   }

   public void levelEvent(@Nullable Player pPlayer, int pType, BlockPos pPos, int pData) {
      this.server.getPlayerList().broadcast(pPlayer, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ(), 64.0D, this.dimension(), new ClientboundLevelEventPacket(pType, pPos, pData, false));
   }

   public int getLogicalHeight() {
      return this.dimensionType().logicalHeight();
   }

   public void gameEvent(@Nullable Entity pEntity, GameEvent pEvent, BlockPos pPos) {
      if (net.minecraftforge.common.ForgeHooks.onVanillaGameEvent(this, pEntity, pEvent, pPos))
      this.postGameEventInRadius(pEntity, pEvent, pPos, pEvent.getNotificationRadius());
   }

   /**
    * Flags are as in setBlockState
    */
   public void sendBlockUpdated(BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
      if (this.isUpdatingNavigations) {
         String s = "recursive call to sendBlockUpdated";
         Util.logAndPauseIfInIde("recursive call to sendBlockUpdated", new IllegalStateException("recursive call to sendBlockUpdated"));
      }

      this.getChunkSource().blockChanged(pPos);
      VoxelShape voxelshape1 = pOldState.getCollisionShape(this, pPos);
      VoxelShape voxelshape = pNewState.getCollisionShape(this, pPos);
      if (Shapes.joinIsNotEmpty(voxelshape1, voxelshape, BooleanOp.NOT_SAME)) {
         List<PathNavigation> list = new ObjectArrayList<>();

         for(Mob mob : this.navigatingMobs) {
            PathNavigation pathnavigation = mob.getNavigation();
            if (pathnavigation.shouldRecomputePath(pPos)) {
               list.add(pathnavigation);
            }
         }

         try {
            this.isUpdatingNavigations = true;

            for(PathNavigation pathnavigation1 : list) {
               pathnavigation1.recomputePath();
            }
         } finally {
            this.isUpdatingNavigations = false;
         }

      }
   }

   /**
    * sends a Packet 38 (Entity Status) to all tracked players of that entity
    */
   public void broadcastEntityEvent(Entity pEntity, byte pState) {
      this.getChunkSource().broadcastAndSend(pEntity, new ClientboundEntityEventPacket(pEntity, pState));
   }

   /**
    * Gets the world's chunk provider
    */
   public ServerChunkCache getChunkSource() {
      return this.chunkSource;
   }

   public Explosion explode(@Nullable Entity pExploder, @Nullable DamageSource pDamageSource, @Nullable ExplosionDamageCalculator pContext, double pX, double pY, double pZ, float pSize, boolean pCausesFire, Explosion.BlockInteraction pMode) {
      Explosion explosion = new Explosion(this, pExploder, pDamageSource, pContext, pX, pY, pZ, pSize, pCausesFire, pMode);
      if (net.minecraftforge.event.ForgeEventFactory.onExplosionStart(this, explosion)) return explosion;
      explosion.explode();
      explosion.finalizeExplosion(false);
      if (pMode == Explosion.BlockInteraction.NONE) {
         explosion.clearToBlow();
      }

      for(ServerPlayer serverplayer : this.players) {
         if (serverplayer.distanceToSqr(pX, pY, pZ) < 4096.0D) {
            serverplayer.connection.send(new ClientboundExplodePacket(pX, pY, pZ, pSize, explosion.getToBlow(), explosion.getHitPlayers().get(serverplayer)));
         }
      }

      return explosion;
   }

   public void blockEvent(BlockPos pPos, Block pBlock, int pEventID, int pEventParam) {
      this.blockEvents.add(new BlockEventData(pPos, pBlock, pEventID, pEventParam));
   }

   private void runBlockEvents() {
      this.blockEventsToReschedule.clear();

      while(!this.blockEvents.isEmpty()) {
         BlockEventData blockeventdata = this.blockEvents.removeFirst();
         if (this.shouldTickBlocksAt(ChunkPos.asLong(blockeventdata.pos()))) {
            if (this.doBlockEvent(blockeventdata)) {
               this.server.getPlayerList().broadcast((Player)null, (double)blockeventdata.pos().getX(), (double)blockeventdata.pos().getY(), (double)blockeventdata.pos().getZ(), 64.0D, this.dimension(), new ClientboundBlockEventPacket(blockeventdata.pos(), blockeventdata.block(), blockeventdata.paramA(), blockeventdata.paramB()));
            }
         } else {
            this.blockEventsToReschedule.add(blockeventdata);
         }
      }

      this.blockEvents.addAll(this.blockEventsToReschedule);
   }

   private boolean doBlockEvent(BlockEventData pEvent) {
      BlockState blockstate = this.getBlockState(pEvent.pos());
      return blockstate.is(pEvent.block()) ? blockstate.triggerEvent(this, pEvent.pos(), pEvent.paramA(), pEvent.paramB()) : false;
   }

   public LevelTicks<Block> getBlockTicks() {
      return this.blockTicks;
   }

   public LevelTicks<Fluid> getFluidTicks() {
      return this.fluidTicks;
   }

   @Nonnull
   public MinecraftServer getServer() {
      return this.server;
   }

   public PortalForcer getPortalForcer() {
      return this.portalForcer;
   }

   public StructureManager getStructureManager() {
      return this.server.getStructureManager();
   }

   public void sendVibrationParticle(VibrationPath p_143284_) {
      BlockPos blockpos = p_143284_.getOrigin();
      ClientboundAddVibrationSignalPacket clientboundaddvibrationsignalpacket = new ClientboundAddVibrationSignalPacket(p_143284_);
      this.players.forEach((p_184082_) -> {
         this.sendParticles(p_184082_, false, (double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ(), clientboundaddvibrationsignalpacket);
      });
   }

   public <T extends ParticleOptions> int sendParticles(T pType, double pPosX, double pPosY, double pPosZ, int pParticleCount, double pXOffset, double pYOffset, double pZOffset, double pSpeed) {
      ClientboundLevelParticlesPacket clientboundlevelparticlespacket = new ClientboundLevelParticlesPacket(pType, false, pPosX, pPosY, pPosZ, (float)pXOffset, (float)pYOffset, (float)pZOffset, (float)pSpeed, pParticleCount);
      int i = 0;

      for(int j = 0; j < this.players.size(); ++j) {
         ServerPlayer serverplayer = this.players.get(j);
         if (this.sendParticles(serverplayer, false, pPosX, pPosY, pPosZ, clientboundlevelparticlespacket)) {
            ++i;
         }
      }

      return i;
   }

   public <T extends ParticleOptions> boolean sendParticles(ServerPlayer pPlayer, T pType, boolean pLongDistance, double pPosX, double pPosY, double pPosZ, int pParticleCount, double pXOffset, double pYOffset, double pZOffset, double pSpeed) {
      Packet<?> packet = new ClientboundLevelParticlesPacket(pType, pLongDistance, pPosX, pPosY, pPosZ, (float)pXOffset, (float)pYOffset, (float)pZOffset, (float)pSpeed, pParticleCount);
      return this.sendParticles(pPlayer, pLongDistance, pPosX, pPosY, pPosZ, packet);
   }

   private boolean sendParticles(ServerPlayer pPlayer, boolean pLongDistance, double pPosX, double pPosY, double pPosZ, Packet<?> pPacket) {
      if (pPlayer.getLevel() != this) {
         return false;
      } else {
         BlockPos blockpos = pPlayer.blockPosition();
         if (blockpos.closerToCenterThan(new Vec3(pPosX, pPosY, pPosZ), pLongDistance ? 512.0D : 32.0D)) {
            pPlayer.connection.send(pPacket);
            return true;
         } else {
            return false;
         }
      }
   }

   /**
    * Returns the Entity with the given ID, or null if it doesn't exist in this World.
    */
   @Nullable
   public Entity getEntity(int pId) {
      return this.getEntities().get(pId);
   }

   /** @deprecated */
   @Deprecated
   @Nullable
   public Entity getEntityOrPart(int p_143318_) {
      Entity entity = this.getEntities().get(p_143318_);
      return entity != null ? entity : this.dragonParts.get(p_143318_);
   }

   @Nullable
   public Entity getEntity(UUID pUniqueId) {
      return this.getEntities().get(pUniqueId);
   }

   @Nullable
   public BlockPos findNearestMapFeature(TagKey<ConfiguredStructureFeature<?, ?>> pStructureTag, BlockPos pPos, int pRadius, boolean pSkipExistingChunks) {
      if (!this.server.getWorldData().worldGenSettings().generateFeatures()) {
         return null;
      } else {
         Optional<HolderSet.Named<ConfiguredStructureFeature<?, ?>>> optional = this.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY).getTag(pStructureTag);
         if (optional.isEmpty()) {
            return null;
         } else {
            Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair = this.getChunkSource().getGenerator().findNearestMapFeature(this, optional.get(), pPos, pRadius, pSkipExistingChunks);
            return pair != null ? pair.getFirst() : null;
         }
      }
   }

   @Nullable
   public Pair<BlockPos, Holder<Biome>> findNearestBiome(Predicate<Holder<Biome>> pBiomePredicate, BlockPos pPos, int pRadius, int pIncrement) {
      return this.getChunkSource().getGenerator().getBiomeSource().findBiomeHorizontal(pPos.getX(), pPos.getY(), pPos.getZ(), pRadius, pIncrement, pBiomePredicate, this.random, true, this.getChunkSource().getGenerator().climateSampler());
   }

   public RecipeManager getRecipeManager() {
      return this.server.getRecipeManager();
   }

   public boolean noSave() {
      return this.noSave;
   }

   public RegistryAccess registryAccess() {
      return this.server.registryAccess();
   }

   public DimensionDataStorage getDataStorage() {
      return this.getChunkSource().getDataStorage();
   }

   @Nullable
   public MapItemSavedData getMapData(String pMapName) {
      return this.getServer().overworld().getDataStorage().get(MapItemSavedData::load, pMapName);
   }

   public void setMapData(String pMapId, MapItemSavedData pData) {
      this.getServer().overworld().getDataStorage().set(pMapId, pData);
   }

   public int getFreeMapId() {
      return this.getServer().overworld().getDataStorage().computeIfAbsent(MapIndex::load, MapIndex::new, "idcounts").getFreeAuxValueForMap();
   }

   public void setDefaultSpawnPos(BlockPos pPos, float pAngle) {
      ChunkPos chunkpos = new ChunkPos(new BlockPos(this.levelData.getXSpawn(), 0, this.levelData.getZSpawn()));
      this.levelData.setSpawn(pPos, pAngle);
      this.getChunkSource().removeRegionTicket(TicketType.START, chunkpos, 11, Unit.INSTANCE);
      this.getChunkSource().addRegionTicket(TicketType.START, new ChunkPos(pPos), 11, Unit.INSTANCE);
      this.getServer().getPlayerList().broadcastAll(new ClientboundSetDefaultSpawnPositionPacket(pPos, pAngle));
   }

   public BlockPos getSharedSpawnPos() {
      BlockPos blockpos = new BlockPos(this.levelData.getXSpawn(), this.levelData.getYSpawn(), this.levelData.getZSpawn());
      if (!this.getWorldBorder().isWithinBounds(blockpos)) {
         blockpos = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(this.getWorldBorder().getCenterX(), 0.0D, this.getWorldBorder().getCenterZ()));
      }

      return blockpos;
   }

   public float getSharedSpawnAngle() {
      return this.levelData.getSpawnAngle();
   }

   public LongSet getForcedChunks() {
      ForcedChunksSavedData forcedchunkssaveddata = this.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
      return (LongSet)(forcedchunkssaveddata != null ? LongSets.unmodifiable(forcedchunkssaveddata.getChunks()) : LongSets.EMPTY_SET);
   }

   public boolean setChunkForced(int pChunkX, int pChunkZ, boolean pAdd) {
      ForcedChunksSavedData forcedchunkssaveddata = this.getDataStorage().computeIfAbsent(ForcedChunksSavedData::load, ForcedChunksSavedData::new, "chunks");
      ChunkPos chunkpos = new ChunkPos(pChunkX, pChunkZ);
      long i = chunkpos.toLong();
      boolean flag;
      if (pAdd) {
         flag = forcedchunkssaveddata.getChunks().add(i);
         if (flag) {
            this.getChunk(pChunkX, pChunkZ);
         }
      } else {
         flag = forcedchunkssaveddata.getChunks().remove(i);
      }

      forcedchunkssaveddata.setDirty(flag);
      if (flag) {
         this.getChunkSource().updateChunkForced(chunkpos, pAdd);
      }

      return flag;
   }

   public List<ServerPlayer> players() {
      return this.players;
   }

   public void onBlockStateChange(BlockPos pPos, BlockState pBlockState, BlockState pNewState) {
      Optional<PoiType> optional = PoiType.forState(pBlockState);
      Optional<PoiType> optional1 = PoiType.forState(pNewState);
      if (!Objects.equals(optional, optional1)) {
         BlockPos blockpos = pPos.immutable();
         optional.ifPresent((p_207583_) -> {
            this.getServer().execute(() -> {
               this.getPoiManager().remove(blockpos);
               DebugPackets.sendPoiRemovedPacket(this, blockpos);
            });
         });
         optional1.ifPresent((p_184106_) -> {
            this.getServer().execute(() -> {
               this.getPoiManager().add(blockpos, p_184106_);
               DebugPackets.sendPoiAddedPacket(this, blockpos);
            });
         });
      }
   }

   public PoiManager getPoiManager() {
      return this.getChunkSource().getPoiManager();
   }

   public boolean isVillage(BlockPos pPos) {
      return this.isCloseToVillage(pPos, 1);
   }

   public boolean isVillage(SectionPos pPos) {
      return this.isVillage(pPos.center());
   }

   public boolean isCloseToVillage(BlockPos pPos, int pSections) {
      if (pSections > 6) {
         return false;
      } else {
         return this.sectionsToVillage(SectionPos.of(pPos)) <= pSections;
      }
   }

   public int sectionsToVillage(SectionPos pPos) {
      return this.getPoiManager().sectionsToVillage(pPos);
   }

   public Raids getRaids() {
      return this.raids;
   }

   @Nullable
   public Raid getRaidAt(BlockPos pPos) {
      return this.raids.getNearbyRaid(pPos, 9216);
   }

   public boolean isRaided(BlockPos pPos) {
      return this.getRaidAt(pPos) != null;
   }

   public void onReputationEvent(ReputationEventType pType, Entity pTarget, ReputationEventHandler pHost) {
      pHost.onReputationEventFrom(pType, pTarget);
   }

   public void saveDebugReport(Path pPath) throws IOException {
      ChunkMap chunkmap = this.getChunkSource().chunkMap;
      Writer writer = Files.newBufferedWriter(pPath.resolve("stats.txt"));

      try {
         writer.write(String.format("spawning_chunks: %d\n", chunkmap.getDistanceManager().getNaturalSpawnChunkCount()));
         NaturalSpawner.SpawnState naturalspawner$spawnstate = this.getChunkSource().getLastSpawnState();
         if (naturalspawner$spawnstate != null) {
            for(Entry<MobCategory> entry : naturalspawner$spawnstate.getMobCategoryCounts().object2IntEntrySet()) {
               writer.write(String.format("spawn_count.%s: %d\n", entry.getKey().getName(), entry.getIntValue()));
            }
         }

         writer.write(String.format("entities: %s\n", this.entityManager.gatherStats()));
         writer.write(String.format("block_entity_tickers: %d\n", this.blockEntityTickers.size()));
         writer.write(String.format("block_ticks: %d\n", this.getBlockTicks().count()));
         writer.write(String.format("fluid_ticks: %d\n", this.getFluidTicks().count()));
         writer.write("distance_manager: " + chunkmap.getDistanceManager().getDebugStatus() + "\n");
         writer.write(String.format("pending_tasks: %d\n", this.getChunkSource().getPendingTasksCount()));
      } catch (Throwable throwable11) {
         if (writer != null) {
            try {
               writer.close();
            } catch (Throwable throwable5) {
               throwable11.addSuppressed(throwable5);
            }
         }

         throw throwable11;
      }

      if (writer != null) {
         writer.close();
      }

      CrashReport crashreport = new CrashReport("Level dump", new Exception("dummy"));
      this.fillReportDetails(crashreport);
      Writer writer3 = Files.newBufferedWriter(pPath.resolve("example_crash.txt"));

      try {
         writer3.write(crashreport.getFriendlyReport());
      } catch (Throwable throwable10) {
         if (writer3 != null) {
            try {
               writer3.close();
            } catch (Throwable throwable4) {
               throwable10.addSuppressed(throwable4);
            }
         }

         throw throwable10;
      }

      if (writer3 != null) {
         writer3.close();
      }

      Path path = pPath.resolve("chunks.csv");
      Writer writer4 = Files.newBufferedWriter(path);

      try {
         chunkmap.dumpChunks(writer4);
      } catch (Throwable throwable9) {
         if (writer4 != null) {
            try {
               writer4.close();
            } catch (Throwable throwable3) {
               throwable9.addSuppressed(throwable3);
            }
         }

         throw throwable9;
      }

      if (writer4 != null) {
         writer4.close();
      }

      Path path1 = pPath.resolve("entity_chunks.csv");
      Writer writer5 = Files.newBufferedWriter(path1);

      try {
         this.entityManager.dumpSections(writer5);
      } catch (Throwable throwable8) {
         if (writer5 != null) {
            try {
               writer5.close();
            } catch (Throwable throwable2) {
               throwable8.addSuppressed(throwable2);
            }
         }

         throw throwable8;
      }

      if (writer5 != null) {
         writer5.close();
      }

      Path path2 = pPath.resolve("entities.csv");
      Writer writer1 = Files.newBufferedWriter(path2);

      try {
         dumpEntities(writer1, this.getEntities().getAll());
      } catch (Throwable throwable7) {
         if (writer1 != null) {
            try {
               writer1.close();
            } catch (Throwable throwable1) {
               throwable7.addSuppressed(throwable1);
            }
         }

         throw throwable7;
      }

      if (writer1 != null) {
         writer1.close();
      }

      Path path3 = pPath.resolve("block_entities.csv");
      Writer writer2 = Files.newBufferedWriter(path3);

      try {
         this.dumpBlockEntityTickers(writer2);
      } catch (Throwable throwable6) {
         if (writer2 != null) {
            try {
               writer2.close();
            } catch (Throwable throwable) {
               throwable6.addSuppressed(throwable);
            }
         }

         throw throwable6;
      }

      if (writer2 != null) {
         writer2.close();
      }

   }

   private static void dumpEntities(Writer pWriter, Iterable<Entity> pEntities) throws IOException {
      CsvOutput csvoutput = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("uuid").addColumn("type").addColumn("alive").addColumn("display_name").addColumn("custom_name").build(pWriter);

      for(Entity entity : pEntities) {
         Component component = entity.getCustomName();
         Component component1 = entity.getDisplayName();
         csvoutput.writeRow(entity.getX(), entity.getY(), entity.getZ(), entity.getUUID(), Registry.ENTITY_TYPE.getKey(entity.getType()), entity.isAlive(), component1.getString(), component != null ? component.getString() : null);
      }

   }

   private void dumpBlockEntityTickers(Writer p_143300_) throws IOException {
      CsvOutput csvoutput = CsvOutput.builder().addColumn("x").addColumn("y").addColumn("z").addColumn("type").build(p_143300_);

      for(TickingBlockEntity tickingblockentity : this.blockEntityTickers) {
         BlockPos blockpos = tickingblockentity.getPos();
         csvoutput.writeRow(blockpos.getX(), blockpos.getY(), blockpos.getZ(), tickingblockentity.getType());
      }

   }

   @VisibleForTesting
   public void clearBlockEvents(BoundingBox pBoundingBox) {
      this.blockEvents.removeIf((p_207568_) -> {
         return pBoundingBox.isInside(p_207568_.pos());
      });
   }

   public void blockUpdated(BlockPos pPos, Block pBlock) {
      if (!this.isDebug()) {
         this.updateNeighborsAt(pPos, pBlock);
      }

   }

   public float getShade(Direction pDirection, boolean pShade) {
      return 1.0F;
   }

   /**
    * Gets an unmodifiable iterator of all loaded entities in the world.
    */
   public Iterable<Entity> getAllEntities() {
      return this.getEntities().getAll();
   }

   public String toString() {
      return "ServerLevel[" + this.serverLevelData.getLevelName() + "]";
   }

   public boolean isFlat() {
      return this.server.getWorldData().worldGenSettings().isFlatWorld();
   }

   /**
    * gets the random world seed
    */
   public long getSeed() {
      return this.server.getWorldData().worldGenSettings().seed();
   }

   @Nullable
   public EndDragonFight dragonFight() {
      return this.dragonFight;
   }

   public ServerLevel getLevel() {
      return this;
   }

   @VisibleForTesting
   public String getWatchdogStats() {
      return String.format("players: %s, entities: %s [%s], block_entities: %d [%s], block_ticks: %d, fluid_ticks: %d, chunk_source: %s", this.players.size(), this.entityManager.gatherStats(), getTypeCount(this.entityManager.getEntityGetter().getAll(), (p_207585_) -> {
         return Registry.ENTITY_TYPE.getKey(p_207585_.getType()).toString();
      }), this.blockEntityTickers.size(), getTypeCount(this.blockEntityTickers, TickingBlockEntity::getType), this.getBlockTicks().count(), this.getFluidTicks().count(), this.gatherChunkSourceStats());
   }

   private static <T> String getTypeCount(Iterable<T> p_143302_, Function<T, String> p_143303_) {
      try {
         Object2IntOpenHashMap<String> object2intopenhashmap = new Object2IntOpenHashMap<>();

         for(T t : p_143302_) {
            String s = p_143303_.apply(t);
            object2intopenhashmap.addTo(s, 1);
         }

         return object2intopenhashmap.object2IntEntrySet().stream().sorted(Comparator.<Entry<String>,Integer>comparing(Entry::getIntValue).reversed()).limit(5L).map((p_207570_) -> {
            return (String)p_207570_.getKey() + ":" + p_207570_.getIntValue();
         }).collect(Collectors.joining(","));
      } catch (Exception exception) {
         return "";
      }
   }

   public static void makeObsidianPlatform(ServerLevel pServerLevel) {
      BlockPos blockpos = END_SPAWN_POINT;
      int i = blockpos.getX();
      int j = blockpos.getY() - 2;
      int k = blockpos.getZ();
      BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((p_207578_) -> {
         pServerLevel.setBlockAndUpdate(p_207578_, Blocks.AIR.defaultBlockState());
      });
      BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach((p_184101_) -> {
         pServerLevel.setBlockAndUpdate(p_184101_, Blocks.OBSIDIAN.defaultBlockState());
      });
   }

   protected void initCapabilities() {
      this.gatherCapabilities();
      capabilityData = this.getDataStorage().computeIfAbsent(e -> net.minecraftforge.common.util.WorldCapabilityData.load(e, getCapabilities()), () -> new net.minecraftforge.common.util.WorldCapabilityData(getCapabilities()), net.minecraftforge.common.util.WorldCapabilityData.ID);
      capabilityData.setCapabilities(getCapabilities());
   }

   public LevelEntityGetter<Entity> getEntities() {
      return this.entityManager.getEntityGetter();
   }

   public void addLegacyChunkEntities(Stream<Entity> p_143312_) {
      this.entityManager.addLegacyChunkEntities(p_143312_);
   }

   public void addWorldGenChunkEntities(Stream<Entity> p_143328_) {
      this.entityManager.addWorldGenChunkEntities(p_143328_);
   }

   public void startTickingChunk(LevelChunk p_184103_) {
      p_184103_.unpackTicks(this.getLevelData().getGameTime());
   }

   public void onStructureStartsAvailable(ChunkAccess p_196558_) {
      this.server.execute(() -> {
         this.structureCheck.onStructureLoad(p_196558_.getPos(), p_196558_.getAllStarts());
      });
   }

   public void close() throws IOException {
      super.close();
      this.entityManager.close();
   }

   /**
    * Returns the name of the current chunk provider, by calling chunkprovider.makeString()
    */
   public String gatherChunkSourceStats() {
      return "Chunks[S] W: " + this.chunkSource.gatherStats() + " E: " + this.entityManager.gatherStats();
   }

   public boolean areEntitiesLoaded(long p_143320_) {
      return this.entityManager.areEntitiesLoaded(p_143320_);
   }

   private boolean isPositionTickingWithEntitiesLoaded(long p_184111_) {
      return this.areEntitiesLoaded(p_184111_) && this.chunkSource.isPositionTicking(p_184111_);
   }

   public boolean isPositionEntityTicking(BlockPos p_143341_) {
      return this.entityManager.canPositionTick(p_143341_) && this.chunkSource.chunkMap.getDistanceManager().inEntityTickingRange(ChunkPos.asLong(p_143341_));
   }

   public boolean isNaturalSpawningAllowed(BlockPos p_201919_) {
      return this.entityManager.canPositionTick(p_201919_);
   }

   public boolean isNaturalSpawningAllowed(ChunkPos p_201917_) {
      return this.entityManager.canPositionTick(p_201917_);
   }

   final class EntityCallbacks implements LevelCallback<Entity> {
      public void onCreated(Entity p_143355_) {
      }

      public void onDestroyed(Entity p_143359_) {
         ServerLevel.this.getScoreboard().entityRemoved(p_143359_);
      }

      public void onTickingStart(Entity p_143363_) {
         ServerLevel.this.entityTickList.add(p_143363_);
      }

      public void onTickingEnd(Entity p_143367_) {
         ServerLevel.this.entityTickList.remove(p_143367_);
      }

      public void onTrackingStart(Entity p_143371_) {
         ServerLevel.this.getChunkSource().addEntity(p_143371_);
         if (p_143371_ instanceof ServerPlayer) {
            ServerPlayer serverplayer = (ServerPlayer)p_143371_;
            ServerLevel.this.players.add(serverplayer);
            ServerLevel.this.updateSleepingPlayerList();
         }

         if (p_143371_ instanceof Mob) {
            Mob mob = (Mob)p_143371_;
            if (ServerLevel.this.isUpdatingNavigations) {
               String s = "onTrackingStart called during navigation iteration";
               Util.logAndPauseIfInIde("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
            }

            ServerLevel.this.navigatingMobs.add(mob);
         }

         if (p_143371_.isMultipartEntity()) {
            for(net.minecraftforge.entity.PartEntity<?> enderdragonpart : p_143371_.getParts()) {
               ServerLevel.this.dragonParts.put(enderdragonpart.getId(), enderdragonpart);
            }
         }

      }

      public void onTrackingEnd(Entity p_143375_) {
         ServerLevel.this.getChunkSource().removeEntity(p_143375_);
         if (p_143375_ instanceof ServerPlayer) {
            ServerPlayer serverplayer = (ServerPlayer)p_143375_;
            ServerLevel.this.players.remove(serverplayer);
            ServerLevel.this.updateSleepingPlayerList();
         }

         if (p_143375_ instanceof Mob) {
            Mob mob = (Mob)p_143375_;
            if (ServerLevel.this.isUpdatingNavigations) {
               String s = "onTrackingStart called during navigation iteration";
               Util.logAndPauseIfInIde("onTrackingStart called during navigation iteration", new IllegalStateException("onTrackingStart called during navigation iteration"));
            }

            ServerLevel.this.navigatingMobs.remove(mob);
         }

         if (p_143375_.isMultipartEntity()) {
            for(net.minecraftforge.entity.PartEntity<?> enderdragonpart : p_143375_.getParts()) {
               ServerLevel.this.dragonParts.remove(enderdragonpart.getId());
            }
         }

         GameEventListenerRegistrar gameeventlistenerregistrar = p_143375_.getGameEventListenerRegistrar();
         if (gameeventlistenerregistrar != null) {
            gameeventlistenerregistrar.onListenerRemoved(p_143375_.level);
         }

         p_143375_.onRemovedFromWorld();
         net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.EntityLeaveWorldEvent(p_143375_, ServerLevel.this));
      }
   }
}
