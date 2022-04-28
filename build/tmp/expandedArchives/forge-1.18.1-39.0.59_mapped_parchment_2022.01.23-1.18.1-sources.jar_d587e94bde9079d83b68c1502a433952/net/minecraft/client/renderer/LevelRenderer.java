package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final int CHUNK_SIZE = 16;
   private static final int HALF_CHUNK_SIZE = 8;
   private static final float SKY_DISC_RADIUS = 512.0F;
   private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
   private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0D) * 16.0D);
   private static final int MIN_FOG_DISTANCE = 32;
   private static final int RAIN_RADIUS = 10;
   private static final int RAIN_DIAMETER = 21;
   private static final int TRANSPARENT_SORT_COUNT = 15;
   private static final int HALF_A_SECOND_IN_MILLIS = 500;
   private static final ResourceLocation MOON_LOCATION = new ResourceLocation("textures/environment/moon_phases.png");
   private static final ResourceLocation SUN_LOCATION = new ResourceLocation("textures/environment/sun.png");
   private static final ResourceLocation CLOUDS_LOCATION = new ResourceLocation("textures/environment/clouds.png");
   private static final ResourceLocation END_SKY_LOCATION = new ResourceLocation("textures/environment/end_sky.png");
   private static final ResourceLocation FORCEFIELD_LOCATION = new ResourceLocation("textures/misc/forcefield.png");
   private static final ResourceLocation RAIN_LOCATION = new ResourceLocation("textures/environment/rain.png");
   private static final ResourceLocation SNOW_LOCATION = new ResourceLocation("textures/environment/snow.png");
   public static final Direction[] DIRECTIONS = Direction.values();
   private final Minecraft minecraft;
   private final EntityRenderDispatcher entityRenderDispatcher;
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final RenderBuffers renderBuffers;
   @Nullable
   private ClientLevel level;
   private final BlockingQueue<ChunkRenderDispatcher.RenderChunk> recentlyCompiledChunks = new LinkedBlockingQueue<>();
   private final AtomicReference<LevelRenderer.RenderChunkStorage> renderChunkStorage = new AtomicReference<>();
   private final ObjectArrayList<LevelRenderer.RenderChunkInfo> renderChunksInFrustum = new ObjectArrayList<>(10000);
   /**
    * Global block entities; these are always rendered, even if off-screen.
    * Any block entity is added to this if {@link
    * net.minecraft.client.renderer.blockentity.BlockEntityRenderer#shouldRenderOffScreen(net.minecraft.world.level.block.entity.BlockEntity)}
    * returns {@code true}.
    */
   private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
   @Nullable
   private Future<?> lastFullRenderChunkUpdate;
   @Nullable
   private ViewArea viewArea;
   @Nullable
   private VertexBuffer starBuffer;
   @Nullable
   private VertexBuffer skyBuffer;
   @Nullable
   private VertexBuffer darkBuffer;
   private boolean generateClouds = true;
   @Nullable
   private VertexBuffer cloudBuffer;
   private final RunningTrimmedMean frameTimes = new RunningTrimmedMean(100);
   private int ticks;
   private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
   private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
   private final Map<BlockPos, SoundInstance> playingRecords = Maps.newHashMap();
   @Nullable
   private RenderTarget entityTarget;
   @Nullable
   private PostChain entityEffect;
   @Nullable
   private RenderTarget translucentTarget;
   @Nullable
   private RenderTarget itemEntityTarget;
   @Nullable
   private RenderTarget particlesTarget;
   @Nullable
   private RenderTarget weatherTarget;
   @Nullable
   private RenderTarget cloudsTarget;
   @Nullable
   private PostChain transparencyChain;
   private double lastCameraX = Double.MIN_VALUE;
   private double lastCameraY = Double.MIN_VALUE;
   private double lastCameraZ = Double.MIN_VALUE;
   private int lastCameraChunkX = Integer.MIN_VALUE;
   private int lastCameraChunkY = Integer.MIN_VALUE;
   private int lastCameraChunkZ = Integer.MIN_VALUE;
   private double prevCamX = Double.MIN_VALUE;
   private double prevCamY = Double.MIN_VALUE;
   private double prevCamZ = Double.MIN_VALUE;
   private double prevCamRotX = Double.MIN_VALUE;
   private double prevCamRotY = Double.MIN_VALUE;
   private int prevCloudX = Integer.MIN_VALUE;
   private int prevCloudY = Integer.MIN_VALUE;
   private int prevCloudZ = Integer.MIN_VALUE;
   private Vec3 prevCloudColor = Vec3.ZERO;
   @Nullable
   private CloudStatus prevCloudsType;
   @Nullable
   private ChunkRenderDispatcher chunkRenderDispatcher;
   private int lastViewDistance = -1;
   private int renderedEntities;
   private int culledEntities;
   private Frustum cullingFrustum;
   private boolean captureFrustum;
   @Nullable
   private Frustum capturedFrustum;
   private final Vector4f[] frustumPoints = new Vector4f[8];
   private final Vector3d frustumPos = new Vector3d(0.0D, 0.0D, 0.0D);
   private double xTransparentOld;
   private double yTransparentOld;
   private double zTransparentOld;
   private boolean needsFullRenderChunkUpdate = true;
   private final AtomicLong nextFullUpdateMillis = new AtomicLong(0L);
   private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);
   private int rainSoundTime;
   private final float[] rainSizeX = new float[1024];
   private final float[] rainSizeZ = new float[1024];

   public LevelRenderer(Minecraft pMinecraft, RenderBuffers pRenderBuffers) {
      this.minecraft = pMinecraft;
      this.entityRenderDispatcher = pMinecraft.getEntityRenderDispatcher();
      this.blockEntityRenderDispatcher = pMinecraft.getBlockEntityRenderDispatcher();
      this.renderBuffers = pRenderBuffers;

      for(int i = 0; i < 32; ++i) {
         for(int j = 0; j < 32; ++j) {
            float f = (float)(j - 16);
            float f1 = (float)(i - 16);
            float f2 = Mth.sqrt(f * f + f1 * f1);
            this.rainSizeX[i << 5 | j] = -f1 / f2;
            this.rainSizeZ[i << 5 | j] = f / f2;
         }
      }

      this.createStars();
      this.createLightSky();
      this.createDarkSky();
   }

   private void renderSnowAndRain(LightTexture pLightTexture, float pPartialTick, double pCamX, double pCamY, double pCamZ) {
      net.minecraftforge.client.IWeatherRenderHandler renderHandler = level.effects().getWeatherRenderHandler();
      if (renderHandler != null) {
         renderHandler.render(ticks, pPartialTick, level, minecraft, pLightTexture, pCamX, pCamY, pCamZ);
         return;
      }
      float f = this.minecraft.level.getRainLevel(pPartialTick);
      if (!(f <= 0.0F)) {
         pLightTexture.turnOnLightLayer();
         Level level = this.minecraft.level;
         int i = Mth.floor(pCamX);
         int j = Mth.floor(pCamY);
         int k = Mth.floor(pCamZ);
         Tesselator tesselator = Tesselator.getInstance();
         BufferBuilder bufferbuilder = tesselator.getBuilder();
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.enableDepthTest();
         int l = 5;
         if (Minecraft.useFancyGraphics()) {
            l = 10;
         }

         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         int i1 = -1;
         float f1 = (float)this.ticks + pPartialTick;
         RenderSystem.setShader(GameRenderer::getParticleShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(int j1 = k - l; j1 <= k + l; ++j1) {
            for(int k1 = i - l; k1 <= i + l; ++k1) {
               int l1 = (j1 - k + 16) * 32 + k1 - i + 16;
               double d0 = (double)this.rainSizeX[l1] * 0.5D;
               double d1 = (double)this.rainSizeZ[l1] * 0.5D;
               blockpos$mutableblockpos.set((double)k1, pCamY, (double)j1);
               Biome biome = level.getBiome(blockpos$mutableblockpos);
               if (biome.getPrecipitation() != Biome.Precipitation.NONE) {
                  int i2 = level.getHeight(Heightmap.Types.MOTION_BLOCKING, k1, j1);
                  int j2 = j - l;
                  int k2 = j + l;
                  if (j2 < i2) {
                     j2 = i2;
                  }

                  if (k2 < i2) {
                     k2 = i2;
                  }

                  int l2 = i2;
                  if (i2 < j) {
                     l2 = j;
                  }

                  if (j2 != k2) {
                     Random random = new Random((long)(k1 * k1 * 3121 + k1 * 45238971 ^ j1 * j1 * 418711 + j1 * 13761));
                     blockpos$mutableblockpos.set(k1, j2, j1);
                     if (biome.warmEnoughToRain(blockpos$mutableblockpos)) {
                        if (i1 != 0) {
                           if (i1 >= 0) {
                              tesselator.end();
                           }

                           i1 = 0;
                           RenderSystem.setShaderTexture(0, RAIN_LOCATION);
                           bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        int i3 = this.ticks + k1 * k1 * 3121 + k1 * 45238971 + j1 * j1 * 418711 + j1 * 13761 & 31;
                        float f2 = -((float)i3 + pPartialTick) / 32.0F * (3.0F + random.nextFloat());
                        double d2 = (double)k1 + 0.5D - pCamX;
                        double d4 = (double)j1 + 0.5D - pCamZ;
                        float f3 = (float)Math.sqrt(d2 * d2 + d4 * d4) / (float)l;
                        float f4 = ((1.0F - f3 * f3) * 0.5F + 0.5F) * f;
                        blockpos$mutableblockpos.set(k1, l2, j1);
                        int j3 = getLightColor(level, blockpos$mutableblockpos);
                        bufferbuilder.vertex((double)k1 - pCamX - d0 + 0.5D, (double)k2 - pCamY, (double)j1 - pCamZ - d1 + 0.5D).uv(0.0F, (float)j2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                        bufferbuilder.vertex((double)k1 - pCamX + d0 + 0.5D, (double)k2 - pCamY, (double)j1 - pCamZ + d1 + 0.5D).uv(1.0F, (float)j2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                        bufferbuilder.vertex((double)k1 - pCamX + d0 + 0.5D, (double)j2 - pCamY, (double)j1 - pCamZ + d1 + 0.5D).uv(1.0F, (float)k2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                        bufferbuilder.vertex((double)k1 - pCamX - d0 + 0.5D, (double)j2 - pCamY, (double)j1 - pCamZ - d1 + 0.5D).uv(0.0F, (float)k2 * 0.25F + f2).color(1.0F, 1.0F, 1.0F, f4).uv2(j3).endVertex();
                     } else {
                        if (i1 != 1) {
                           if (i1 >= 0) {
                              tesselator.end();
                           }

                           i1 = 1;
                           RenderSystem.setShaderTexture(0, SNOW_LOCATION);
                           bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
                        }

                        float f5 = -((float)(this.ticks & 511) + pPartialTick) / 512.0F;
                        float f6 = (float)(random.nextDouble() + (double)f1 * 0.01D * (double)((float)random.nextGaussian()));
                        float f7 = (float)(random.nextDouble() + (double)(f1 * (float)random.nextGaussian()) * 0.001D);
                        double d3 = (double)k1 + 0.5D - pCamX;
                        double d5 = (double)j1 + 0.5D - pCamZ;
                        float f8 = (float)Math.sqrt(d3 * d3 + d5 * d5) / (float)l;
                        float f9 = ((1.0F - f8 * f8) * 0.3F + 0.5F) * f;
                        blockpos$mutableblockpos.set(k1, l2, j1);
                        int k3 = getLightColor(level, blockpos$mutableblockpos);
                        int l3 = k3 >> 16 & '\uffff';
                        int i4 = k3 & '\uffff';
                        int j4 = (l3 * 3 + 240) / 4;
                        int k4 = (i4 * 3 + 240) / 4;
                        bufferbuilder.vertex((double)k1 - pCamX - d0 + 0.5D, (double)k2 - pCamY, (double)j1 - pCamZ - d1 + 0.5D).uv(0.0F + f6, (float)j2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                        bufferbuilder.vertex((double)k1 - pCamX + d0 + 0.5D, (double)k2 - pCamY, (double)j1 - pCamZ + d1 + 0.5D).uv(1.0F + f6, (float)j2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                        bufferbuilder.vertex((double)k1 - pCamX + d0 + 0.5D, (double)j2 - pCamY, (double)j1 - pCamZ + d1 + 0.5D).uv(1.0F + f6, (float)k2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                        bufferbuilder.vertex((double)k1 - pCamX - d0 + 0.5D, (double)j2 - pCamY, (double)j1 - pCamZ - d1 + 0.5D).uv(0.0F + f6, (float)k2 * 0.25F + f5 + f7).color(1.0F, 1.0F, 1.0F, f9).uv2(k4, j4).endVertex();
                     }
                  }
               }
            }
         }

         if (i1 >= 0) {
            tesselator.end();
         }

         RenderSystem.enableCull();
         RenderSystem.disableBlend();
         pLightTexture.turnOffLightLayer();
      }
   }

   public void tickRain(Camera pCamera) {
      net.minecraftforge.client.IWeatherParticleRenderHandler renderHandler = level.effects().getWeatherParticleRenderHandler();
      if (renderHandler != null) {
         renderHandler.render(ticks, level, minecraft, pCamera);
         return;
      }
      float f = this.minecraft.level.getRainLevel(1.0F) / (Minecraft.useFancyGraphics() ? 1.0F : 2.0F);
      if (!(f <= 0.0F)) {
         Random random = new Random((long)this.ticks * 312987231L);
         LevelReader levelreader = this.minecraft.level;
         BlockPos blockpos = new BlockPos(pCamera.getPosition());
         BlockPos blockpos1 = null;
         int i = (int)(100.0F * f * f) / (this.minecraft.options.particles == ParticleStatus.DECREASED ? 2 : 1);

         for(int j = 0; j < i; ++j) {
            int k = random.nextInt(21) - 10;
            int l = random.nextInt(21) - 10;
            BlockPos blockpos2 = levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos.offset(k, 0, l));
            Biome biome = levelreader.getBiome(blockpos2);
            if (blockpos2.getY() > levelreader.getMinBuildHeight() && blockpos2.getY() <= blockpos.getY() + 10 && blockpos2.getY() >= blockpos.getY() - 10 && biome.getPrecipitation() == Biome.Precipitation.RAIN && biome.warmEnoughToRain(blockpos2)) {
               blockpos1 = blockpos2.below();
               if (this.minecraft.options.particles == ParticleStatus.MINIMAL) {
                  break;
               }

               double d0 = random.nextDouble();
               double d1 = random.nextDouble();
               BlockState blockstate = levelreader.getBlockState(blockpos1);
               FluidState fluidstate = levelreader.getFluidState(blockpos1);
               VoxelShape voxelshape = blockstate.getCollisionShape(levelreader, blockpos1);
               double d2 = voxelshape.max(Direction.Axis.Y, d0, d1);
               double d3 = (double)fluidstate.getHeight(levelreader, blockpos1);
               double d4 = Math.max(d2, d3);
               ParticleOptions particleoptions = !fluidstate.is(FluidTags.LAVA) && !blockstate.is(Blocks.MAGMA_BLOCK) && !CampfireBlock.isLitCampfire(blockstate) ? ParticleTypes.RAIN : ParticleTypes.SMOKE;
               this.minecraft.level.addParticle(particleoptions, (double)blockpos1.getX() + d0, (double)blockpos1.getY() + d4, (double)blockpos1.getZ() + d1, 0.0D, 0.0D, 0.0D);
            }
         }

         if (blockpos1 != null && random.nextInt(3) < this.rainSoundTime++) {
            this.rainSoundTime = 0;
            if (blockpos1.getY() > blockpos.getY() + 1 && levelreader.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, blockpos).getY() > Mth.floor((float)blockpos.getY())) {
               this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN_ABOVE, SoundSource.WEATHER, 0.1F, 0.5F, false);
            } else {
               this.minecraft.level.playLocalSound(blockpos1, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, 0.2F, 1.0F, false);
            }
         }

      }
   }

   public void close() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.close();
      }

   }

   public void onResourceManagerReload(ResourceManager pResourceManager) {
      this.initOutline();
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      }

   }

   public void initOutline() {
      if (this.entityEffect != null) {
         this.entityEffect.close();
      }

      ResourceLocation resourcelocation = new ResourceLocation("shaders/post/entity_outline.json");

      try {
         this.entityEffect = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation);
         this.entityEffect.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         this.entityTarget = this.entityEffect.getTempTarget("final");
      } catch (IOException ioexception) {
         LOGGER.warn("Failed to load shader: {}", resourcelocation, ioexception);
         this.entityEffect = null;
         this.entityTarget = null;
      } catch (JsonSyntaxException jsonsyntaxexception) {
         LOGGER.warn("Failed to parse shader: {}", resourcelocation, jsonsyntaxexception);
         this.entityEffect = null;
         this.entityTarget = null;
      }

   }

   private void initTransparency() {
      this.deinitTransparency();
      ResourceLocation resourcelocation = new ResourceLocation("shaders/post/transparency.json");

      try {
         PostChain postchain = new PostChain(this.minecraft.getTextureManager(), this.minecraft.getResourceManager(), this.minecraft.getMainRenderTarget(), resourcelocation);
         postchain.resize(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight());
         RenderTarget rendertarget1 = postchain.getTempTarget("translucent");
         RenderTarget rendertarget2 = postchain.getTempTarget("itemEntity");
         RenderTarget rendertarget3 = postchain.getTempTarget("particles");
         RenderTarget rendertarget4 = postchain.getTempTarget("weather");
         RenderTarget rendertarget = postchain.getTempTarget("clouds");
         this.transparencyChain = postchain;
         this.translucentTarget = rendertarget1;
         this.itemEntityTarget = rendertarget2;
         this.particlesTarget = rendertarget3;
         this.weatherTarget = rendertarget4;
         this.cloudsTarget = rendertarget;
      } catch (Exception exception) {
         String s = exception instanceof JsonSyntaxException ? "parse" : "load";
         String s1 = "Failed to " + s + " shader: " + resourcelocation;
         LevelRenderer.TransparencyShaderException levelrenderer$transparencyshaderexception = new LevelRenderer.TransparencyShaderException(s1, exception);
         if (this.minecraft.getResourcePackRepository().getSelectedIds().size() > 1) {
            Component component;
            try {
               component = new TextComponent(this.minecraft.getResourceManager().getResource(resourcelocation).getSourceName());
            } catch (IOException ioexception) {
               component = null;
            }

            this.minecraft.options.graphicsMode = GraphicsStatus.FANCY;
            this.minecraft.clearResourcePacksOnError(levelrenderer$transparencyshaderexception, component);
         } else {
            CrashReport crashreport = this.minecraft.fillReport(new CrashReport(s1, levelrenderer$transparencyshaderexception));
            this.minecraft.options.graphicsMode = GraphicsStatus.FANCY;
            this.minecraft.options.save();
            LOGGER.fatal(s1, (Throwable)levelrenderer$transparencyshaderexception);
            this.minecraft.emergencySave();
            Minecraft.crash(crashreport);
         }
      }

   }

   private void deinitTransparency() {
      if (this.transparencyChain != null) {
         this.transparencyChain.close();
         this.translucentTarget.destroyBuffers();
         this.itemEntityTarget.destroyBuffers();
         this.particlesTarget.destroyBuffers();
         this.weatherTarget.destroyBuffers();
         this.cloudsTarget.destroyBuffers();
         this.transparencyChain = null;
         this.translucentTarget = null;
         this.itemEntityTarget = null;
         this.particlesTarget = null;
         this.weatherTarget = null;
         this.cloudsTarget = null;
      }

   }

   public void doEntityOutline() {
      if (this.shouldShowEntityOutlines()) {
         RenderSystem.enableBlend();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE);
         this.entityTarget.blitToScreen(this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), false);
         RenderSystem.disableBlend();
      }

   }

   protected boolean shouldShowEntityOutlines() {
      return !this.minecraft.gameRenderer.isPanoramicMode() && this.entityTarget != null && this.entityEffect != null && this.minecraft.player != null;
   }

   private void createDarkSky() {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      if (this.darkBuffer != null) {
         this.darkBuffer.close();
      }

      this.darkBuffer = new VertexBuffer();
      buildSkyDisc(bufferbuilder, -16.0F);
      this.darkBuffer.upload(bufferbuilder);
   }

   private void createLightSky() {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      if (this.skyBuffer != null) {
         this.skyBuffer.close();
      }

      this.skyBuffer = new VertexBuffer();
      buildSkyDisc(bufferbuilder, 16.0F);
      this.skyBuffer.upload(bufferbuilder);
   }

   private static void buildSkyDisc(BufferBuilder pBuilder, float pY) {
      float f = Math.signum(pY) * 512.0F;
      float f1 = 512.0F;
      RenderSystem.setShader(GameRenderer::getPositionShader);
      pBuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
      pBuilder.vertex(0.0D, (double)pY, 0.0D).endVertex();

      for(int i = -180; i <= 180; i += 45) {
         pBuilder.vertex((double)(f * Mth.cos((float)i * ((float)Math.PI / 180F))), (double)pY, (double)(512.0F * Mth.sin((float)i * ((float)Math.PI / 180F)))).endVertex();
      }

      pBuilder.end();
   }

   private void createStars() {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionShader);
      if (this.starBuffer != null) {
         this.starBuffer.close();
      }

      this.starBuffer = new VertexBuffer();
      this.drawStars(bufferbuilder);
      bufferbuilder.end();
      this.starBuffer.upload(bufferbuilder);
   }

   private void drawStars(BufferBuilder pBuilder) {
      Random random = new Random(10842L);
      pBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

      for(int i = 0; i < 1500; ++i) {
         double d0 = (double)(random.nextFloat() * 2.0F - 1.0F);
         double d1 = (double)(random.nextFloat() * 2.0F - 1.0F);
         double d2 = (double)(random.nextFloat() * 2.0F - 1.0F);
         double d3 = (double)(0.15F + random.nextFloat() * 0.1F);
         double d4 = d0 * d0 + d1 * d1 + d2 * d2;
         if (d4 < 1.0D && d4 > 0.01D) {
            d4 = 1.0D / Math.sqrt(d4);
            d0 *= d4;
            d1 *= d4;
            d2 *= d4;
            double d5 = d0 * 100.0D;
            double d6 = d1 * 100.0D;
            double d7 = d2 * 100.0D;
            double d8 = Math.atan2(d0, d2);
            double d9 = Math.sin(d8);
            double d10 = Math.cos(d8);
            double d11 = Math.atan2(Math.sqrt(d0 * d0 + d2 * d2), d1);
            double d12 = Math.sin(d11);
            double d13 = Math.cos(d11);
            double d14 = random.nextDouble() * Math.PI * 2.0D;
            double d15 = Math.sin(d14);
            double d16 = Math.cos(d14);

            for(int j = 0; j < 4; ++j) {
               double d17 = 0.0D;
               double d18 = (double)((j & 2) - 1) * d3;
               double d19 = (double)((j + 1 & 2) - 1) * d3;
               double d20 = 0.0D;
               double d21 = d18 * d16 - d19 * d15;
               double d22 = d19 * d16 + d18 * d15;
               double d23 = d21 * d12 + 0.0D * d13;
               double d24 = 0.0D * d12 - d21 * d13;
               double d25 = d24 * d9 - d22 * d10;
               double d26 = d22 * d9 + d24 * d10;
               pBuilder.vertex(d5 + d25, d6 + d23, d7 + d26).endVertex();
            }
         }
      }

   }

   /**
    * 
    * @param pLevel the level to set, or {@code null} to clear
    */
   public void setLevel(@Nullable ClientLevel pLevel) {
      this.lastCameraX = Double.MIN_VALUE;
      this.lastCameraY = Double.MIN_VALUE;
      this.lastCameraZ = Double.MIN_VALUE;
      this.lastCameraChunkX = Integer.MIN_VALUE;
      this.lastCameraChunkY = Integer.MIN_VALUE;
      this.lastCameraChunkZ = Integer.MIN_VALUE;
      this.entityRenderDispatcher.setLevel(pLevel);
      this.level = pLevel;
      if (pLevel != null) {
         this.allChanged();
      } else {
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
            this.viewArea = null;
         }

         if (this.chunkRenderDispatcher != null) {
            this.chunkRenderDispatcher.dispose();
         }

         this.chunkRenderDispatcher = null;
         this.globalBlockEntities.clear();
         this.renderChunkStorage.set((LevelRenderer.RenderChunkStorage)null);
         this.renderChunksInFrustum.clear();
      }

   }

   public void graphicsChanged() {
      if (Minecraft.useShaderTransparency()) {
         this.initTransparency();
      } else {
         this.deinitTransparency();
      }

   }

   /**
    * Loads all renderers and sets up the basic options usage.
    */
   public void allChanged() {
      if (this.level != null) {
         this.graphicsChanged();
         this.level.clearTintCaches();
         if (this.chunkRenderDispatcher == null) {
            this.chunkRenderDispatcher = new ChunkRenderDispatcher(this.level, this, Util.backgroundExecutor(), this.minecraft.is64Bit(), this.renderBuffers.fixedBufferPack());
         } else {
            this.chunkRenderDispatcher.setLevel(this.level);
         }

         this.needsFullRenderChunkUpdate = true;
         this.generateClouds = true;
         this.recentlyCompiledChunks.clear();
         ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
         this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
         if (this.viewArea != null) {
            this.viewArea.releaseAllBuffers();
         }

         this.chunkRenderDispatcher.blockUntilClear();
         synchronized(this.globalBlockEntities) {
            this.globalBlockEntities.clear();
         }

         this.viewArea = new ViewArea(this.chunkRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
         if (this.lastFullRenderChunkUpdate != null) {
            try {
               this.lastFullRenderChunkUpdate.get();
               this.lastFullRenderChunkUpdate = null;
            } catch (Exception exception) {
               LOGGER.warn("Full update failed", (Throwable)exception);
            }
         }

         this.renderChunkStorage.set(new LevelRenderer.RenderChunkStorage(this.viewArea.chunks.length));
         this.renderChunksInFrustum.clear();
         Entity entity = this.minecraft.getCameraEntity();
         if (entity != null) {
            this.viewArea.repositionCamera(entity.getX(), entity.getZ());
         }

      }
   }

   public void resize(int pWidth, int pHeight) {
      this.needsUpdate();
      if (this.entityEffect != null) {
         this.entityEffect.resize(pWidth, pHeight);
      }

      if (this.transparencyChain != null) {
         this.transparencyChain.resize(pWidth, pHeight);
      }

   }

   /**
    * @return chunk rendering statistics to display on the {@linkplain
    * net.minecraft.client.gui.components.DebugScreenOverlay debug overlay}
    */
   public String getChunkStatistics() {
      int i = this.viewArea.chunks.length;
      int j = this.countRenderedChunks();
      return String.format("C: %d/%d %sD: %d, %s", j, i, this.minecraft.smartCull ? "(s) " : "", this.lastViewDistance, this.chunkRenderDispatcher == null ? "null" : this.chunkRenderDispatcher.getStats());
   }

   public ChunkRenderDispatcher getChunkRenderDispatcher() {
      return this.chunkRenderDispatcher;
   }

   public double getTotalChunks() {
      return (double)this.viewArea.chunks.length;
   }

   public double getLastViewDistance() {
      return (double)this.lastViewDistance;
   }

   public int countRenderedChunks() {
      int i = 0;

      for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
         if (!levelrenderer$renderchunkinfo.chunk.getCompiledChunk().hasNoRenderableLayers()) {
            ++i;
         }
      }

      return i;
   }

   /**
    * @return entity rendering statistics to display on the {@linkplain
    * net.minecraft.client.gui.components.DebugScreenOverlay debug overlay}
    */
   public String getEntityStatistics() {
      return "E: " + this.renderedEntities + "/" + this.level.getEntityCount() + ", B: " + this.culledEntities + ", SD: " + this.level.getServerSimulationDistance();
   }

   private void setupRender(Camera p_194339_, Frustum p_194340_, boolean p_194341_, boolean p_194342_) {
      Vec3 vec3 = p_194339_.getPosition();
      if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
         this.allChanged();
      }

      this.level.getProfiler().push("camera");
      double d0 = this.minecraft.player.getX();
      double d1 = this.minecraft.player.getY();
      double d2 = this.minecraft.player.getZ();
      double d3 = d0 - this.lastCameraX;
      double d4 = d1 - this.lastCameraY;
      double d5 = d2 - this.lastCameraZ;
      int i = SectionPos.posToSectionCoord(d0);
      int j = SectionPos.posToSectionCoord(d1);
      int k = SectionPos.posToSectionCoord(d2);
      if (this.lastCameraChunkX != i || this.lastCameraChunkY != j || this.lastCameraChunkZ != k || d3 * d3 + d4 * d4 + d5 * d5 > 16.0D) {
         this.lastCameraX = d0;
         this.lastCameraY = d1;
         this.lastCameraZ = d2;
         this.lastCameraChunkX = i;
         this.lastCameraChunkY = j;
         this.lastCameraChunkZ = k;
         this.viewArea.repositionCamera(d0, d2);
      }

      this.chunkRenderDispatcher.setCamera(vec3);
      this.level.getProfiler().popPush("cull");
      this.minecraft.getProfiler().popPush("culling");
      BlockPos blockpos = p_194339_.getBlockPosition();
      double d6 = Math.floor(vec3.x / 8.0D);
      double d7 = Math.floor(vec3.y / 8.0D);
      double d8 = Math.floor(vec3.z / 8.0D);
      this.needsFullRenderChunkUpdate = this.needsFullRenderChunkUpdate || d6 != this.prevCamX || d7 != this.prevCamY || d8 != this.prevCamZ;
      this.nextFullUpdateMillis.updateAndGet((p_194369_) -> {
         if (p_194369_ > 0L && System.currentTimeMillis() > p_194369_) {
            this.needsFullRenderChunkUpdate = true;
            return 0L;
         } else {
            return p_194369_;
         }
      });
      this.prevCamX = d6;
      this.prevCamY = d7;
      this.prevCamZ = d8;
      this.minecraft.getProfiler().popPush("update");
      boolean flag = this.minecraft.smartCull;
      if (p_194342_ && this.level.getBlockState(blockpos).isSolidRender(this.level, blockpos)) {
         flag = false;
      }

      if (!p_194341_) {
         if (this.needsFullRenderChunkUpdate && (this.lastFullRenderChunkUpdate == null || this.lastFullRenderChunkUpdate.isDone())) {
            this.minecraft.getProfiler().push("full_update_schedule");
            this.needsFullRenderChunkUpdate = false;
            boolean flag1 = flag;
            this.lastFullRenderChunkUpdate = Util.backgroundExecutor().submit(() -> {
               Queue<LevelRenderer.RenderChunkInfo> queue1 = Queues.newArrayDeque();
               this.initializeQueueForFullUpdate(p_194339_, queue1);
               LevelRenderer.RenderChunkStorage levelrenderer$renderchunkstorage1 = new LevelRenderer.RenderChunkStorage(this.viewArea.chunks.length);
               this.updateRenderChunks(levelrenderer$renderchunkstorage1.renderChunks, levelrenderer$renderchunkstorage1.renderInfoMap, vec3, queue1, flag1);
               this.renderChunkStorage.set(levelrenderer$renderchunkstorage1);
               this.needsFrustumUpdate.set(true);
            });
            this.minecraft.getProfiler().pop();
         }

         LevelRenderer.RenderChunkStorage levelrenderer$renderchunkstorage = this.renderChunkStorage.get();
         if (!this.recentlyCompiledChunks.isEmpty()) {
            this.minecraft.getProfiler().push("partial_update");
            Queue<LevelRenderer.RenderChunkInfo> queue = Queues.newArrayDeque();

            while(!this.recentlyCompiledChunks.isEmpty()) {
               ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = this.recentlyCompiledChunks.poll();
               LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo = levelrenderer$renderchunkstorage.renderInfoMap.get(chunkrenderdispatcher$renderchunk);
               if (levelrenderer$renderchunkinfo != null && levelrenderer$renderchunkinfo.chunk == chunkrenderdispatcher$renderchunk) {
                  queue.add(levelrenderer$renderchunkinfo);
               }
            }

            this.updateRenderChunks(levelrenderer$renderchunkstorage.renderChunks, levelrenderer$renderchunkstorage.renderInfoMap, vec3, queue, flag);
            this.needsFrustumUpdate.set(true);
            this.minecraft.getProfiler().pop();
         }

         double d9 = Math.floor((double)(p_194339_.getXRot() / 2.0F));
         double d10 = Math.floor((double)(p_194339_.getYRot() / 2.0F));
         if (this.needsFrustumUpdate.compareAndSet(true, false) || d9 != this.prevCamRotX || d10 != this.prevCamRotY) {
            this.applyFrustum((new Frustum(p_194340_)).offsetToFullyIncludeCameraCube(8));
            this.prevCamRotX = d9;
            this.prevCamRotY = d10;
         }
      }

      this.minecraft.getProfiler().pop();
   }

   private void applyFrustum(Frustum p_194355_) {
      this.minecraft.getProfiler().push("apply_frustum");
      this.renderChunksInFrustum.clear();

      for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : (this.renderChunkStorage.get()).renderChunks) {
         if (p_194355_.isVisible(levelrenderer$renderchunkinfo.chunk.bb)) {
            this.renderChunksInFrustum.add(levelrenderer$renderchunkinfo);
         }
      }

      this.minecraft.getProfiler().pop();
   }

   private void initializeQueueForFullUpdate(Camera p_194344_, Queue<LevelRenderer.RenderChunkInfo> p_194345_) {
      int i = 16;
      Vec3 vec3 = p_194344_.getPosition();
      BlockPos blockpos = p_194344_.getBlockPosition();
      ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = this.viewArea.getRenderChunkAt(blockpos);
      if (chunkrenderdispatcher$renderchunk == null) {
         boolean flag = blockpos.getY() > this.level.getMinBuildHeight();
         int j = flag ? this.level.getMaxBuildHeight() - 8 : this.level.getMinBuildHeight() + 8;
         int k = Mth.floor(vec3.x / 16.0D) * 16;
         int l = Mth.floor(vec3.z / 16.0D) * 16;
         List<LevelRenderer.RenderChunkInfo> list = Lists.newArrayList();

         for(int i1 = -this.lastViewDistance; i1 <= this.lastViewDistance; ++i1) {
            for(int j1 = -this.lastViewDistance; j1 <= this.lastViewDistance; ++j1) {
               ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk1 = this.viewArea.getRenderChunkAt(new BlockPos(k + SectionPos.sectionToBlockCoord(i1, 8), j, l + SectionPos.sectionToBlockCoord(j1, 8)));
               if (chunkrenderdispatcher$renderchunk1 != null) {
                  list.add(new LevelRenderer.RenderChunkInfo(chunkrenderdispatcher$renderchunk1, (Direction)null, 0));
               }
            }
         }

         list.sort(Comparator.comparingDouble((p_194358_) -> {
            return blockpos.distSqr(p_194358_.chunk.getOrigin().offset(8, 8, 8));
         }));
         p_194345_.addAll(list);
      } else {
         p_194345_.add(new LevelRenderer.RenderChunkInfo(chunkrenderdispatcher$renderchunk, (Direction)null, 0));
      }

   }

   public void addRecentlyCompiledChunk(ChunkRenderDispatcher.RenderChunk p_194353_) {
      this.recentlyCompiledChunks.add(p_194353_);
   }

   private void updateRenderChunks(LinkedHashSet<LevelRenderer.RenderChunkInfo> p_194363_, LevelRenderer.RenderInfoMap p_194364_, Vec3 p_194365_, Queue<LevelRenderer.RenderChunkInfo> p_194366_, boolean p_194367_) {
      int i = 16;
      BlockPos blockpos = new BlockPos(Mth.floor(p_194365_.x / 16.0D) * 16, Mth.floor(p_194365_.y / 16.0D) * 16, Mth.floor(p_194365_.z / 16.0D) * 16);
      BlockPos blockpos1 = blockpos.offset(8, 8, 8);
      Entity.setViewScale(Mth.clamp((double)this.minecraft.options.getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * (double)this.minecraft.options.entityDistanceScaling);

      while(!p_194366_.isEmpty()) {
         LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo = p_194366_.poll();
         ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo.chunk;
         p_194363_.add(levelrenderer$renderchunkinfo);
         Direction direction = Direction.getNearest((float)(chunkrenderdispatcher$renderchunk.getOrigin().getX() - blockpos.getX()), (float)(chunkrenderdispatcher$renderchunk.getOrigin().getY() - blockpos.getY()), (float)(chunkrenderdispatcher$renderchunk.getOrigin().getZ() - blockpos.getZ()));
         boolean flag = Math.abs(chunkrenderdispatcher$renderchunk.getOrigin().getX() - blockpos.getX()) > 60 || Math.abs(chunkrenderdispatcher$renderchunk.getOrigin().getY() - blockpos.getY()) > 60 || Math.abs(chunkrenderdispatcher$renderchunk.getOrigin().getZ() - blockpos.getZ()) > 60;

         for(Direction direction1 : DIRECTIONS) {
            ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk1 = this.getRelativeFrom(blockpos, chunkrenderdispatcher$renderchunk, direction1);
            if (chunkrenderdispatcher$renderchunk1 == null) {
               if (!this.closeToBorder(blockpos, chunkrenderdispatcher$renderchunk)) {
                  this.nextFullUpdateMillis.set(System.currentTimeMillis() + 500L);
               }
            } else if (!p_194367_ || !levelrenderer$renderchunkinfo.hasDirection(direction1.getOpposite())) {
               if (p_194367_ && levelrenderer$renderchunkinfo.hasSourceDirections()) {
                  ChunkRenderDispatcher.CompiledChunk chunkrenderdispatcher$compiledchunk = chunkrenderdispatcher$renderchunk.getCompiledChunk();
                  boolean flag1 = false;

                  for(int j = 0; j < DIRECTIONS.length; ++j) {
                     if (levelrenderer$renderchunkinfo.hasSourceDirection(j) && chunkrenderdispatcher$compiledchunk.facesCanSeeEachother(DIRECTIONS[j].getOpposite(), direction1)) {
                        flag1 = true;
                        break;
                     }
                  }

                  if (!flag1) {
                     continue;
                  }
               }

               if (p_194367_ && flag && levelrenderer$renderchunkinfo.hasSourceDirections() && !levelrenderer$renderchunkinfo.hasSourceDirection(direction.ordinal())) {
                  ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk3 = this.getRelativeFrom(blockpos, chunkrenderdispatcher$renderchunk, direction.getOpposite());
                  if (chunkrenderdispatcher$renderchunk3 == null) {
                     continue;
                  }

                  LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo2 = p_194364_.get(chunkrenderdispatcher$renderchunk3);
                  if (levelrenderer$renderchunkinfo2 == null) {
                     continue;
                  }
               }

               if (p_194367_ && flag) {
                  BlockPos blockpos2;
                  byte b0;
                  label140: {
                     label139: {
                        blockpos2 = chunkrenderdispatcher$renderchunk1.getOrigin();
                        if (direction1.getAxis() == Direction.Axis.X) {
                           if (blockpos1.getX() > blockpos2.getX()) {
                              break label139;
                           }
                        } else if (blockpos1.getX() < blockpos2.getX()) {
                           break label139;
                        }

                        b0 = 0;
                        break label140;
                     }

                     b0 = 16;
                  }

                  byte b1;
                  label132: {
                     label131: {
                        if (direction1.getAxis() == Direction.Axis.Y) {
                           if (blockpos1.getY() > blockpos2.getY()) {
                              break label131;
                           }
                        } else if (blockpos1.getY() < blockpos2.getY()) {
                           break label131;
                        }

                        b1 = 0;
                        break label132;
                     }

                     b1 = 16;
                  }

                  byte b2;
                  label124: {
                     label123: {
                        if (direction1.getAxis() == Direction.Axis.Z) {
                           if (blockpos1.getZ() > blockpos2.getZ()) {
                              break label123;
                           }
                        } else if (blockpos1.getZ() < blockpos2.getZ()) {
                           break label123;
                        }

                        b2 = 0;
                        break label124;
                     }

                     b2 = 16;
                  }

                  BlockPos blockpos3 = blockpos2.offset(b0, b1, b2);
                  Vec3 vec31 = new Vec3((double)blockpos3.getX(), (double)blockpos3.getY(), (double)blockpos3.getZ());
                  Vec3 vec3 = p_194365_.subtract(vec31).normalize().scale(CEILED_SECTION_DIAGONAL);
                  boolean flag2 = true;

                  while(p_194365_.subtract(vec31).lengthSqr() > 3600.0D) {
                     vec31 = vec31.add(vec3);
                     if (vec31.y > (double)this.level.getMaxBuildHeight() || vec31.y < (double)this.level.getMinBuildHeight()) {
                        break;
                     }

                     ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk2 = this.viewArea.getRenderChunkAt(new BlockPos(vec31.x, vec31.y, vec31.z));
                     if (chunkrenderdispatcher$renderchunk2 == null || p_194364_.get(chunkrenderdispatcher$renderchunk2) == null) {
                        flag2 = false;
                        break;
                     }
                  }

                  if (!flag2) {
                     continue;
                  }
               }

               LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo1 = p_194364_.get(chunkrenderdispatcher$renderchunk1);
               if (levelrenderer$renderchunkinfo1 != null) {
                  levelrenderer$renderchunkinfo1.addSourceDirection(direction1);
               } else if (!chunkrenderdispatcher$renderchunk1.hasAllNeighbors()) {
                  if (!this.closeToBorder(blockpos, chunkrenderdispatcher$renderchunk)) {
                     this.nextFullUpdateMillis.set(System.currentTimeMillis() + 500L);
                  }
               } else {
                  LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo3 = new LevelRenderer.RenderChunkInfo(chunkrenderdispatcher$renderchunk1, direction1, levelrenderer$renderchunkinfo.step + 1);
                  levelrenderer$renderchunkinfo3.setDirections(levelrenderer$renderchunkinfo.directions, direction1);
                  p_194366_.add(levelrenderer$renderchunkinfo3);
                  p_194364_.put(chunkrenderdispatcher$renderchunk1, levelrenderer$renderchunkinfo3);
               }
            }
         }
      }

   }

   /**
    * @return the specified {@code renderChunk} offset in the specified {@code direction}, or {@code null} if it can't
    * be seen by the camera at the specified {@code cameraChunkPos}
    * @param pCameraChunkPos the minimum block position of the chunk the camera is in
    */
   @Nullable
   private ChunkRenderDispatcher.RenderChunk getRelativeFrom(BlockPos pCameraChunkPos, ChunkRenderDispatcher.RenderChunk pRenderChunk, Direction pFacing) {
      BlockPos blockpos = pRenderChunk.getRelativeOrigin(pFacing);
      if (Mth.abs(pCameraChunkPos.getX() - blockpos.getX()) > this.lastViewDistance * 16) {
         return null;
      } else if (Mth.abs(pCameraChunkPos.getY() - blockpos.getY()) <= this.lastViewDistance * 16 && blockpos.getY() >= this.level.getMinBuildHeight() && blockpos.getY() < this.level.getMaxBuildHeight()) {
         return Mth.abs(pCameraChunkPos.getZ() - blockpos.getZ()) > this.lastViewDistance * 16 ? null : this.viewArea.getRenderChunkAt(blockpos);
      } else {
         return null;
      }
   }

   private boolean closeToBorder(BlockPos p_194360_, ChunkRenderDispatcher.RenderChunk p_194361_) {
      int i = SectionPos.blockToSectionCoord(p_194360_.getX());
      int j = SectionPos.blockToSectionCoord(p_194360_.getZ());
      BlockPos blockpos = p_194361_.getOrigin();
      int k = SectionPos.blockToSectionCoord(blockpos.getX());
      int l = SectionPos.blockToSectionCoord(blockpos.getZ());
      return !ChunkMap.isChunkInRange(k, l, i, j, this.lastViewDistance - 2);
   }

   private void captureFrustum(Matrix4f pViewMatrix, Matrix4f pProjectionMatrix, double pCamX, double pCamY, double pCamZ, Frustum pFrustum) {
      this.capturedFrustum = pFrustum;
      Matrix4f matrix4f = pProjectionMatrix.copy();
      matrix4f.multiply(pViewMatrix);
      matrix4f.invert();
      this.frustumPos.x = pCamX;
      this.frustumPos.y = pCamY;
      this.frustumPos.z = pCamZ;
      this.frustumPoints[0] = new Vector4f(-1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[1] = new Vector4f(1.0F, -1.0F, -1.0F, 1.0F);
      this.frustumPoints[2] = new Vector4f(1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[3] = new Vector4f(-1.0F, 1.0F, -1.0F, 1.0F);
      this.frustumPoints[4] = new Vector4f(-1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[5] = new Vector4f(1.0F, -1.0F, 1.0F, 1.0F);
      this.frustumPoints[6] = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
      this.frustumPoints[7] = new Vector4f(-1.0F, 1.0F, 1.0F, 1.0F);

      for(int i = 0; i < 8; ++i) {
         this.frustumPoints[i].transform(matrix4f);
         this.frustumPoints[i].perspectiveDivide();
      }

   }

   public void prepareCullFrustum(PoseStack pPoseStack, Vec3 pCameraPos, Matrix4f pProjectionMatrix) {
      Matrix4f matrix4f = pPoseStack.last().pose();
      double d0 = pCameraPos.x();
      double d1 = pCameraPos.y();
      double d2 = pCameraPos.z();
      this.cullingFrustum = new Frustum(matrix4f, pProjectionMatrix);
      this.cullingFrustum.prepare(d0, d1, d2);
   }

   public void renderLevel(PoseStack pPoseStack, float pPartialTick, long pFinishNanoTime, boolean pRenderBlockOutline, Camera pCamera, GameRenderer pGameRenderer, LightTexture pLightTexture, Matrix4f pProjectionMatrix) {
      RenderSystem.setShaderGameTime(this.level.getGameTime(), pPartialTick);
      this.blockEntityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.hitResult);
      this.entityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.crosshairPickEntity);
      ProfilerFiller profilerfiller = this.level.getProfiler();
      profilerfiller.popPush("light_update_queue");
      this.level.pollLightUpdates();
      profilerfiller.popPush("light_updates");
      boolean flag = this.level.isLightUpdateQueueEmpty();
      this.level.getChunkSource().getLightEngine().runUpdates(Integer.MAX_VALUE, flag, true);
      Vec3 vec3 = pCamera.getPosition();
      double d0 = vec3.x();
      double d1 = vec3.y();
      double d2 = vec3.z();
      Matrix4f matrix4f = pPoseStack.last().pose();
      profilerfiller.popPush("culling");
      boolean flag1 = this.capturedFrustum != null;
      Frustum frustum;
      if (flag1) {
         frustum = this.capturedFrustum;
         frustum.prepare(this.frustumPos.x, this.frustumPos.y, this.frustumPos.z);
      } else {
         frustum = this.cullingFrustum;
      }

      this.minecraft.getProfiler().popPush("captureFrustum");
      if (this.captureFrustum) {
         this.captureFrustum(matrix4f, pProjectionMatrix, vec3.x, vec3.y, vec3.z, flag1 ? new Frustum(matrix4f, pProjectionMatrix) : frustum);
         this.captureFrustum = false;
      }

      profilerfiller.popPush("clear");
      FogRenderer.setupColor(pCamera, pPartialTick, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), pGameRenderer.getDarkenWorldAmount(pPartialTick));
      FogRenderer.levelFogColor();
      RenderSystem.clear(16640, Minecraft.ON_OSX);
      float f = pGameRenderer.getRenderDistance();
      boolean flag2 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
      FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_SKY, f, flag2, pPartialTick);
      profilerfiller.popPush("sky");
      RenderSystem.setShader(GameRenderer::getPositionShader);
      this.renderSky(pPoseStack, pProjectionMatrix, pPartialTick, () -> {
         FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_SKY, f, flag2, pPartialTick);
      });
      profilerfiller.popPush("fog");
      FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_TERRAIN, Math.max(f, 32.0F), flag2, pPartialTick);
      profilerfiller.popPush("terrain_setup");
      this.setupRender(pCamera, frustum, flag1, this.minecraft.player.isSpectator());
      profilerfiller.popPush("compilechunks");
      this.compileChunks(pCamera);
      profilerfiller.popPush("terrain");
      this.renderChunkLayer(RenderType.solid(), pPoseStack, d0, d1, d2, pProjectionMatrix);
      this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).setBlurMipmap(false, this.minecraft.options.mipmapLevels > 0); // FORGE: fix flickering leaves when mods mess up the blurMipmap settings
      this.renderChunkLayer(RenderType.cutoutMipped(), pPoseStack, d0, d1, d2, pProjectionMatrix);
      this.minecraft.getModelManager().getAtlas(TextureAtlas.LOCATION_BLOCKS).restoreLastBlurMipmap();
      this.renderChunkLayer(RenderType.cutout(), pPoseStack, d0, d1, d2, pProjectionMatrix);
      if (this.level.effects().constantAmbientLight()) {
         Lighting.setupNetherLevel(pPoseStack.last().pose());
      } else {
         Lighting.setupLevel(pPoseStack.last().pose());
      }

      profilerfiller.popPush("entities");
      this.renderedEntities = 0;
      this.culledEntities = 0;
      if (this.itemEntityTarget != null) {
         this.itemEntityTarget.clear(Minecraft.ON_OSX);
         this.itemEntityTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      if (this.weatherTarget != null) {
         this.weatherTarget.clear(Minecraft.ON_OSX);
      }

      if (this.shouldShowEntityOutlines()) {
         this.entityTarget.clear(Minecraft.ON_OSX);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      boolean flag3 = false;
      MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();

      for(Entity entity : this.level.entitiesForRendering()) {
         if ((this.entityRenderDispatcher.shouldRender(entity, frustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) && (entity != pCamera.getEntity() || pCamera.isDetached() || pCamera.getEntity() instanceof LivingEntity && ((LivingEntity)pCamera.getEntity()).isSleeping()) && (!(entity instanceof LocalPlayer) || pCamera.getEntity() == entity || (entity == minecraft.player && !minecraft.player.isSpectator()))) { //FORGE: render local player entity when it is not the renderViewEntity
            ++this.renderedEntities;
            if (entity.tickCount == 0) {
               entity.xOld = entity.getX();
               entity.yOld = entity.getY();
               entity.zOld = entity.getZ();
            }

            MultiBufferSource multibuffersource;
            if (this.shouldShowEntityOutlines() && this.minecraft.shouldEntityAppearGlowing(entity)) {
               flag3 = true;
               OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
               multibuffersource = outlinebuffersource;
               int i = entity.getTeamColor();
               int j = 255;
               int k = i >> 16 & 255;
               int l = i >> 8 & 255;
               int i1 = i & 255;
               outlinebuffersource.setColor(k, l, i1, 255);
            } else {
               multibuffersource = multibuffersource$buffersource;
            }

            this.renderEntity(entity, d0, d1, d2, pPartialTick, pPoseStack, multibuffersource);
         }
      }

      multibuffersource$buffersource.endLastBatch();
      this.checkPoseStack(pPoseStack);
      multibuffersource$buffersource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
      multibuffersource$buffersource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
      multibuffersource$buffersource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
      multibuffersource$buffersource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
      profilerfiller.popPush("blockentities");

      for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
         List<BlockEntity> list = levelrenderer$renderchunkinfo.chunk.getCompiledChunk().getRenderableBlockEntities();
         if (!list.isEmpty()) {
            for(BlockEntity blockentity1 : list) {
               if(!frustum.isVisible(blockentity1.getRenderBoundingBox())) continue;
               BlockPos blockpos3 = blockentity1.getBlockPos();
               MultiBufferSource multibuffersource1 = multibuffersource$buffersource;
               pPoseStack.pushPose();
               pPoseStack.translate((double)blockpos3.getX() - d0, (double)blockpos3.getY() - d1, (double)blockpos3.getZ() - d2);
               SortedSet<BlockDestructionProgress> sortedset = this.destructionProgress.get(blockpos3.asLong());
               if (sortedset != null && !sortedset.isEmpty()) {
                  int j1 = sortedset.last().getProgress();
                  if (j1 >= 0) {
                     PoseStack.Pose posestack$pose = pPoseStack.last();
                     VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(j1)), posestack$pose.pose(), posestack$pose.normal());
                     multibuffersource1 = (p_194349_) -> {
                        VertexConsumer vertexconsumer3 = multibuffersource$buffersource.getBuffer(p_194349_);
                        return p_194349_.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer3) : vertexconsumer3;
                     };
                  }
               }

               this.blockEntityRenderDispatcher.render(blockentity1, pPartialTick, pPoseStack, multibuffersource1);
               pPoseStack.popPose();
            }
         }
      }

      synchronized(this.globalBlockEntities) {
         for(BlockEntity blockentity : this.globalBlockEntities) {
            if(!frustum.isVisible(blockentity.getRenderBoundingBox())) continue;
            BlockPos blockpos2 = blockentity.getBlockPos();
            pPoseStack.pushPose();
            pPoseStack.translate((double)blockpos2.getX() - d0, (double)blockpos2.getY() - d1, (double)blockpos2.getZ() - d2);
            this.blockEntityRenderDispatcher.render(blockentity, pPartialTick, pPoseStack, multibuffersource$buffersource);
            pPoseStack.popPose();
         }
      }

      this.checkPoseStack(pPoseStack);
      multibuffersource$buffersource.endBatch(RenderType.solid());
      multibuffersource$buffersource.endBatch(RenderType.endPortal());
      multibuffersource$buffersource.endBatch(RenderType.endGateway());
      multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
      multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
      multibuffersource$buffersource.endBatch(Sheets.bedSheet());
      multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
      multibuffersource$buffersource.endBatch(Sheets.signSheet());
      multibuffersource$buffersource.endBatch(Sheets.chestSheet());
      this.renderBuffers.outlineBufferSource().endOutlineBatch();
      if (flag3) {
         this.entityEffect.process(pPartialTick);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      }

      profilerfiller.popPush("destroyProgress");

      for(Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet()) {
         BlockPos blockpos1 = BlockPos.of(entry.getLongKey());
         double d3 = (double)blockpos1.getX() - d0;
         double d4 = (double)blockpos1.getY() - d1;
         double d5 = (double)blockpos1.getZ() - d2;
         if (!(d3 * d3 + d4 * d4 + d5 * d5 > 1024.0D)) {
            SortedSet<BlockDestructionProgress> sortedset1 = entry.getValue();
            if (sortedset1 != null && !sortedset1.isEmpty()) {
               int k1 = sortedset1.last().getProgress();
               pPoseStack.pushPose();
               pPoseStack.translate((double)blockpos1.getX() - d0, (double)blockpos1.getY() - d1, (double)blockpos1.getZ() - d2);
               PoseStack.Pose posestack$pose1 = pPoseStack.last();
               VertexConsumer vertexconsumer1 = new SheetedDecalTextureGenerator(this.renderBuffers.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(k1)), posestack$pose1.pose(), posestack$pose1.normal());
               this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(blockpos1), blockpos1, this.level, pPoseStack, vertexconsumer1);
               pPoseStack.popPose();
            }
         }
      }

      this.checkPoseStack(pPoseStack);
      HitResult hitresult = this.minecraft.hitResult;
      if (pRenderBlockOutline && hitresult != null && hitresult.getType() == HitResult.Type.BLOCK) {
         profilerfiller.popPush("outline");
         BlockPos blockpos = ((BlockHitResult)hitresult).getBlockPos();
         BlockState blockstate = this.level.getBlockState(blockpos);
         if (!net.minecraftforge.client.ForgeHooksClient.onDrawHighlight(this, pCamera, hitresult, pPartialTick, pPoseStack, multibuffersource$buffersource))
         if (!blockstate.isAir() && this.level.getWorldBorder().isWithinBounds(blockpos)) {
            VertexConsumer vertexconsumer2 = multibuffersource$buffersource.getBuffer(RenderType.lines());
            this.renderHitOutline(pPoseStack, vertexconsumer2, pCamera.getEntity(), d0, d1, d2, blockpos, blockstate);
         }
      } else if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
         net.minecraftforge.client.ForgeHooksClient.onDrawHighlight(this, pCamera, hitresult, pPartialTick, pPoseStack, multibuffersource$buffersource);
      }

      PoseStack posestack = RenderSystem.getModelViewStack();
      posestack.pushPose();
      posestack.mulPoseMatrix(pPoseStack.last().pose());
      RenderSystem.applyModelViewMatrix();
      this.minecraft.debugRenderer.render(pPoseStack, multibuffersource$buffersource, d0, d1, d2);
      posestack.popPose();
      RenderSystem.applyModelViewMatrix();
      multibuffersource$buffersource.endBatch(Sheets.translucentCullBlockSheet());
      multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
      multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
      multibuffersource$buffersource.endBatch(RenderType.armorGlint());
      multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
      multibuffersource$buffersource.endBatch(RenderType.glint());
      multibuffersource$buffersource.endBatch(RenderType.glintDirect());
      multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
      multibuffersource$buffersource.endBatch(RenderType.entityGlint());
      multibuffersource$buffersource.endBatch(RenderType.entityGlintDirect());
      multibuffersource$buffersource.endBatch(RenderType.waterMask());
      this.renderBuffers.crumblingBufferSource().endBatch();
      if (this.transparencyChain != null) {
         multibuffersource$buffersource.endBatch(RenderType.lines());
         multibuffersource$buffersource.endBatch();
         this.translucentTarget.clear(Minecraft.ON_OSX);
         this.translucentTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         profilerfiller.popPush("translucent");
         this.renderChunkLayer(RenderType.translucent(), pPoseStack, d0, d1, d2, pProjectionMatrix);
         profilerfiller.popPush("string");
         this.renderChunkLayer(RenderType.tripwire(), pPoseStack, d0, d1, d2, pProjectionMatrix);
         this.particlesTarget.clear(Minecraft.ON_OSX);
         this.particlesTarget.copyDepthFrom(this.minecraft.getMainRenderTarget());
         RenderStateShard.PARTICLES_TARGET.setupRenderState();
         profilerfiller.popPush("particles");
         this.minecraft.particleEngine.render(pPoseStack, multibuffersource$buffersource, pLightTexture, pCamera, pPartialTick, frustum);
         RenderStateShard.PARTICLES_TARGET.clearRenderState();
      } else {
         profilerfiller.popPush("translucent");
         if (this.translucentTarget != null) {
            this.translucentTarget.clear(Minecraft.ON_OSX);
         }

         this.renderChunkLayer(RenderType.translucent(), pPoseStack, d0, d1, d2, pProjectionMatrix);
         multibuffersource$buffersource.endBatch(RenderType.lines());
         multibuffersource$buffersource.endBatch();
         profilerfiller.popPush("string");
         this.renderChunkLayer(RenderType.tripwire(), pPoseStack, d0, d1, d2, pProjectionMatrix);
         profilerfiller.popPush("particles");
         this.minecraft.particleEngine.render(pPoseStack, multibuffersource$buffersource, pLightTexture, pCamera, pPartialTick, frustum);
      }

      posestack.pushPose();
      posestack.mulPoseMatrix(pPoseStack.last().pose());
      RenderSystem.applyModelViewMatrix();
      if (this.minecraft.options.getCloudsType() != CloudStatus.OFF) {
         if (this.transparencyChain != null) {
            this.cloudsTarget.clear(Minecraft.ON_OSX);
            RenderStateShard.CLOUDS_TARGET.setupRenderState();
            profilerfiller.popPush("clouds");
            this.renderClouds(pPoseStack, pProjectionMatrix, pPartialTick, d0, d1, d2);
            RenderStateShard.CLOUDS_TARGET.clearRenderState();
         } else {
            profilerfiller.popPush("clouds");
            RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
            this.renderClouds(pPoseStack, pProjectionMatrix, pPartialTick, d0, d1, d2);
         }
      }

      if (this.transparencyChain != null) {
         RenderStateShard.WEATHER_TARGET.setupRenderState();
         profilerfiller.popPush("weather");
         this.renderSnowAndRain(pLightTexture, pPartialTick, d0, d1, d2);
         this.renderWorldBorder(pCamera);
         RenderStateShard.WEATHER_TARGET.clearRenderState();
         this.transparencyChain.process(pPartialTick);
         this.minecraft.getMainRenderTarget().bindWrite(false);
      } else {
         RenderSystem.depthMask(false);
         profilerfiller.popPush("weather");
         this.renderSnowAndRain(pLightTexture, pPartialTick, d0, d1, d2);
         this.renderWorldBorder(pCamera);
         RenderSystem.depthMask(true);
      }

      this.renderDebug(pCamera);
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
      posestack.popPose();
      RenderSystem.applyModelViewMatrix();
      FogRenderer.setupNoFog();
   }

   /**
    * Asserts that the specified {@code poseStack} is {@linkplain com.mojang.blaze3d.vertex.PoseStack#clear() clear}.
    * @throws java.lang.IllegalStateException if the specified {@code poseStack} is not clear
    */
   private void checkPoseStack(PoseStack pPoseStack) {
      if (!pPoseStack.clear()) {
         throw new IllegalStateException("Pose stack not empty");
      }
   }

   private void renderEntity(Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource) {
      double d0 = Mth.lerp((double)pPartialTick, pEntity.xOld, pEntity.getX());
      double d1 = Mth.lerp((double)pPartialTick, pEntity.yOld, pEntity.getY());
      double d2 = Mth.lerp((double)pPartialTick, pEntity.zOld, pEntity.getZ());
      float f = Mth.lerp(pPartialTick, pEntity.yRotO, pEntity.getYRot());
      this.entityRenderDispatcher.render(pEntity, d0 - pCamX, d1 - pCamY, d2 - pCamZ, f, pPartialTick, pPoseStack, pBufferSource, this.entityRenderDispatcher.getPackedLightCoords(pEntity, pPartialTick));
   }

   private void renderChunkLayer(RenderType pRenderType, PoseStack pPoseStack, double pCamX, double pCamY, double pCamZ, Matrix4f pProjectionMatrix) {
      RenderSystem.assertOnRenderThread();
      pRenderType.setupRenderState();
      if (pRenderType == RenderType.translucent()) {
         this.minecraft.getProfiler().push("translucent_sort");
         double d0 = pCamX - this.xTransparentOld;
         double d1 = pCamY - this.yTransparentOld;
         double d2 = pCamZ - this.zTransparentOld;
         if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D) {
            this.xTransparentOld = pCamX;
            this.yTransparentOld = pCamY;
            this.zTransparentOld = pCamZ;
            int j = 0;

            for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
               if (j < 15 && levelrenderer$renderchunkinfo.chunk.resortTransparency(pRenderType, this.chunkRenderDispatcher)) {
                  ++j;
               }
            }
         }

         this.minecraft.getProfiler().pop();
      }

      this.minecraft.getProfiler().push("filterempty");
      this.minecraft.getProfiler().popPush(() -> {
         return "render_" + pRenderType;
      });
      boolean flag = pRenderType != RenderType.translucent();
      ObjectListIterator<LevelRenderer.RenderChunkInfo> objectlistiterator = this.renderChunksInFrustum.listIterator(flag ? 0 : this.renderChunksInFrustum.size());
      VertexFormat vertexformat = pRenderType.format();
      ShaderInstance shaderinstance = RenderSystem.getShader();
      BufferUploader.reset();

      for(int k = 0; k < 12; ++k) {
         int i = RenderSystem.getShaderTexture(k);
         shaderinstance.setSampler("Sampler" + k, i);
      }

      if (shaderinstance.MODEL_VIEW_MATRIX != null) {
         shaderinstance.MODEL_VIEW_MATRIX.set(pPoseStack.last().pose());
      }

      if (shaderinstance.PROJECTION_MATRIX != null) {
         shaderinstance.PROJECTION_MATRIX.set(pProjectionMatrix);
      }

      if (shaderinstance.COLOR_MODULATOR != null) {
         shaderinstance.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
      }

      if (shaderinstance.FOG_START != null) {
         shaderinstance.FOG_START.set(RenderSystem.getShaderFogStart());
      }

      if (shaderinstance.FOG_END != null) {
         shaderinstance.FOG_END.set(RenderSystem.getShaderFogEnd());
      }

      if (shaderinstance.FOG_COLOR != null) {
         shaderinstance.FOG_COLOR.set(RenderSystem.getShaderFogColor());
      }

      if (shaderinstance.TEXTURE_MATRIX != null) {
         shaderinstance.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
      }

      if (shaderinstance.GAME_TIME != null) {
         shaderinstance.GAME_TIME.set(RenderSystem.getShaderGameTime());
      }

      RenderSystem.setupShaderLights(shaderinstance);
      shaderinstance.apply();
      Uniform uniform = shaderinstance.CHUNK_OFFSET;
      boolean flag1 = false;

      while(true) {
         if (flag) {
            if (!objectlistiterator.hasNext()) {
               break;
            }
         } else if (!objectlistiterator.hasPrevious()) {
            break;
         }

         LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo1 = flag ? objectlistiterator.next() : objectlistiterator.previous();
         ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo1.chunk;
         if (!chunkrenderdispatcher$renderchunk.getCompiledChunk().isEmpty(pRenderType)) {
            VertexBuffer vertexbuffer = chunkrenderdispatcher$renderchunk.getBuffer(pRenderType);
            BlockPos blockpos = chunkrenderdispatcher$renderchunk.getOrigin();
            if (uniform != null) {
               uniform.set((float)((double)blockpos.getX() - pCamX), (float)((double)blockpos.getY() - pCamY), (float)((double)blockpos.getZ() - pCamZ));
               uniform.upload();
            }

            vertexbuffer.drawChunkLayer();
            flag1 = true;
         }
      }

      if (uniform != null) {
         uniform.set(Vector3f.ZERO);
      }

      shaderinstance.clear();
      if (flag1) {
         vertexformat.clearBufferState();
      }

      VertexBuffer.unbind();
      VertexBuffer.unbindVertexArray();
      this.minecraft.getProfiler().pop();
      pRenderType.clearRenderState();
   }

   private void renderDebug(Camera pCamera) {
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();
      RenderSystem.setShader(GameRenderer::getPositionColorShader);
      if (this.minecraft.chunkPath || this.minecraft.chunkVisibility) {
         double d0 = pCamera.getPosition().x();
         double d1 = pCamera.getPosition().y();
         double d2 = pCamera.getPosition().z();
         RenderSystem.depthMask(true);
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.disableTexture();

         for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
            ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo.chunk;
            BlockPos blockpos = chunkrenderdispatcher$renderchunk.getOrigin();
            PoseStack posestack = RenderSystem.getModelViewStack();
            posestack.pushPose();
            posestack.translate((double)blockpos.getX() - d0, (double)blockpos.getY() - d1, (double)blockpos.getZ() - d2);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            if (this.minecraft.chunkPath) {
               bufferbuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
               RenderSystem.lineWidth(5.0F);
               int i = levelrenderer$renderchunkinfo.step == 0 ? 0 : Mth.hsvToRgb((float)levelrenderer$renderchunkinfo.step / 50.0F, 0.9F, 0.9F);
               int j = i >> 16 & 255;
               int k = i >> 8 & 255;
               int l = i & 255;

               for(int i1 = 0; i1 < DIRECTIONS.length; ++i1) {
                  if (levelrenderer$renderchunkinfo.hasSourceDirection(i1)) {
                     Direction direction = DIRECTIONS[i1];
                     bufferbuilder.vertex(8.0D, 8.0D, 8.0D).color(j, k, l, 255).normal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ()).endVertex();
                     bufferbuilder.vertex((double)(8 - 16 * direction.getStepX()), (double)(8 - 16 * direction.getStepY()), (double)(8 - 16 * direction.getStepZ())).color(j, k, l, 255).normal((float)direction.getStepX(), (float)direction.getStepY(), (float)direction.getStepZ()).endVertex();
                  }
               }

               tesselator.end();
               RenderSystem.lineWidth(1.0F);
            }

            if (this.minecraft.chunkVisibility && !chunkrenderdispatcher$renderchunk.getCompiledChunk().hasNoRenderableLayers()) {
               bufferbuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
               RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
               RenderSystem.lineWidth(5.0F);
               int j1 = 0;

               for(Direction direction2 : DIRECTIONS) {
                  for(Direction direction1 : DIRECTIONS) {
                     boolean flag = chunkrenderdispatcher$renderchunk.getCompiledChunk().facesCanSeeEachother(direction2, direction1);
                     if (!flag) {
                        ++j1;
                        bufferbuilder.vertex((double)(8 + 8 * direction2.getStepX()), (double)(8 + 8 * direction2.getStepY()), (double)(8 + 8 * direction2.getStepZ())).color(255, 0, 0, 255).normal((float)direction2.getStepX(), (float)direction2.getStepY(), (float)direction2.getStepZ()).endVertex();
                        bufferbuilder.vertex((double)(8 + 8 * direction1.getStepX()), (double)(8 + 8 * direction1.getStepY()), (double)(8 + 8 * direction1.getStepZ())).color(255, 0, 0, 255).normal((float)direction1.getStepX(), (float)direction1.getStepY(), (float)direction1.getStepZ()).endVertex();
                     }
                  }
               }

               tesselator.end();
               RenderSystem.lineWidth(1.0F);
               RenderSystem.setShader(GameRenderer::getPositionColorShader);
               if (j1 > 0) {
                  bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                  float f = 0.5F;
                  float f1 = 0.2F;
                  bufferbuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 0.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 15.5D, 0.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 15.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(15.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  bufferbuilder.vertex(0.5D, 0.5D, 15.5D).color(0.9F, 0.9F, 0.0F, 0.2F).endVertex();
                  tesselator.end();
               }
            }

            posestack.popPose();
            RenderSystem.applyModelViewMatrix();
         }

         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.enableTexture();
      }

      if (this.capturedFrustum != null) {
         RenderSystem.disableCull();
         RenderSystem.disableTexture();
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.lineWidth(5.0F);
         RenderSystem.setShader(GameRenderer::getPositionColorShader);
         PoseStack posestack1 = RenderSystem.getModelViewStack();
         posestack1.pushPose();
         posestack1.translate((double)((float)(this.frustumPos.x - pCamera.getPosition().x)), (double)((float)(this.frustumPos.y - pCamera.getPosition().y)), (double)((float)(this.frustumPos.z - pCamera.getPosition().z)));
         RenderSystem.applyModelViewMatrix();
         RenderSystem.depthMask(true);
         bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
         this.addFrustumQuad(bufferbuilder, 0, 1, 2, 3, 0, 1, 1);
         this.addFrustumQuad(bufferbuilder, 4, 5, 6, 7, 1, 0, 0);
         this.addFrustumQuad(bufferbuilder, 0, 1, 5, 4, 1, 1, 0);
         this.addFrustumQuad(bufferbuilder, 2, 3, 7, 6, 0, 0, 1);
         this.addFrustumQuad(bufferbuilder, 0, 4, 7, 3, 0, 1, 0);
         this.addFrustumQuad(bufferbuilder, 1, 5, 6, 2, 1, 0, 1);
         tesselator.end();
         RenderSystem.depthMask(false);
         RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
         bufferbuilder.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         this.addFrustumVertex(bufferbuilder, 0);
         this.addFrustumVertex(bufferbuilder, 1);
         this.addFrustumVertex(bufferbuilder, 1);
         this.addFrustumVertex(bufferbuilder, 2);
         this.addFrustumVertex(bufferbuilder, 2);
         this.addFrustumVertex(bufferbuilder, 3);
         this.addFrustumVertex(bufferbuilder, 3);
         this.addFrustumVertex(bufferbuilder, 0);
         this.addFrustumVertex(bufferbuilder, 4);
         this.addFrustumVertex(bufferbuilder, 5);
         this.addFrustumVertex(bufferbuilder, 5);
         this.addFrustumVertex(bufferbuilder, 6);
         this.addFrustumVertex(bufferbuilder, 6);
         this.addFrustumVertex(bufferbuilder, 7);
         this.addFrustumVertex(bufferbuilder, 7);
         this.addFrustumVertex(bufferbuilder, 4);
         this.addFrustumVertex(bufferbuilder, 0);
         this.addFrustumVertex(bufferbuilder, 4);
         this.addFrustumVertex(bufferbuilder, 1);
         this.addFrustumVertex(bufferbuilder, 5);
         this.addFrustumVertex(bufferbuilder, 2);
         this.addFrustumVertex(bufferbuilder, 6);
         this.addFrustumVertex(bufferbuilder, 3);
         this.addFrustumVertex(bufferbuilder, 7);
         tesselator.end();
         posestack1.popPose();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.depthMask(true);
         RenderSystem.disableBlend();
         RenderSystem.enableCull();
         RenderSystem.enableTexture();
         RenderSystem.lineWidth(1.0F);
      }

   }

   private void addFrustumVertex(VertexConsumer pConsumer, int pVertex) {
      pConsumer.vertex((double)this.frustumPoints[pVertex].x(), (double)this.frustumPoints[pVertex].y(), (double)this.frustumPoints[pVertex].z()).color(0, 0, 0, 255).normal(0.0F, 0.0F, -1.0F).endVertex();
   }

   private void addFrustumQuad(VertexConsumer pConsumer, int pVertex1, int pVertex2, int pVertex3, int pVertex4, int pRed, int pGreen, int pBlue) {
      float f = 0.25F;
      pConsumer.vertex((double)this.frustumPoints[pVertex1].x(), (double)this.frustumPoints[pVertex1].y(), (double)this.frustumPoints[pVertex1].z()).color((float)pRed, (float)pGreen, (float)pBlue, 0.25F).endVertex();
      pConsumer.vertex((double)this.frustumPoints[pVertex2].x(), (double)this.frustumPoints[pVertex2].y(), (double)this.frustumPoints[pVertex2].z()).color((float)pRed, (float)pGreen, (float)pBlue, 0.25F).endVertex();
      pConsumer.vertex((double)this.frustumPoints[pVertex3].x(), (double)this.frustumPoints[pVertex3].y(), (double)this.frustumPoints[pVertex3].z()).color((float)pRed, (float)pGreen, (float)pBlue, 0.25F).endVertex();
      pConsumer.vertex((double)this.frustumPoints[pVertex4].x(), (double)this.frustumPoints[pVertex4].y(), (double)this.frustumPoints[pVertex4].z()).color((float)pRed, (float)pGreen, (float)pBlue, 0.25F).endVertex();
   }

   public void captureFrustum() {
      this.captureFrustum = true;
   }

   public void killFrustum() {
      this.capturedFrustum = null;
   }

   public void tick() {
      ++this.ticks;
      if (this.ticks % 20 == 0) {
         Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

         while(iterator.hasNext()) {
            BlockDestructionProgress blockdestructionprogress = iterator.next();
            int i = blockdestructionprogress.getUpdatedRenderTick();
            if (this.ticks - i > 400) {
               iterator.remove();
               this.removeProgress(blockdestructionprogress);
            }
         }

      }
   }

   private void removeProgress(BlockDestructionProgress pProgress) {
      long i = pProgress.getPos().asLong();
      Set<BlockDestructionProgress> set = this.destructionProgress.get(i);
      set.remove(pProgress);
      if (set.isEmpty()) {
         this.destructionProgress.remove(i);
      }

   }

   private void renderEndSky(PoseStack pPoseStack) {
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.depthMask(false);
      RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
      RenderSystem.setShaderTexture(0, END_SKY_LOCATION);
      Tesselator tesselator = Tesselator.getInstance();
      BufferBuilder bufferbuilder = tesselator.getBuilder();

      for(int i = 0; i < 6; ++i) {
         pPoseStack.pushPose();
         if (i == 1) {
            pPoseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
         }

         if (i == 2) {
            pPoseStack.mulPose(Vector3f.XP.rotationDegrees(-90.0F));
         }

         if (i == 3) {
            pPoseStack.mulPose(Vector3f.XP.rotationDegrees(180.0F));
         }

         if (i == 4) {
            pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
         }

         if (i == 5) {
            pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(-90.0F));
         }

         Matrix4f matrix4f = pPoseStack.last().pose();
         bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, -100.0F).uv(0.0F, 0.0F).color(40, 40, 40, 255).endVertex();
         bufferbuilder.vertex(matrix4f, -100.0F, -100.0F, 100.0F).uv(0.0F, 16.0F).color(40, 40, 40, 255).endVertex();
         bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, 100.0F).uv(16.0F, 16.0F).color(40, 40, 40, 255).endVertex();
         bufferbuilder.vertex(matrix4f, 100.0F, -100.0F, -100.0F).uv(16.0F, 0.0F).color(40, 40, 40, 255).endVertex();
         tesselator.end();
         pPoseStack.popPose();
      }

      RenderSystem.depthMask(true);
      RenderSystem.enableTexture();
      RenderSystem.disableBlend();
   }

   public void renderSky(PoseStack pPoseStack, Matrix4f pProjectionMatrix, float pPartialTick, Runnable pSkyFogSetup) {
      pSkyFogSetup.run();
      net.minecraftforge.client.ISkyRenderHandler renderHandler = level.effects().getSkyRenderHandler();
      if (renderHandler != null) {
         renderHandler.render(ticks, pPartialTick, pPoseStack, level, minecraft);
         return;
      }
      if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.END) {
         this.renderEndSky(pPoseStack);
      } else if (this.minecraft.level.effects().skyType() == DimensionSpecialEffects.SkyType.NORMAL) {
         RenderSystem.disableTexture();
         Vec3 vec3 = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), pPartialTick);
         float f = (float)vec3.x;
         float f1 = (float)vec3.y;
         float f2 = (float)vec3.z;
         FogRenderer.levelFogColor();
         BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
         RenderSystem.depthMask(false);
         RenderSystem.setShaderColor(f, f1, f2, 1.0F);
         ShaderInstance shaderinstance = RenderSystem.getShader();
         this.skyBuffer.drawWithShader(pPoseStack.last().pose(), pProjectionMatrix, shaderinstance);
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         float[] afloat = this.level.effects().getSunriseColor(this.level.getTimeOfDay(pPartialTick), pPartialTick);
         if (afloat != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableTexture();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            pPoseStack.pushPose();
            pPoseStack.mulPose(Vector3f.XP.rotationDegrees(90.0F));
            float f3 = Mth.sin(this.level.getSunAngle(pPartialTick)) < 0.0F ? 180.0F : 0.0F;
            pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(f3));
            pPoseStack.mulPose(Vector3f.ZP.rotationDegrees(90.0F));
            float f4 = afloat[0];
            float f5 = afloat[1];
            float f6 = afloat[2];
            Matrix4f matrix4f = pPoseStack.last().pose();
            bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bufferbuilder.vertex(matrix4f, 0.0F, 100.0F, 0.0F).color(f4, f5, f6, afloat[3]).endVertex();
            int i = 16;

            for(int j = 0; j <= 16; ++j) {
               float f7 = (float)j * ((float)Math.PI * 2F) / 16.0F;
               float f8 = Mth.sin(f7);
               float f9 = Mth.cos(f7);
               bufferbuilder.vertex(matrix4f, f8 * 120.0F, f9 * 120.0F, -f9 * 40.0F * afloat[3]).color(afloat[0], afloat[1], afloat[2], 0.0F).endVertex();
            }

            bufferbuilder.end();
            BufferUploader.end(bufferbuilder);
            pPoseStack.popPose();
         }

         RenderSystem.enableTexture();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
         pPoseStack.pushPose();
         float f11 = 1.0F - this.level.getRainLevel(pPartialTick);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, f11);
         pPoseStack.mulPose(Vector3f.YP.rotationDegrees(-90.0F));
         pPoseStack.mulPose(Vector3f.XP.rotationDegrees(this.level.getTimeOfDay(pPartialTick) * 360.0F));
         Matrix4f matrix4f1 = pPoseStack.last().pose();
         float f12 = 30.0F;
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, SUN_LOCATION);
         bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         bufferbuilder.vertex(matrix4f1, -f12, 100.0F, -f12).uv(0.0F, 0.0F).endVertex();
         bufferbuilder.vertex(matrix4f1, f12, 100.0F, -f12).uv(1.0F, 0.0F).endVertex();
         bufferbuilder.vertex(matrix4f1, f12, 100.0F, f12).uv(1.0F, 1.0F).endVertex();
         bufferbuilder.vertex(matrix4f1, -f12, 100.0F, f12).uv(0.0F, 1.0F).endVertex();
         bufferbuilder.end();
         BufferUploader.end(bufferbuilder);
         f12 = 20.0F;
         RenderSystem.setShaderTexture(0, MOON_LOCATION);
         int k = this.level.getMoonPhase();
         int l = k % 4;
         int i1 = k / 4 % 2;
         float f13 = (float)(l + 0) / 4.0F;
         float f14 = (float)(i1 + 0) / 2.0F;
         float f15 = (float)(l + 1) / 4.0F;
         float f16 = (float)(i1 + 1) / 2.0F;
         bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         bufferbuilder.vertex(matrix4f1, -f12, -100.0F, f12).uv(f15, f16).endVertex();
         bufferbuilder.vertex(matrix4f1, f12, -100.0F, f12).uv(f13, f16).endVertex();
         bufferbuilder.vertex(matrix4f1, f12, -100.0F, -f12).uv(f13, f14).endVertex();
         bufferbuilder.vertex(matrix4f1, -f12, -100.0F, -f12).uv(f15, f14).endVertex();
         bufferbuilder.end();
         BufferUploader.end(bufferbuilder);
         RenderSystem.disableTexture();
         float f10 = this.level.getStarBrightness(pPartialTick) * f11;
         if (f10 > 0.0F) {
            RenderSystem.setShaderColor(f10, f10, f10, f10);
            FogRenderer.setupNoFog();
            this.starBuffer.drawWithShader(pPoseStack.last().pose(), pProjectionMatrix, GameRenderer.getPositionShader());
            pSkyFogSetup.run();
         }

         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.disableBlend();
         pPoseStack.popPose();
         RenderSystem.disableTexture();
         RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
         double d0 = this.minecraft.player.getEyePosition(pPartialTick).y - this.level.getLevelData().getHorizonHeight(this.level);
         if (d0 < 0.0D) {
            pPoseStack.pushPose();
            pPoseStack.translate(0.0D, 12.0D, 0.0D);
            this.darkBuffer.drawWithShader(pPoseStack.last().pose(), pProjectionMatrix, shaderinstance);
            pPoseStack.popPose();
         }

         if (this.level.effects().hasGround()) {
            RenderSystem.setShaderColor(f * 0.2F + 0.04F, f1 * 0.2F + 0.04F, f2 * 0.6F + 0.1F, 1.0F);
         } else {
            RenderSystem.setShaderColor(f, f1, f2, 1.0F);
         }

         RenderSystem.enableTexture();
         RenderSystem.depthMask(true);
      }
   }

   public void renderClouds(PoseStack pPoseStack, Matrix4f pProjectionMatrix, float pPartialTick, double pCamX, double pCamY, double pCamZ) {
      net.minecraftforge.client.ICloudRenderHandler renderHandler = level.effects().getCloudRenderHandler();
      if (renderHandler != null) {
         renderHandler.render(ticks, pPartialTick, pPoseStack, level, minecraft, pCamX, pCamY, pCamZ);
         return;
      }
      float f = this.level.effects().getCloudHeight();
      if (!Float.isNaN(f)) {
         RenderSystem.disableCull();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
         RenderSystem.depthMask(true);
         float f1 = 12.0F;
         float f2 = 4.0F;
         double d0 = 2.0E-4D;
         double d1 = (double)(((float)this.ticks + pPartialTick) * 0.03F);
         double d2 = (pCamX + d1) / 12.0D;
         double d3 = (double)(f - (float)pCamY + 0.33F);
         double d4 = pCamZ / 12.0D + (double)0.33F;
         d2 -= (double)(Mth.floor(d2 / 2048.0D) * 2048);
         d4 -= (double)(Mth.floor(d4 / 2048.0D) * 2048);
         float f3 = (float)(d2 - (double)Mth.floor(d2));
         float f4 = (float)(d3 / 4.0D - (double)Mth.floor(d3 / 4.0D)) * 4.0F;
         float f5 = (float)(d4 - (double)Mth.floor(d4));
         Vec3 vec3 = this.level.getCloudColor(pPartialTick);
         int i = (int)Math.floor(d2);
         int j = (int)Math.floor(d3 / 4.0D);
         int k = (int)Math.floor(d4);
         if (i != this.prevCloudX || j != this.prevCloudY || k != this.prevCloudZ || this.minecraft.options.getCloudsType() != this.prevCloudsType || this.prevCloudColor.distanceToSqr(vec3) > 2.0E-4D) {
            this.prevCloudX = i;
            this.prevCloudY = j;
            this.prevCloudZ = k;
            this.prevCloudColor = vec3;
            this.prevCloudsType = this.minecraft.options.getCloudsType();
            this.generateClouds = true;
         }

         if (this.generateClouds) {
            this.generateClouds = false;
            BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
            if (this.cloudBuffer != null) {
               this.cloudBuffer.close();
            }

            this.cloudBuffer = new VertexBuffer();
            this.buildClouds(bufferbuilder, d2, d3, d4, vec3);
            bufferbuilder.end();
            this.cloudBuffer.upload(bufferbuilder);
         }

         RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
         RenderSystem.setShaderTexture(0, CLOUDS_LOCATION);
         FogRenderer.levelFogColor();
         pPoseStack.pushPose();
         pPoseStack.scale(12.0F, 1.0F, 12.0F);
         pPoseStack.translate((double)(-f3), (double)f4, (double)(-f5));
         if (this.cloudBuffer != null) {
            int i1 = this.prevCloudsType == CloudStatus.FANCY ? 0 : 1;

            for(int l = i1; l < 2; ++l) {
               if (l == 0) {
                  RenderSystem.colorMask(false, false, false, false);
               } else {
                  RenderSystem.colorMask(true, true, true, true);
               }

               ShaderInstance shaderinstance = RenderSystem.getShader();
               this.cloudBuffer.drawWithShader(pPoseStack.last().pose(), pProjectionMatrix, shaderinstance);
            }
         }

         pPoseStack.popPose();
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.enableCull();
         RenderSystem.disableBlend();
      }
   }

   private void buildClouds(BufferBuilder pBuilder, double pX, double pY, double pZ, Vec3 pCloudColor) {
      float f = 4.0F;
      float f1 = 0.00390625F;
      int i = 8;
      int j = 4;
      float f2 = 9.765625E-4F;
      float f3 = (float)Mth.floor(pX) * 0.00390625F;
      float f4 = (float)Mth.floor(pZ) * 0.00390625F;
      float f5 = (float)pCloudColor.x;
      float f6 = (float)pCloudColor.y;
      float f7 = (float)pCloudColor.z;
      float f8 = f5 * 0.9F;
      float f9 = f6 * 0.9F;
      float f10 = f7 * 0.9F;
      float f11 = f5 * 0.7F;
      float f12 = f6 * 0.7F;
      float f13 = f7 * 0.7F;
      float f14 = f5 * 0.8F;
      float f15 = f6 * 0.8F;
      float f16 = f7 * 0.8F;
      RenderSystem.setShader(GameRenderer::getPositionTexColorNormalShader);
      pBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
      float f17 = (float)Math.floor(pY / 4.0D) * 4.0F;
      if (this.prevCloudsType == CloudStatus.FANCY) {
         for(int k = -3; k <= 4; ++k) {
            for(int l = -3; l <= 4; ++l) {
               float f18 = (float)(k * 8);
               float f19 = (float)(l * 8);
               if (f17 > -5.0F) {
                  pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 8.0F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f11, f12, f13, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                  pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + 8.0F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f11, f12, f13, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                  pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + 0.0F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f11, f12, f13, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
                  pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 0.0F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f11, f12, f13, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               }

               if (f17 <= 5.0F) {
                  pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 8.0F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                  pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 8.0F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                  pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 0.0F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
                  pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F - 9.765625E-4F), (double)(f19 + 0.0F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, 1.0F, 0.0F).endVertex();
               }

               if (k > -1) {
                  for(int i1 = 0; i1 < 8; ++i1) {
                     pBuilder.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 8.0F)).uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                     pBuilder.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + 8.0F)).uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                     pBuilder.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + 0.0F)).uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                     pBuilder.vertex((double)(f18 + (float)i1 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + 0.0F)).uv((f18 + (float)i1 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(-1.0F, 0.0F, 0.0F).endVertex();
                  }
               }

               if (k <= 1) {
                  for(int j2 = 0; j2 < 8; ++j2) {
                     pBuilder.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 0.0F), (double)(f19 + 8.0F)).uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                     pBuilder.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 4.0F), (double)(f19 + 8.0F)).uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 8.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                     pBuilder.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 4.0F), (double)(f19 + 0.0F)).uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                     pBuilder.vertex((double)(f18 + (float)j2 + 1.0F - 9.765625E-4F), (double)(f17 + 0.0F), (double)(f19 + 0.0F)).uv((f18 + (float)j2 + 0.5F) * 0.00390625F + f3, (f19 + 0.0F) * 0.00390625F + f4).color(f8, f9, f10, 0.8F).normal(1.0F, 0.0F, 0.0F).endVertex();
                  }
               }

               if (l > -1) {
                  for(int k2 = 0; k2 < 8; ++k2) {
                     pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + (float)k2 + 0.0F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                     pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F), (double)(f19 + (float)k2 + 0.0F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                     pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + (float)k2 + 0.0F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                     pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + (float)k2 + 0.0F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)k2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, -1.0F).endVertex();
                  }
               }

               if (l <= 1) {
                  for(int l2 = 0; l2 < 8; ++l2) {
                     pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 4.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                     pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 4.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                     pBuilder.vertex((double)(f18 + 8.0F), (double)(f17 + 0.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F)).uv((f18 + 8.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                     pBuilder.vertex((double)(f18 + 0.0F), (double)(f17 + 0.0F), (double)(f19 + (float)l2 + 1.0F - 9.765625E-4F)).uv((f18 + 0.0F) * 0.00390625F + f3, (f19 + (float)l2 + 0.5F) * 0.00390625F + f4).color(f14, f15, f16, 0.8F).normal(0.0F, 0.0F, 1.0F).endVertex();
                  }
               }
            }
         }
      } else {
         int j1 = 1;
         int k1 = 32;

         for(int l1 = -32; l1 < 32; l1 += 32) {
            for(int i2 = -32; i2 < 32; i2 += 32) {
               pBuilder.vertex((double)(l1 + 0), (double)f17, (double)(i2 + 32)).uv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               pBuilder.vertex((double)(l1 + 32), (double)f17, (double)(i2 + 32)).uv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 32) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               pBuilder.vertex((double)(l1 + 32), (double)f17, (double)(i2 + 0)).uv((float)(l1 + 32) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
               pBuilder.vertex((double)(l1 + 0), (double)f17, (double)(i2 + 0)).uv((float)(l1 + 0) * 0.00390625F + f3, (float)(i2 + 0) * 0.00390625F + f4).color(f5, f6, f7, 0.8F).normal(0.0F, -1.0F, 0.0F).endVertex();
            }
         }
      }

   }

   private void compileChunks(Camera p_194371_) {
      this.minecraft.getProfiler().push("populate_chunks_to_compile");
      RenderRegionCache renderregioncache = new RenderRegionCache();
      BlockPos blockpos = p_194371_.getBlockPosition();
      List<ChunkRenderDispatcher.RenderChunk> list = Lists.newArrayList();

      for(LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo : this.renderChunksInFrustum) {
         ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk = levelrenderer$renderchunkinfo.chunk;
         ChunkPos chunkpos = new ChunkPos(chunkrenderdispatcher$renderchunk.getOrigin());
         if (chunkrenderdispatcher$renderchunk.isDirty() && this.level.getChunk(chunkpos.x, chunkpos.z).isClientLightReady()) {
            boolean flag = false;
            if (this.minecraft.options.prioritizeChunkUpdates != PrioritizeChunkUpdates.NEARBY) {
               if (this.minecraft.options.prioritizeChunkUpdates == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                  flag = chunkrenderdispatcher$renderchunk.isDirtyFromPlayer();
               }
            } else {
               BlockPos blockpos1 = chunkrenderdispatcher$renderchunk.getOrigin().offset(8, 8, 8);
               flag = !net.minecraftforge.common.ForgeConfig.CLIENT.alwaysSetupTerrainOffThread.get() && (blockpos1.distSqr(blockpos) < 768.0D || chunkrenderdispatcher$renderchunk.isDirtyFromPlayer()); // the target is the else block below, so invert the forge addition to get there early
            }

            if (flag) {
               this.minecraft.getProfiler().push("build_near_sync");
               this.chunkRenderDispatcher.rebuildChunkSync(chunkrenderdispatcher$renderchunk, renderregioncache);
               chunkrenderdispatcher$renderchunk.setNotDirty();
               this.minecraft.getProfiler().pop();
            } else {
               list.add(chunkrenderdispatcher$renderchunk);
            }
         }
      }

      this.minecraft.getProfiler().popPush("upload");
      this.chunkRenderDispatcher.uploadAllPendingUploads();
      this.minecraft.getProfiler().popPush("schedule_async_compile");

      for(ChunkRenderDispatcher.RenderChunk chunkrenderdispatcher$renderchunk1 : list) {
         chunkrenderdispatcher$renderchunk1.rebuildChunkAsync(this.chunkRenderDispatcher, renderregioncache);
         chunkrenderdispatcher$renderchunk1.setNotDirty();
      }

      this.minecraft.getProfiler().pop();
   }

   private void renderWorldBorder(Camera pCamera) {
      BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
      WorldBorder worldborder = this.level.getWorldBorder();
      double d0 = (double)(this.minecraft.options.getEffectiveRenderDistance() * 16);
      if (!(pCamera.getPosition().x < worldborder.getMaxX() - d0) || !(pCamera.getPosition().x > worldborder.getMinX() + d0) || !(pCamera.getPosition().z < worldborder.getMaxZ() - d0) || !(pCamera.getPosition().z > worldborder.getMinZ() + d0)) {
         double d1 = 1.0D - worldborder.getDistanceToBorder(pCamera.getPosition().x, pCamera.getPosition().z) / d0;
         d1 = Math.pow(d1, 4.0D);
         d1 = Mth.clamp(d1, 0.0D, 1.0D);
         double d2 = pCamera.getPosition().x;
         double d3 = pCamera.getPosition().z;
         double d4 = (double)this.minecraft.gameRenderer.getDepthFar();
         RenderSystem.enableBlend();
         RenderSystem.enableDepthTest();
         RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
         RenderSystem.setShaderTexture(0, FORCEFIELD_LOCATION);
         RenderSystem.depthMask(Minecraft.useShaderTransparency());
         PoseStack posestack = RenderSystem.getModelViewStack();
         posestack.pushPose();
         RenderSystem.applyModelViewMatrix();
         int i = worldborder.getStatus().getColor();
         float f = (float)(i >> 16 & 255) / 255.0F;
         float f1 = (float)(i >> 8 & 255) / 255.0F;
         float f2 = (float)(i & 255) / 255.0F;
         RenderSystem.setShaderColor(f, f1, f2, (float)d1);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.polygonOffset(-3.0F, -3.0F);
         RenderSystem.enablePolygonOffset();
         RenderSystem.disableCull();
         float f3 = (float)(Util.getMillis() % 3000L) / 3000.0F;
         float f4 = 0.0F;
         float f5 = 0.0F;
         float f6 = (float)(d4 - Mth.frac(pCamera.getPosition().y));
         bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
         double d5 = Math.max((double)Mth.floor(d3 - d0), worldborder.getMinZ());
         double d6 = Math.min((double)Mth.ceil(d3 + d0), worldborder.getMaxZ());
         if (d2 > worldborder.getMaxX() - d0) {
            float f7 = 0.0F;

            for(double d7 = d5; d7 < d6; f7 += 0.5F) {
               double d8 = Math.min(1.0D, d6 - d7);
               float f8 = (float)d8 * 0.5F;
               bufferbuilder.vertex(worldborder.getMaxX() - d2, -d4, d7 - d3).uv(f3 - f7, f3 + f6).endVertex();
               bufferbuilder.vertex(worldborder.getMaxX() - d2, -d4, d7 + d8 - d3).uv(f3 - (f8 + f7), f3 + f6).endVertex();
               bufferbuilder.vertex(worldborder.getMaxX() - d2, d4, d7 + d8 - d3).uv(f3 - (f8 + f7), f3 + 0.0F).endVertex();
               bufferbuilder.vertex(worldborder.getMaxX() - d2, d4, d7 - d3).uv(f3 - f7, f3 + 0.0F).endVertex();
               ++d7;
            }
         }

         if (d2 < worldborder.getMinX() + d0) {
            float f9 = 0.0F;

            for(double d9 = d5; d9 < d6; f9 += 0.5F) {
               double d12 = Math.min(1.0D, d6 - d9);
               float f12 = (float)d12 * 0.5F;
               bufferbuilder.vertex(worldborder.getMinX() - d2, -d4, d9 - d3).uv(f3 + f9, f3 + f6).endVertex();
               bufferbuilder.vertex(worldborder.getMinX() - d2, -d4, d9 + d12 - d3).uv(f3 + f12 + f9, f3 + f6).endVertex();
               bufferbuilder.vertex(worldborder.getMinX() - d2, d4, d9 + d12 - d3).uv(f3 + f12 + f9, f3 + 0.0F).endVertex();
               bufferbuilder.vertex(worldborder.getMinX() - d2, d4, d9 - d3).uv(f3 + f9, f3 + 0.0F).endVertex();
               ++d9;
            }
         }

         d5 = Math.max((double)Mth.floor(d2 - d0), worldborder.getMinX());
         d6 = Math.min((double)Mth.ceil(d2 + d0), worldborder.getMaxX());
         if (d3 > worldborder.getMaxZ() - d0) {
            float f10 = 0.0F;

            for(double d10 = d5; d10 < d6; f10 += 0.5F) {
               double d13 = Math.min(1.0D, d6 - d10);
               float f13 = (float)d13 * 0.5F;
               bufferbuilder.vertex(d10 - d2, -d4, worldborder.getMaxZ() - d3).uv(f3 + f10, f3 + f6).endVertex();
               bufferbuilder.vertex(d10 + d13 - d2, -d4, worldborder.getMaxZ() - d3).uv(f3 + f13 + f10, f3 + f6).endVertex();
               bufferbuilder.vertex(d10 + d13 - d2, d4, worldborder.getMaxZ() - d3).uv(f3 + f13 + f10, f3 + 0.0F).endVertex();
               bufferbuilder.vertex(d10 - d2, d4, worldborder.getMaxZ() - d3).uv(f3 + f10, f3 + 0.0F).endVertex();
               ++d10;
            }
         }

         if (d3 < worldborder.getMinZ() + d0) {
            float f11 = 0.0F;

            for(double d11 = d5; d11 < d6; f11 += 0.5F) {
               double d14 = Math.min(1.0D, d6 - d11);
               float f14 = (float)d14 * 0.5F;
               bufferbuilder.vertex(d11 - d2, -d4, worldborder.getMinZ() - d3).uv(f3 - f11, f3 + f6).endVertex();
               bufferbuilder.vertex(d11 + d14 - d2, -d4, worldborder.getMinZ() - d3).uv(f3 - (f14 + f11), f3 + f6).endVertex();
               bufferbuilder.vertex(d11 + d14 - d2, d4, worldborder.getMinZ() - d3).uv(f3 - (f14 + f11), f3 + 0.0F).endVertex();
               bufferbuilder.vertex(d11 - d2, d4, worldborder.getMinZ() - d3).uv(f3 - f11, f3 + 0.0F).endVertex();
               ++d11;
            }
         }

         bufferbuilder.end();
         BufferUploader.end(bufferbuilder);
         RenderSystem.enableCull();
         RenderSystem.polygonOffset(0.0F, 0.0F);
         RenderSystem.disablePolygonOffset();
         RenderSystem.disableBlend();
         posestack.popPose();
         RenderSystem.applyModelViewMatrix();
         RenderSystem.depthMask(true);
      }
   }

   private void renderHitOutline(PoseStack pPoseStack, VertexConsumer pConsumer, Entity pEntity, double pCamX, double pCamY, double pCamZ, BlockPos pPos, BlockState pState) {
      renderShape(pPoseStack, pConsumer, pState.getShape(this.level, pPos, CollisionContext.of(pEntity)), (double)pPos.getX() - pCamX, (double)pPos.getY() - pCamY, (double)pPos.getZ() - pCamZ, 0.0F, 0.0F, 0.0F, 0.4F);
   }

   public static void renderVoxelShape(PoseStack pPoseStack, VertexConsumer pConsumer, VoxelShape pShape, double pX, double pY, double pZ, float pRed, float pGreen, float pBlue, float pAlpha) {
      List<AABB> list = pShape.toAabbs();
      int i = Mth.ceil((double)list.size() / 3.0D);

      for(int j = 0; j < list.size(); ++j) {
         AABB aabb = list.get(j);
         float f = ((float)j % (float)i + 1.0F) / (float)i;
         float f1 = (float)(j / i);
         float f2 = f * (float)(f1 == 0.0F ? 1 : 0);
         float f3 = f * (float)(f1 == 1.0F ? 1 : 0);
         float f4 = f * (float)(f1 == 2.0F ? 1 : 0);
         renderShape(pPoseStack, pConsumer, Shapes.create(aabb.move(0.0D, 0.0D, 0.0D)), pX, pY, pZ, f2, f3, f4, 1.0F);
      }

   }

   private static void renderShape(PoseStack pPoseStack, VertexConsumer pConsumer, VoxelShape pShape, double pX, double pY, double pZ, float pRed, float pGreen, float pBlue, float pAlpha) {
      PoseStack.Pose posestack$pose = pPoseStack.last();
      pShape.forAllEdges((p_194324_, p_194325_, p_194326_, p_194327_, p_194328_, p_194329_) -> {
         float f = (float)(p_194327_ - p_194324_);
         float f1 = (float)(p_194328_ - p_194325_);
         float f2 = (float)(p_194329_ - p_194326_);
         float f3 = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
         f /= f3;
         f1 /= f3;
         f2 /= f3;
         pConsumer.vertex(posestack$pose.pose(), (float)(p_194324_ + pX), (float)(p_194325_ + pY), (float)(p_194326_ + pZ)).color(pRed, pGreen, pBlue, pAlpha).normal(posestack$pose.normal(), f, f1, f2).endVertex();
         pConsumer.vertex(posestack$pose.pose(), (float)(p_194327_ + pX), (float)(p_194328_ + pY), (float)(p_194329_ + pZ)).color(pRed, pGreen, pBlue, pAlpha).normal(posestack$pose.normal(), f, f1, f2).endVertex();
      });
   }

   public static void renderLineBox(VertexConsumer pConsumer, double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, float pRed, float pGreen, float pBlue, float pAlpha) {
      renderLineBox(new PoseStack(), pConsumer, pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ, pRed, pGreen, pBlue, pAlpha, pRed, pGreen, pBlue);
   }

   public static void renderLineBox(PoseStack pPoseStack, VertexConsumer pConsumer, AABB pBox, float pRed, float pGreen, float pBlue, float pAlpha) {
      renderLineBox(pPoseStack, pConsumer, pBox.minX, pBox.minY, pBox.minZ, pBox.maxX, pBox.maxY, pBox.maxZ, pRed, pGreen, pBlue, pAlpha, pRed, pGreen, pBlue);
   }

   public static void renderLineBox(PoseStack pPoseStack, VertexConsumer pConsumer, double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, float pRed, float pGreen, float pBlue, float pAlpha) {
      renderLineBox(pPoseStack, pConsumer, pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ, pRed, pGreen, pBlue, pAlpha, pRed, pGreen, pBlue);
   }

   public static void renderLineBox(PoseStack pPoseStack, VertexConsumer pConsumer, double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, float pRed, float pGreen, float pBlue, float pAlpha, float pRed2, float pGreen2, float pBlue2) {
      Matrix4f matrix4f = pPoseStack.last().pose();
      Matrix3f matrix3f = pPoseStack.last().normal();
      float f = (float)pMinX;
      float f1 = (float)pMinY;
      float f2 = (float)pMinZ;
      float f3 = (float)pMaxX;
      float f4 = (float)pMaxY;
      float f5 = (float)pMaxZ;
      pConsumer.vertex(matrix4f, f, f1, f2).color(pRed, pGreen2, pBlue2, pAlpha).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f1, f2).color(pRed, pGreen2, pBlue2, pAlpha).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f1, f2).color(pRed2, pGreen, pBlue2, pAlpha).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f4, f2).color(pRed2, pGreen, pBlue2, pAlpha).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f1, f2).color(pRed2, pGreen2, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f1, f5).color(pRed2, pGreen2, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f1, f2).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f4, f2).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f4, f2).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f4, f2).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, -1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f4, f2).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f4, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f4, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f1, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, -1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f1, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f1, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f1, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f1, f2).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, -1.0F).endVertex();
      pConsumer.vertex(matrix4f, f, f4, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f4, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 1.0F, 0.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f1, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f4, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 1.0F, 0.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f4, f2).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
      pConsumer.vertex(matrix4f, f3, f4, f5).color(pRed, pGreen, pBlue, pAlpha).normal(matrix3f, 0.0F, 0.0F, 1.0F).endVertex();
   }

   public static void addChainedFilledBoxVertices(BufferBuilder pBuilder, double pMinX, double pMinY, double pMinZ, double pMaxX, double pMaxY, double pMaxZ, float pRed, float pGreen, float pBlue, float pAlpha) {
      pBuilder.vertex(pMinX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMinY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMaxY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMinY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMinY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMinY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMaxY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMinY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMinY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMinY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMinY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMaxY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMaxY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMinX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMinZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
      pBuilder.vertex(pMaxX, pMaxY, pMaxZ).color(pRed, pGreen, pBlue, pAlpha).endVertex();
   }

   public void blockChanged(BlockGetter pLevel, BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
      this.setBlockDirty(pPos, (pFlags & 8) != 0);
   }

   private void setBlockDirty(BlockPos pPos, boolean pReRenderOnMainThread) {
      for(int i = pPos.getZ() - 1; i <= pPos.getZ() + 1; ++i) {
         for(int j = pPos.getX() - 1; j <= pPos.getX() + 1; ++j) {
            for(int k = pPos.getY() - 1; k <= pPos.getY() + 1; ++k) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), pReRenderOnMainThread);
            }
         }
      }

   }

   /**
    * Re-renders all blocks in the specified range.
    */
   public void setBlocksDirty(int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
      for(int i = pMinZ - 1; i <= pMaxZ + 1; ++i) {
         for(int j = pMinX - 1; j <= pMaxX + 1; ++j) {
            for(int k = pMinY - 1; k <= pMaxY + 1; ++k) {
               this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i));
            }
         }
      }

   }

   public void setBlockDirty(BlockPos pPos, BlockState pOldState, BlockState pNewState) {
      if (this.minecraft.getModelManager().requiresRender(pOldState, pNewState)) {
         this.setBlocksDirty(pPos.getX(), pPos.getY(), pPos.getZ(), pPos.getX(), pPos.getY(), pPos.getZ());
      }

   }

   public void setSectionDirtyWithNeighbors(int pSectionX, int pSectionY, int pSectionZ) {
      for(int i = pSectionZ - 1; i <= pSectionZ + 1; ++i) {
         for(int j = pSectionX - 1; j <= pSectionX + 1; ++j) {
            for(int k = pSectionY - 1; k <= pSectionY + 1; ++k) {
               this.setSectionDirty(j, k, i);
            }
         }
      }

   }

   public void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ) {
      this.setSectionDirty(pSectionX, pSectionY, pSectionZ, false);
   }

   private void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
      this.viewArea.setDirty(pSectionX, pSectionY, pSectionZ, pReRenderOnMainThread);
   }

   @Deprecated // Forge: use item aware function below
   public void playStreamingMusic(@Nullable SoundEvent pSoundEvent, BlockPos pPos) {
      this.playStreamingMusic(pSoundEvent, pPos, pSoundEvent == null? null : RecordItem.getBySound(pSoundEvent));
   }

   public void playStreamingMusic(@Nullable SoundEvent pSoundEvent, BlockPos pPos, @Nullable RecordItem musicDiscItem) {
      SoundInstance soundinstance = this.playingRecords.get(pPos);
      if (soundinstance != null) {
         this.minecraft.getSoundManager().stop(soundinstance);
         this.playingRecords.remove(pPos);
      }

      if (pSoundEvent != null) {
         RecordItem recorditem = musicDiscItem;
         if (recorditem != null) {
            this.minecraft.gui.setNowPlaying(recorditem.getDisplayName());
         }

         SoundInstance simplesoundinstance = SimpleSoundInstance.forRecord(pSoundEvent, (double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ());
         this.playingRecords.put(pPos, simplesoundinstance);
         this.minecraft.getSoundManager().play(simplesoundinstance);
      }

      this.notifyNearbyEntities(this.level, pPos, pSoundEvent != null);
   }

   /**
    * Notifies living entities in a 3 block range of the specified {@code pos} that a record is or isn't playing nearby,
    * dependent on the specified {@code playing} parameter.
    * This is used to make parrots start or stop partying.
    */
   private void notifyNearbyEntities(Level pLevel, BlockPos pPos, boolean pPlaying) {
      for(LivingEntity livingentity : pLevel.getEntitiesOfClass(LivingEntity.class, (new AABB(pPos)).inflate(3.0D))) {
         livingentity.setRecordPlayingNearby(pPos, pPlaying);
      }

   }

   /**
    * 
    * @param pForce if {@code true}, the particle will be created regardless of its distance from the camera and the
    * {@linkplain #calculateParticleLevel(boolean) calculated particle level}
    */
   public void addParticle(ParticleOptions pOptions, boolean pForce, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      this.addParticle(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
   }

   /**
    * 
    * @param pForce if {@code true}, the particle will be created regardless of its distance from the camera and the
    * {@linkplain #calculateParticleLevel(boolean) calculated particle level}
    * @param pDecreased if {@code true}, and the {@linkplain net.minecraft.client.Options#particles particles option} is
    * set to minimal, attempts to spawn the particle at a decreased level
    */
   public void addParticle(ParticleOptions pOptions, boolean pForce, boolean pDecreased, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      try {
         this.addParticleInternal(pOptions, pForce, pDecreased, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while adding particle");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
         crashreportcategory.setDetail("ID", Registry.PARTICLE_TYPE.getKey(pOptions.getType()));
         crashreportcategory.setDetail("Parameters", pOptions.writeToString());
         crashreportcategory.setDetail("Position", () -> {
            return CrashReportCategory.formatLocation(this.level, pX, pY, pZ);
         });
         throw new ReportedException(crashreport);
      }
   }

   private <T extends ParticleOptions> void addParticle(T pOptions, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      this.addParticle(pOptions, pOptions.getType().getOverrideLimiter(), pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
   }

   /**
    * 
    * @param pForce if {@code true}, the particle will be created regardless of its distance from the camera and the
    * {@linkplain #calculateParticleLevel(boolean) calculated particle level}
    */
   @Nullable
   private Particle addParticleInternal(ParticleOptions pOptions, boolean pForce, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      return this.addParticleInternal(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
   }

   /**
    * 
    * @param pForce if {@code true}, the particle will be created regardless of its distance from the camera and the
    * {@linkplain #calculateParticleLevel(boolean) calculated particle level}
    * @param pDecreased if {@code true}, and the {@linkplain net.minecraft.client.Options#particles particles option} is
    * set to minimal, attempts to spawn the particle at a decreased level
    */
   @Nullable
   private Particle addParticleInternal(ParticleOptions pOptions, boolean pForce, boolean pDecreased, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
      Camera camera = this.minecraft.gameRenderer.getMainCamera();
      if (this.minecraft != null && camera.isInitialized() && this.minecraft.particleEngine != null) {
         ParticleStatus particlestatus = this.calculateParticleLevel(pDecreased);
         if (pForce) {
            return this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         } else if (camera.getPosition().distanceToSqr(pX, pY, pZ) > 1024.0D) {
            return null;
         } else {
            return particlestatus == ParticleStatus.MINIMAL ? null : this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         }
      } else {
         return null;
      }
   }

   /**
    * Calculates the level of particles to use based on the {@linkplain net.minecraft.client.Options#particles particles
    * option} and the specified {@code decreased} parameter. This leads to randomly generating more or less particles
    * than the set option.
    * @param pDecreased if {@code true}, and the {@linkplain net.minecraft.client.Options#particles particles option} is
    * set to minimal, has a 1 in 10 chance to return a decreased level and a further 1 in 3 chance to minimise it
    */
   private ParticleStatus calculateParticleLevel(boolean pDecreased) {
      ParticleStatus particlestatus = this.minecraft.options.particles;
      if (pDecreased && particlestatus == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
         particlestatus = ParticleStatus.DECREASED;
      }

      if (particlestatus == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
         particlestatus = ParticleStatus.MINIMAL;
      }

      return particlestatus;
   }

   public void clear() {
   }

   /**
    * Handles a global level event. This includes playing sounds that should be heard by any player, regardless of
    * position and dimension, such as the Wither spawning.
    * @param pType the type of level event to handle. This method only handles {@linkplain
    * net.minecraft.world.level.block.LevelEvent#SOUND_WITHER_BOSS_SPAWN the wither boss spawn sound}, {@linkplain
    * net.minecraft.world.level.block.LevelEvent#SOUND_DRAGON_DEATH the dragon's death sound}, and {@linkplain
    * net.minecraft.world.level.block.LevelEvent#SOUND_END_PORTAL_SPAWN the end portal spawn sound}.
    */
   public void globalLevelEvent(int pType, BlockPos pPos, int pData) {
      switch(pType) {
      case 1023:
      case 1028:
      case 1038:
         Camera camera = this.minecraft.gameRenderer.getMainCamera();
         if (camera.isInitialized()) {
            double d0 = (double)pPos.getX() - camera.getPosition().x;
            double d1 = (double)pPos.getY() - camera.getPosition().y;
            double d2 = (double)pPos.getZ() - camera.getPosition().z;
            double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
            double d4 = camera.getPosition().x;
            double d5 = camera.getPosition().y;
            double d6 = camera.getPosition().z;
            if (d3 > 0.0D) {
               d4 += d0 / d3 * 2.0D;
               d5 += d1 / d3 * 2.0D;
               d6 += d2 / d3 * 2.0D;
            }

            if (pType == 1023) {
               this.level.playLocalSound(d4, d5, d6, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
            } else if (pType == 1038) {
               this.level.playLocalSound(d4, d5, d6, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.0F, 1.0F, false);
            } else {
               this.level.playLocalSound(d4, d5, d6, SoundEvents.ENDER_DRAGON_DEATH, SoundSource.HOSTILE, 5.0F, 1.0F, false);
            }
         }
      default:
      }
   }

   public void levelEvent(Player pPlayer, int pType, BlockPos pPos, int pData) {
      Random random = this.level.random;
      switch(pType) {
      case 1000:
         this.level.playLocalSound(pPos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1001:
         this.level.playLocalSound(pPos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 1.0F, 1.2F, false);
         break;
      case 1002:
         this.level.playLocalSound(pPos, SoundEvents.DISPENSER_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.2F, false);
         break;
      case 1003:
         this.level.playLocalSound(pPos, SoundEvents.ENDER_EYE_LAUNCH, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
         break;
      case 1004:
         this.level.playLocalSound(pPos, SoundEvents.FIREWORK_ROCKET_SHOOT, SoundSource.NEUTRAL, 1.0F, 1.2F, false);
         break;
      case 1005:
         this.level.playLocalSound(pPos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1006:
         this.level.playLocalSound(pPos, SoundEvents.WOODEN_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1007:
         this.level.playLocalSound(pPos, SoundEvents.WOODEN_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1008:
         this.level.playLocalSound(pPos, SoundEvents.FENCE_GATE_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1009:
         if (pData == 0) {
            this.level.playLocalSound(pPos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);
         } else if (pData == 1) {
            this.level.playLocalSound(pPos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 0.7F, 1.6F + (random.nextFloat() - random.nextFloat()) * 0.4F, false);
         }
         break;
      case 1010:
         if (Item.byId(pData) instanceof RecordItem) {
            this.playStreamingMusic(((RecordItem)Item.byId(pData)).getSound(), pPos, (RecordItem) Item.byId(pData));
         } else {
            this.playStreamingMusic((SoundEvent)null, pPos);
         }
         break;
      case 1011:
         this.level.playLocalSound(pPos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1012:
         this.level.playLocalSound(pPos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1013:
         this.level.playLocalSound(pPos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1014:
         this.level.playLocalSound(pPos, SoundEvents.FENCE_GATE_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1015:
         this.level.playLocalSound(pPos, SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1016:
         this.level.playLocalSound(pPos, SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1017:
         this.level.playLocalSound(pPos, SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 10.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1018:
         this.level.playLocalSound(pPos, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1019:
         this.level.playLocalSound(pPos, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1020:
         this.level.playLocalSound(pPos, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1021:
         this.level.playLocalSound(pPos, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1022:
         this.level.playLocalSound(pPos, SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1024:
         this.level.playLocalSound(pPos, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1025:
         this.level.playLocalSound(pPos, SoundEvents.BAT_TAKEOFF, SoundSource.NEUTRAL, 0.05F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1026:
         this.level.playLocalSound(pPos, SoundEvents.ZOMBIE_INFECT, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1027:
         this.level.playLocalSound(pPos, SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1029:
         this.level.playLocalSound(pPos, SoundEvents.ANVIL_DESTROY, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1030:
         this.level.playLocalSound(pPos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1031:
         this.level.playLocalSound(pPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1032:
         this.minecraft.getSoundManager().play(SimpleSoundInstance.forLocalAmbience(SoundEvents.PORTAL_TRAVEL, random.nextFloat() * 0.4F + 0.8F, 0.25F));
         break;
      case 1033:
         this.level.playLocalSound(pPos, SoundEvents.CHORUS_FLOWER_GROW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1034:
         this.level.playLocalSound(pPos, SoundEvents.CHORUS_FLOWER_DEATH, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1035:
         this.level.playLocalSound(pPos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 1036:
         this.level.playLocalSound(pPos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1037:
         this.level.playLocalSound(pPos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1039:
         this.level.playLocalSound(pPos, SoundEvents.PHANTOM_BITE, SoundSource.HOSTILE, 0.3F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1040:
         this.level.playLocalSound(pPos, SoundEvents.ZOMBIE_CONVERTED_TO_DROWNED, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1041:
         this.level.playLocalSound(pPos, SoundEvents.HUSK_CONVERTED_TO_ZOMBIE, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1042:
         this.level.playLocalSound(pPos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1043:
         this.level.playLocalSound(pPos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1044:
         this.level.playLocalSound(pPos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1045:
         this.level.playLocalSound(pPos, SoundEvents.POINTED_DRIPSTONE_LAND, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1046:
         this.level.playLocalSound(pPos, SoundEvents.POINTED_DRIPSTONE_DRIP_LAVA_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1047:
         this.level.playLocalSound(pPos, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER_INTO_CAULDRON, SoundSource.BLOCKS, 2.0F, this.level.random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 1048:
         this.level.playLocalSound(pPos, SoundEvents.SKELETON_CONVERTED_TO_STRAY, SoundSource.HOSTILE, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F, false);
         break;
      case 1500:
         ComposterBlock.handleFill(this.level, pPos, pData > 0);
         break;
      case 1501:
         this.level.playLocalSound(pPos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);

         for(int i2 = 0; i2 < 8; ++i2) {
            this.level.addParticle(ParticleTypes.LARGE_SMOKE, (double)pPos.getX() + random.nextDouble(), (double)pPos.getY() + 1.2D, (double)pPos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
         }
         break;
      case 1502:
         this.level.playLocalSound(pPos, SoundEvents.REDSTONE_TORCH_BURNOUT, SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F, false);

         for(int l1 = 0; l1 < 5; ++l1) {
            double d15 = (double)pPos.getX() + random.nextDouble() * 0.6D + 0.2D;
            double d20 = (double)pPos.getY() + random.nextDouble() * 0.6D + 0.2D;
            double d26 = (double)pPos.getZ() + random.nextDouble() * 0.6D + 0.2D;
            this.level.addParticle(ParticleTypes.SMOKE, d15, d20, d26, 0.0D, 0.0D, 0.0D);
         }
         break;
      case 1503:
         this.level.playLocalSound(pPos, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F, false);

         for(int k1 = 0; k1 < 16; ++k1) {
            double d14 = (double)pPos.getX() + (5.0D + random.nextDouble() * 6.0D) / 16.0D;
            double d19 = (double)pPos.getY() + 0.8125D;
            double d25 = (double)pPos.getZ() + (5.0D + random.nextDouble() * 6.0D) / 16.0D;
            this.level.addParticle(ParticleTypes.SMOKE, d14, d19, d25, 0.0D, 0.0D, 0.0D);
         }
         break;
      case 1504:
         PointedDripstoneBlock.spawnDripParticle(this.level, pPos, this.level.getBlockState(pPos));
         break;
      case 1505:
         BoneMealItem.addGrowthParticles(this.level, pPos, pData);
         this.level.playLocalSound(pPos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 2000:
         Direction direction = Direction.from3DDataValue(pData);
         int j1 = direction.getStepX();
         int j2 = direction.getStepY();
         int k2 = direction.getStepZ();
         double d18 = (double)pPos.getX() + (double)j1 * 0.6D + 0.5D;
         double d24 = (double)pPos.getY() + (double)j2 * 0.6D + 0.5D;
         double d28 = (double)pPos.getZ() + (double)k2 * 0.6D + 0.5D;

         for(int i3 = 0; i3 < 10; ++i3) {
            double d4 = random.nextDouble() * 0.2D + 0.01D;
            double d6 = d18 + (double)j1 * 0.01D + (random.nextDouble() - 0.5D) * (double)k2 * 0.5D;
            double d8 = d24 + (double)j2 * 0.01D + (random.nextDouble() - 0.5D) * (double)j2 * 0.5D;
            double d30 = d28 + (double)k2 * 0.01D + (random.nextDouble() - 0.5D) * (double)j1 * 0.5D;
            double d9 = (double)j1 * d4 + random.nextGaussian() * 0.01D;
            double d10 = (double)j2 * d4 + random.nextGaussian() * 0.01D;
            double d11 = (double)k2 * d4 + random.nextGaussian() * 0.01D;
            this.addParticle(ParticleTypes.SMOKE, d6, d8, d30, d9, d10, d11);
         }
         break;
      case 2001:
         BlockState blockstate = Block.stateById(pData);
         if (!blockstate.isAir()) {
            SoundType soundtype = blockstate.getSoundType(this.level, pPos, null);
            this.level.playLocalSound(pPos, soundtype.getBreakSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F, false);
         }

         this.level.addDestroyBlockEffect(pPos, blockstate);
         break;
      case 2002:
      case 2007:
         Vec3 vec3 = Vec3.atBottomCenterOf(pPos);

         for(int i1 = 0; i1 < 8; ++i1) {
            this.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SPLASH_POTION)), vec3.x, vec3.y, vec3.z, random.nextGaussian() * 0.15D, random.nextDouble() * 0.2D, random.nextGaussian() * 0.15D);
         }

         float f3 = (float)(pData >> 16 & 255) / 255.0F;
         float f4 = (float)(pData >> 8 & 255) / 255.0F;
         float f5 = (float)(pData >> 0 & 255) / 255.0F;
         ParticleOptions particleoptions = pType == 2007 ? ParticleTypes.INSTANT_EFFECT : ParticleTypes.EFFECT;

         for(int j = 0; j < 100; ++j) {
            double d23 = random.nextDouble() * 4.0D;
            double d27 = random.nextDouble() * Math.PI * 2.0D;
            double d29 = Math.cos(d27) * d23;
            double d5 = 0.01D + random.nextDouble() * 0.5D;
            double d7 = Math.sin(d27) * d23;
            Particle particle1 = this.addParticleInternal(particleoptions, particleoptions.getType().getOverrideLimiter(), vec3.x + d29 * 0.1D, vec3.y + 0.3D, vec3.z + d7 * 0.1D, d29, d5, d7);
            if (particle1 != null) {
               float f2 = 0.75F + random.nextFloat() * 0.25F;
               particle1.setColor(f3 * f2, f4 * f2, f5 * f2);
               particle1.setPower((float)d23);
            }
         }

         this.level.playLocalSound(pPos, SoundEvents.SPLASH_POTION_BREAK, SoundSource.NEUTRAL, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         break;
      case 2003:
         double d0 = (double)pPos.getX() + 0.5D;
         double d13 = (double)pPos.getY();
         double d17 = (double)pPos.getZ() + 0.5D;

         for(int l2 = 0; l2 < 8; ++l2) {
            this.addParticle(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ENDER_EYE)), d0, d13, d17, random.nextGaussian() * 0.15D, random.nextDouble() * 0.2D, random.nextGaussian() * 0.15D);
         }

         for(double d22 = 0.0D; d22 < (Math.PI * 2D); d22 += 0.15707963267948966D) {
            this.addParticle(ParticleTypes.PORTAL, d0 + Math.cos(d22) * 5.0D, d13 - 0.4D, d17 + Math.sin(d22) * 5.0D, Math.cos(d22) * -5.0D, 0.0D, Math.sin(d22) * -5.0D);
            this.addParticle(ParticleTypes.PORTAL, d0 + Math.cos(d22) * 5.0D, d13 - 0.4D, d17 + Math.sin(d22) * 5.0D, Math.cos(d22) * -7.0D, 0.0D, Math.sin(d22) * -7.0D);
         }
         break;
      case 2004:
         for(int l = 0; l < 20; ++l) {
            double d12 = (double)pPos.getX() + 0.5D + (random.nextDouble() - 0.5D) * 2.0D;
            double d16 = (double)pPos.getY() + 0.5D + (random.nextDouble() - 0.5D) * 2.0D;
            double d21 = (double)pPos.getZ() + 0.5D + (random.nextDouble() - 0.5D) * 2.0D;
            this.level.addParticle(ParticleTypes.SMOKE, d12, d16, d21, 0.0D, 0.0D, 0.0D);
            this.level.addParticle(ParticleTypes.FLAME, d12, d16, d21, 0.0D, 0.0D, 0.0D);
         }
         break;
      case 2005:
         BoneMealItem.addGrowthParticles(this.level, pPos, pData);
         break;
      case 2006:
         for(int k = 0; k < 200; ++k) {
            float f = random.nextFloat() * 4.0F;
            float f1 = random.nextFloat() * ((float)Math.PI * 2F);
            double d1 = (double)(Mth.cos(f1) * f);
            double d2 = 0.01D + random.nextDouble() * 0.5D;
            double d3 = (double)(Mth.sin(f1) * f);
            Particle particle = this.addParticleInternal(ParticleTypes.DRAGON_BREATH, false, (double)pPos.getX() + d1 * 0.1D, (double)pPos.getY() + 0.3D, (double)pPos.getZ() + d3 * 0.1D, d1, d2, d3);
            if (particle != null) {
               particle.setPower(f);
            }
         }

         if (pData == 1) {
            this.level.playLocalSound(pPos, SoundEvents.DRAGON_FIREBALL_EXPLODE, SoundSource.HOSTILE, 1.0F, random.nextFloat() * 0.1F + 0.9F, false);
         }
         break;
      case 2008:
         this.level.addParticle(ParticleTypes.EXPLOSION, (double)pPos.getX() + 0.5D, (double)pPos.getY() + 0.5D, (double)pPos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
         break;
      case 2009:
         for(int i = 0; i < 8; ++i) {
            this.level.addParticle(ParticleTypes.CLOUD, (double)pPos.getX() + random.nextDouble(), (double)pPos.getY() + 1.2D, (double)pPos.getZ() + random.nextDouble(), 0.0D, 0.0D, 0.0D);
         }
         break;
      case 3000:
         this.level.addParticle(ParticleTypes.EXPLOSION_EMITTER, true, (double)pPos.getX() + 0.5D, (double)pPos.getY() + 0.5D, (double)pPos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D);
         this.level.playLocalSound(pPos, SoundEvents.END_GATEWAY_SPAWN, SoundSource.BLOCKS, 10.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
         break;
      case 3001:
         this.level.playLocalSound(pPos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 64.0F, 0.8F + this.level.random.nextFloat() * 0.3F, false);
         break;
      case 3002:
         if (pData >= 0 && pData < Direction.Axis.VALUES.length) {
            ParticleUtils.spawnParticlesAlongAxis(Direction.Axis.VALUES[pData], this.level, pPos, 0.125D, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(10, 19));
         } else {
            ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.ELECTRIC_SPARK, UniformInt.of(3, 5));
         }
         break;
      case 3003:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.WAX_ON, UniformInt.of(3, 5));
         this.level.playLocalSound(pPos, SoundEvents.HONEYCOMB_WAX_ON, SoundSource.BLOCKS, 1.0F, 1.0F, false);
         break;
      case 3004:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.WAX_OFF, UniformInt.of(3, 5));
         break;
      case 3005:
         ParticleUtils.spawnParticlesOnBlockFaces(this.level, pPos, ParticleTypes.SCRAPE, UniformInt.of(3, 5));
      }

   }

   public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
      if (pProgress >= 0 && pProgress < 10) {
         BlockDestructionProgress blockdestructionprogress1 = this.destroyingBlocks.get(pBreakerId);
         if (blockdestructionprogress1 != null) {
            this.removeProgress(blockdestructionprogress1);
         }

         if (blockdestructionprogress1 == null || blockdestructionprogress1.getPos().getX() != pPos.getX() || blockdestructionprogress1.getPos().getY() != pPos.getY() || blockdestructionprogress1.getPos().getZ() != pPos.getZ()) {
            blockdestructionprogress1 = new BlockDestructionProgress(pBreakerId, pPos);
            this.destroyingBlocks.put(pBreakerId, blockdestructionprogress1);
         }

         blockdestructionprogress1.setProgress(pProgress);
         blockdestructionprogress1.updateTick(this.ticks);
         this.destructionProgress.computeIfAbsent(blockdestructionprogress1.getPos().asLong(), (p_194313_) -> {
            return Sets.newTreeSet();
         }).add(blockdestructionprogress1);
      } else {
         BlockDestructionProgress blockdestructionprogress = this.destroyingBlocks.remove(pBreakerId);
         if (blockdestructionprogress != null) {
            this.removeProgress(blockdestructionprogress);
         }
      }

   }

   public boolean hasRenderedAllChunks() {
      return this.chunkRenderDispatcher.isQueueEmpty();
   }

   public void needsUpdate() {
      this.needsFullRenderChunkUpdate = true;
      this.generateClouds = true;
   }

   public void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntitiesToRemove, Collection<BlockEntity> pBlockEntitiesToAdd) {
      synchronized(this.globalBlockEntities) {
         this.globalBlockEntities.removeAll(pBlockEntitiesToRemove);
         this.globalBlockEntities.addAll(pBlockEntitiesToAdd);
      }
   }

   public static int getLightColor(BlockAndTintGetter pLevel, BlockPos pPos) {
      return getLightColor(pLevel, pLevel.getBlockState(pPos), pPos);
   }

   public static int getLightColor(BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos) {
      if (pState.emissiveRendering(pLevel, pPos)) {
         return 15728880;
      } else {
         int i = pLevel.getBrightness(LightLayer.SKY, pPos);
         int j = pLevel.getBrightness(LightLayer.BLOCK, pPos);
         int k = pState.getLightEmission(pLevel, pPos);
         if (j < k) {
            j = k;
         }

         return i << 20 | j << 4;
      }
   }

   @Nullable
   public RenderTarget entityTarget() {
      return this.entityTarget;
   }

   @Nullable
   public RenderTarget getTranslucentTarget() {
      return this.translucentTarget;
   }

   @Nullable
   public RenderTarget getItemEntityTarget() {
      return this.itemEntityTarget;
   }

   @Nullable
   public RenderTarget getParticlesTarget() {
      return this.particlesTarget;
   }

   @Nullable
   public RenderTarget getWeatherTarget() {
      return this.weatherTarget;
   }

   @Nullable
   public RenderTarget getCloudsTarget() {
      return this.cloudsTarget;
   }

   @OnlyIn(Dist.CLIENT)
   static class RenderChunkInfo {
      final ChunkRenderDispatcher.RenderChunk chunk;
      private byte sourceDirections;
      byte directions;
      final int step;

      RenderChunkInfo(ChunkRenderDispatcher.RenderChunk pChunk, @Nullable Direction pSourceDirection, int pStep) {
         this.chunk = pChunk;
         if (pSourceDirection != null) {
            this.addSourceDirection(pSourceDirection);
         }

         this.step = pStep;
      }

      public void setDirections(byte pDir, Direction pFacing) {
         this.directions = (byte)(this.directions | pDir | 1 << pFacing.ordinal());
      }

      public boolean hasDirection(Direction pFacing) {
         return (this.directions & 1 << pFacing.ordinal()) > 0;
      }

      public void addSourceDirection(Direction pDirection) {
         this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << pDirection.ordinal());
      }

      public boolean hasSourceDirection(int pDirectionIndex) {
         return (this.sourceDirections & 1 << pDirectionIndex) > 0;
      }

      public boolean hasSourceDirections() {
         return this.sourceDirections != 0;
      }

      public int hashCode() {
         return this.chunk.getOrigin().hashCode();
      }

      public boolean equals(Object p_194373_) {
         if (!(p_194373_ instanceof LevelRenderer.RenderChunkInfo)) {
            return false;
         } else {
            LevelRenderer.RenderChunkInfo levelrenderer$renderchunkinfo = (LevelRenderer.RenderChunkInfo)p_194373_;
            return this.chunk.getOrigin().equals(levelrenderer$renderchunkinfo.chunk.getOrigin());
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class RenderChunkStorage {
      public final LevelRenderer.RenderInfoMap renderInfoMap;
      public final LinkedHashSet<LevelRenderer.RenderChunkInfo> renderChunks;

      public RenderChunkStorage(int p_194378_) {
         this.renderInfoMap = new LevelRenderer.RenderInfoMap(p_194378_);
         this.renderChunks = new LinkedHashSet<>(p_194378_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   static class RenderInfoMap {
      private final LevelRenderer.RenderChunkInfo[] infos;

      RenderInfoMap(int pSize) {
         this.infos = new LevelRenderer.RenderChunkInfo[pSize];
      }

      public void put(ChunkRenderDispatcher.RenderChunk pRenderChunk, LevelRenderer.RenderChunkInfo pInfo) {
         this.infos[pRenderChunk.index] = pInfo;
      }

      @Nullable
      public LevelRenderer.RenderChunkInfo get(ChunkRenderDispatcher.RenderChunk pRenderChunk) {
         int i = pRenderChunk.index;
         return i >= 0 && i < this.infos.length ? this.infos[i] : null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class TransparencyShaderException extends RuntimeException {
      public TransparencyShaderException(String pMessage, Throwable pCause) {
         super(pMessage, pCause);
      }
   }
}
