package net.minecraft.world.level.block;

import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractCandleBlock extends Block {
   public static final int LIGHT_PER_CANDLE = 3;
   public static final BooleanProperty LIT = BlockStateProperties.LIT;

   protected AbstractCandleBlock(BlockBehaviour.Properties p_151898_) {
      super(p_151898_);
   }

   protected abstract Iterable<Vec3> getParticleOffsets(BlockState pState);

   public static boolean isLit(BlockState pState) {
      return pState.hasProperty(LIT) && (pState.is(BlockTags.CANDLES) || pState.is(BlockTags.CANDLE_CAKES)) && pState.getValue(LIT);
   }

   public void onProjectileHit(Level pLevel, BlockState pState, BlockHitResult pHit, Projectile pProjectile) {
      if (!pLevel.isClientSide && pProjectile.isOnFire() && this.canBeLit(pState)) {
         setLit(pLevel, pState, pHit.getBlockPos(), true);
      }

   }

   protected boolean canBeLit(BlockState pState) {
      return !pState.getValue(LIT);
   }

   /**
    * Called periodically clientside on blocks near the player to show effects (like furnace fire particles).
    */
   public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, Random pRandom) {
      if (pState.getValue(LIT)) {
         this.getParticleOffsets(pState).forEach((p_151917_) -> {
            addParticlesAndSound(pLevel, p_151917_.add((double)pPos.getX(), (double)pPos.getY(), (double)pPos.getZ()), pRandom);
         });
      }
   }

   private static void addParticlesAndSound(Level p_151910_, Vec3 p_151911_, Random p_151912_) {
      float f = p_151912_.nextFloat();
      if (f < 0.3F) {
         p_151910_.addParticle(ParticleTypes.SMOKE, p_151911_.x, p_151911_.y, p_151911_.z, 0.0D, 0.0D, 0.0D);
         if (f < 0.17F) {
            p_151910_.playLocalSound(p_151911_.x + 0.5D, p_151911_.y + 0.5D, p_151911_.z + 0.5D, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS, 1.0F + p_151912_.nextFloat(), p_151912_.nextFloat() * 0.7F + 0.3F, false);
         }
      }

      p_151910_.addParticle(ParticleTypes.SMALL_FLAME, p_151911_.x, p_151911_.y, p_151911_.z, 0.0D, 0.0D, 0.0D);
   }

   public static void extinguish(@Nullable Player p_151900_, BlockState p_151901_, LevelAccessor p_151902_, BlockPos p_151903_) {
      setLit(p_151902_, p_151901_, p_151903_, false);
      if (p_151901_.getBlock() instanceof AbstractCandleBlock) {
         ((AbstractCandleBlock)p_151901_.getBlock()).getParticleOffsets(p_151901_).forEach((p_151926_) -> {
            p_151902_.addParticle(ParticleTypes.SMOKE, (double)p_151903_.getX() + p_151926_.x(), (double)p_151903_.getY() + p_151926_.y(), (double)p_151903_.getZ() + p_151926_.z(), 0.0D, (double)0.1F, 0.0D);
         });
      }

      p_151902_.playSound((Player)null, p_151903_, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
      p_151902_.gameEvent(p_151900_, GameEvent.BLOCK_CHANGE, p_151903_);
   }

   private static void setLit(LevelAccessor pLevel, BlockState pState, BlockPos pPos, boolean pLit) {
      pLevel.setBlock(pPos, pState.setValue(LIT, Boolean.valueOf(pLit)), 11);
   }
}