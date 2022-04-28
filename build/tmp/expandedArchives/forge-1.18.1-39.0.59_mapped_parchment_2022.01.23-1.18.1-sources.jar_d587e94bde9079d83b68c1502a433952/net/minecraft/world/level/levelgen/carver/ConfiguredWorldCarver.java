package net.minecraft.world.level.levelgen.carver;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Aquifer;

public class ConfiguredWorldCarver<WC extends CarverConfiguration> {
   public static final Codec<ConfiguredWorldCarver<?>> DIRECT_CODEC = Registry.CARVER.byNameCodec().dispatch((p_64867_) -> {
      return p_64867_.worldCarver;
   }, WorldCarver::configuredCodec);
   public static final Codec<Supplier<ConfiguredWorldCarver<?>>> CODEC = RegistryFileCodec.create(Registry.CONFIGURED_CARVER_REGISTRY, DIRECT_CODEC);
   public static final Codec<List<Supplier<ConfiguredWorldCarver<?>>>> LIST_CODEC = RegistryFileCodec.homogeneousList(Registry.CONFIGURED_CARVER_REGISTRY, DIRECT_CODEC);
   private final WorldCarver<WC> worldCarver;
   private final WC config;

   public ConfiguredWorldCarver(WorldCarver<WC> pWorldCarver, WC pConfig) {
      this.worldCarver = pWorldCarver;
      this.config = pConfig;
   }

   public WC config() {
      return this.config;
   }

   public boolean isStartChunk(Random pRandom) {
      return this.worldCarver.isStartChunk(this.config, pRandom);
   }

   public boolean carve(CarvingContext pContext, ChunkAccess pChunk, Function<BlockPos, Biome> pBiomeAccessor, Random pRandom, Aquifer pAquifer, ChunkPos pChunkPos, CarvingMask pCarvingMask) {
      return SharedConstants.debugVoidTerrain(pChunk.getPos()) ? false : this.worldCarver.carve(pContext, this.config, pChunk, pBiomeAccessor, pRandom, pAquifer, pChunkPos, pCarvingMask);
   }
}