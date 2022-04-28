package net.minecraft.world.level.material;

import java.util.Optional;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.IdMapper;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class Fluid extends net.minecraftforge.registries.ForgeRegistryEntry<Fluid> implements net.minecraftforge.common.extensions.IForgeFluid {
   public static final IdMapper<FluidState> FLUID_STATE_REGISTRY = new IdMapper<>();
   protected final StateDefinition<Fluid, FluidState> stateDefinition;
   private FluidState defaultFluidState;

   protected Fluid() {
      StateDefinition.Builder<Fluid, FluidState> builder = new StateDefinition.Builder<>(this);
      this.createFluidStateDefinition(builder);
      this.stateDefinition = builder.create(Fluid::defaultFluidState, FluidState::new);
      this.registerDefaultState(this.stateDefinition.any());
   }

   protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> pBuilder) {
   }

   public StateDefinition<Fluid, FluidState> getStateDefinition() {
      return this.stateDefinition;
   }

   protected final void registerDefaultState(FluidState pState) {
      this.defaultFluidState = pState;
   }

   public final FluidState defaultFluidState() {
      return this.defaultFluidState;
   }

   public abstract Item getBucket();

   protected void animateTick(Level pLevel, BlockPos pPos, FluidState pState, Random pRandom) {
   }

   protected void tick(Level pLevel, BlockPos pPos, FluidState pState) {
   }

   protected void randomTick(Level pLevel, BlockPos pPos, FluidState pState, Random pRandom) {
   }

   @Nullable
   protected ParticleOptions getDripParticle() {
      return null;
   }

   protected abstract boolean canBeReplacedWith(FluidState pFluidState, BlockGetter pBlockReader, BlockPos pPos, Fluid pFluid, Direction pDirection);

   protected abstract Vec3 getFlow(BlockGetter pBlockReader, BlockPos pPos, FluidState pFluidState);

   public abstract int getTickDelay(LevelReader p_76120_);

   protected boolean isRandomlyTicking() {
      return false;
   }

   protected boolean isEmpty() {
      return false;
   }

   protected abstract float getExplosionResistance();

   public abstract float getHeight(FluidState p_76124_, BlockGetter p_76125_, BlockPos p_76126_);

   public abstract float getOwnHeight(FluidState p_76123_);

   protected abstract BlockState createLegacyBlock(FluidState pState);

   public abstract boolean isSource(FluidState pState);

   public abstract int getAmount(FluidState pState);

   public boolean isSame(Fluid pFluid) {
      return pFluid == this;
   }

   public boolean is(Tag<Fluid> pTag) {
      return pTag.contains(this);
   }

   public abstract VoxelShape getShape(FluidState p_76137_, BlockGetter p_76138_, BlockPos p_76139_);

   private final net.minecraftforge.common.util.ReverseTagWrapper<Fluid> reverseTags = new net.minecraftforge.common.util.ReverseTagWrapper<>(this, net.minecraft.tags.FluidTags::getAllTags);
   @Override
   public java.util.Set<net.minecraft.resources.ResourceLocation> getTags() {
      return reverseTags.getTagNames();
   }

   /**
    * Creates the fluid attributes object, which will contain all the extended values for the fluid that aren't part of the vanilla system.
    * Do not call this from outside. To retrieve the values use {@link Fluid#getAttributes()}
    */
   protected net.minecraftforge.fluids.FluidAttributes createAttributes()
   {
      return net.minecraftforge.common.ForgeHooks.createVanillaFluidAttributes(this);
   }

   private net.minecraftforge.fluids.FluidAttributes forgeFluidAttributes;
   public final net.minecraftforge.fluids.FluidAttributes getAttributes() {
      if (forgeFluidAttributes == null)
         forgeFluidAttributes = createAttributes();
      return forgeFluidAttributes;
   }

   public Optional<SoundEvent> getPickupSound() {
      return Optional.empty();
   }
}
