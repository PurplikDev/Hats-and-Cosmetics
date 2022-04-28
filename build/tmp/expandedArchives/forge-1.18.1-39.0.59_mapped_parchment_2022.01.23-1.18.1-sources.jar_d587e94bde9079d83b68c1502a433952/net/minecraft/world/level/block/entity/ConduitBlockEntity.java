package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ConduitBlockEntity extends BlockEntity {
   private static final int BLOCK_REFRESH_RATE = 2;
   private static final int EFFECT_DURATION = 13;
   private static final float ROTATION_SPEED = -0.0375F;
   private static final int MIN_ACTIVE_SIZE = 16;
   private static final int MIN_KILL_SIZE = 42;
   private static final int KILL_RANGE = 8;
   private static final Block[] VALID_BLOCKS = new Block[]{Blocks.PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.SEA_LANTERN, Blocks.DARK_PRISMARINE};
   public int tickCount;
   private float activeRotation;
   private boolean isActive;
   private boolean isHunting;
   private final List<BlockPos> effectBlocks = Lists.newArrayList();
   @Nullable
   private LivingEntity destroyTarget;
   @Nullable
   private UUID destroyTargetUUID;
   private long nextAmbientSoundActivation;

   public ConduitBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
      super(BlockEntityType.CONDUIT, pWorldPosition, pBlockState);
   }

   public void load(CompoundTag pTag) {
      super.load(pTag);
      if (pTag.hasUUID("Target")) {
         this.destroyTargetUUID = pTag.getUUID("Target");
      } else {
         this.destroyTargetUUID = null;
      }

   }

   protected void saveAdditional(CompoundTag pTag) {
      super.saveAdditional(pTag);
      if (this.destroyTarget != null) {
         pTag.putUUID("Target", this.destroyTarget.getUUID());
      }

   }

   /**
    * Retrieves packet to send to the client whenever this Tile Entity is resynced via World.notifyBlockUpdate. For
    * modded TE's, this packet comes back to you clientside in {@link #onDataPacket}
    */
   public ClientboundBlockEntityDataPacket getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }

   /**
    * Get an NBT compound to sync to the client with SPacketChunkData, used for initial loading of the chunk or when
    * many blocks change at once. This compound comes back to you clientside in {@link handleUpdateTag}
    */
   public CompoundTag getUpdateTag() {
      return this.saveWithoutMetadata();
   }

   public static void clientTick(Level pLevel, BlockPos pPos, BlockState pState, ConduitBlockEntity pBlockEntity) {
      ++pBlockEntity.tickCount;
      long i = pLevel.getGameTime();
      List<BlockPos> list = pBlockEntity.effectBlocks;
      if (i % 40L == 0L) {
         pBlockEntity.isActive = updateShape(pLevel, pPos, list);
         updateHunting(pBlockEntity, list);
      }

      updateClientTarget(pLevel, pPos, pBlockEntity);
      animationTick(pLevel, pPos, list, pBlockEntity.destroyTarget, pBlockEntity.tickCount);
      if (pBlockEntity.isActive()) {
         ++pBlockEntity.activeRotation;
      }

   }

   public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, ConduitBlockEntity pBlockEntity) {
      ++pBlockEntity.tickCount;
      long i = pLevel.getGameTime();
      List<BlockPos> list = pBlockEntity.effectBlocks;
      if (i % 40L == 0L) {
         boolean flag = updateShape(pLevel, pPos, list);
         if (flag != pBlockEntity.isActive) {
            SoundEvent soundevent = flag ? SoundEvents.CONDUIT_ACTIVATE : SoundEvents.CONDUIT_DEACTIVATE;
            pLevel.playSound((Player)null, pPos, soundevent, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         pBlockEntity.isActive = flag;
         updateHunting(pBlockEntity, list);
         if (flag) {
            applyEffects(pLevel, pPos, list);
            updateDestroyTarget(pLevel, pPos, pState, list, pBlockEntity);
         }
      }

      if (pBlockEntity.isActive()) {
         if (i % 80L == 0L) {
            pLevel.playSound((Player)null, pPos, SoundEvents.CONDUIT_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }

         if (i > pBlockEntity.nextAmbientSoundActivation) {
            pBlockEntity.nextAmbientSoundActivation = i + 60L + (long)pLevel.getRandom().nextInt(40);
            pLevel.playSound((Player)null, pPos, SoundEvents.CONDUIT_AMBIENT_SHORT, SoundSource.BLOCKS, 1.0F, 1.0F);
         }
      }

   }

   private static void updateHunting(ConduitBlockEntity p_155429_, List<BlockPos> p_155430_) {
      p_155429_.setHunting(p_155430_.size() >= 42);
   }

   private static boolean updateShape(Level p_155415_, BlockPos p_155416_, List<BlockPos> p_155417_) {
      p_155417_.clear();

      for(int i = -1; i <= 1; ++i) {
         for(int j = -1; j <= 1; ++j) {
            for(int k = -1; k <= 1; ++k) {
               BlockPos blockpos = p_155416_.offset(i, j, k);
               if (!p_155415_.isWaterAt(blockpos)) {
                  return false;
               }
            }
         }
      }

      for(int j1 = -2; j1 <= 2; ++j1) {
         for(int k1 = -2; k1 <= 2; ++k1) {
            for(int l1 = -2; l1 <= 2; ++l1) {
               int i2 = Math.abs(j1);
               int l = Math.abs(k1);
               int i1 = Math.abs(l1);
               if ((i2 > 1 || l > 1 || i1 > 1) && (j1 == 0 && (l == 2 || i1 == 2) || k1 == 0 && (i2 == 2 || i1 == 2) || l1 == 0 && (i2 == 2 || l == 2))) {
                  BlockPos blockpos1 = p_155416_.offset(j1, k1, l1);
                  BlockState blockstate = p_155415_.getBlockState(blockpos1);

                  if (blockstate.isConduitFrame(p_155415_, blockpos1, p_155416_)) {
                     p_155417_.add(blockpos1);
                  }
               }
            }
         }
      }

      return p_155417_.size() >= 16;
   }

   private static void applyEffects(Level p_155444_, BlockPos p_155445_, List<BlockPos> p_155446_) {
      int i = p_155446_.size();
      int j = i / 7 * 16;
      int k = p_155445_.getX();
      int l = p_155445_.getY();
      int i1 = p_155445_.getZ();
      AABB aabb = (new AABB((double)k, (double)l, (double)i1, (double)(k + 1), (double)(l + 1), (double)(i1 + 1))).inflate((double)j).expandTowards(0.0D, (double)p_155444_.getHeight(), 0.0D);
      List<Player> list = p_155444_.getEntitiesOfClass(Player.class, aabb);
      if (!list.isEmpty()) {
         for(Player player : list) {
            if (p_155445_.closerThan(player.blockPosition(), (double)j) && player.isInWaterOrRain()) {
               player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 260, 0, true, true));
            }
         }

      }
   }

   private static void updateDestroyTarget(Level p_155409_, BlockPos p_155410_, BlockState p_155411_, List<BlockPos> p_155412_, ConduitBlockEntity p_155413_) {
      LivingEntity livingentity = p_155413_.destroyTarget;
      int i = p_155412_.size();
      if (i < 42) {
         p_155413_.destroyTarget = null;
      } else if (p_155413_.destroyTarget == null && p_155413_.destroyTargetUUID != null) {
         p_155413_.destroyTarget = findDestroyTarget(p_155409_, p_155410_, p_155413_.destroyTargetUUID);
         p_155413_.destroyTargetUUID = null;
      } else if (p_155413_.destroyTarget == null) {
         List<LivingEntity> list = p_155409_.getEntitiesOfClass(LivingEntity.class, getDestroyRangeAABB(p_155410_), (p_59213_) -> {
            return p_59213_ instanceof Enemy && p_59213_.isInWaterOrRain();
         });
         if (!list.isEmpty()) {
            p_155413_.destroyTarget = list.get(p_155409_.random.nextInt(list.size()));
         }
      } else if (!p_155413_.destroyTarget.isAlive() || !p_155410_.closerThan(p_155413_.destroyTarget.blockPosition(), 8.0D)) {
         p_155413_.destroyTarget = null;
      }

      if (p_155413_.destroyTarget != null) {
         p_155409_.playSound((Player)null, p_155413_.destroyTarget.getX(), p_155413_.destroyTarget.getY(), p_155413_.destroyTarget.getZ(), SoundEvents.CONDUIT_ATTACK_TARGET, SoundSource.BLOCKS, 1.0F, 1.0F);
         p_155413_.destroyTarget.hurt(DamageSource.MAGIC, 4.0F);
      }

      if (livingentity != p_155413_.destroyTarget) {
         p_155409_.sendBlockUpdated(p_155410_, p_155411_, p_155411_, 2);
      }

   }

   private static void updateClientTarget(Level pLevel, BlockPos pPos, ConduitBlockEntity pBlockEntity) {
      if (pBlockEntity.destroyTargetUUID == null) {
         pBlockEntity.destroyTarget = null;
      } else if (pBlockEntity.destroyTarget == null || !pBlockEntity.destroyTarget.getUUID().equals(pBlockEntity.destroyTargetUUID)) {
         pBlockEntity.destroyTarget = findDestroyTarget(pLevel, pPos, pBlockEntity.destroyTargetUUID);
         if (pBlockEntity.destroyTarget == null) {
            pBlockEntity.destroyTargetUUID = null;
         }
      }

   }

   private static AABB getDestroyRangeAABB(BlockPos pPos) {
      int i = pPos.getX();
      int j = pPos.getY();
      int k = pPos.getZ();
      return (new AABB((double)i, (double)j, (double)k, (double)(i + 1), (double)(j + 1), (double)(k + 1))).inflate(8.0D);
   }

   @Nullable
   private static LivingEntity findDestroyTarget(Level pLevel, BlockPos pPos, UUID pTargetId) {
      List<LivingEntity> list = pLevel.getEntitiesOfClass(LivingEntity.class, getDestroyRangeAABB(pPos), (p_155435_) -> {
         return p_155435_.getUUID().equals(pTargetId);
      });
      return list.size() == 1 ? list.get(0) : null;
   }

   private static void animationTick(Level p_155419_, BlockPos p_155420_, List<BlockPos> p_155421_, @Nullable Entity p_155422_, int p_155423_) {
      Random random = p_155419_.random;
      double d0 = (double)(Mth.sin((float)(p_155423_ + 35) * 0.1F) / 2.0F + 0.5F);
      d0 = (d0 * d0 + d0) * (double)0.3F;
      Vec3 vec3 = new Vec3((double)p_155420_.getX() + 0.5D, (double)p_155420_.getY() + 1.5D + d0, (double)p_155420_.getZ() + 0.5D);

      for(BlockPos blockpos : p_155421_) {
         if (random.nextInt(50) == 0) {
            BlockPos blockpos1 = blockpos.subtract(p_155420_);
            float f = -0.5F + random.nextFloat() + (float)blockpos1.getX();
            float f1 = -2.0F + random.nextFloat() + (float)blockpos1.getY();
            float f2 = -0.5F + random.nextFloat() + (float)blockpos1.getZ();
            p_155419_.addParticle(ParticleTypes.NAUTILUS, vec3.x, vec3.y, vec3.z, (double)f, (double)f1, (double)f2);
         }
      }

      if (p_155422_ != null) {
         Vec3 vec31 = new Vec3(p_155422_.getX(), p_155422_.getEyeY(), p_155422_.getZ());
         float f3 = (-0.5F + random.nextFloat()) * (3.0F + p_155422_.getBbWidth());
         float f4 = -1.0F + random.nextFloat() * p_155422_.getBbHeight();
         float f5 = (-0.5F + random.nextFloat()) * (3.0F + p_155422_.getBbWidth());
         Vec3 vec32 = new Vec3((double)f3, (double)f4, (double)f5);
         p_155419_.addParticle(ParticleTypes.NAUTILUS, vec31.x, vec31.y, vec31.z, vec32.x, vec32.y, vec32.z);
      }

   }

   public boolean isActive() {
      return this.isActive;
   }

   public boolean isHunting() {
      return this.isHunting;
   }

   private void setHunting(boolean pIsHunting) {
      this.isHunting = pIsHunting;
   }

   public float getActiveRotation(float p_59198_) {
      return (this.activeRotation + p_59198_) * -0.0375F;
   }
}
