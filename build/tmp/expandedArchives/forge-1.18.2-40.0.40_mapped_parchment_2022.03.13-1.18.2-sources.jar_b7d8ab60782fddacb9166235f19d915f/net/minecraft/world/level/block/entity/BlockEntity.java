package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public abstract class BlockEntity extends net.minecraftforge.common.capabilities.CapabilityProvider<BlockEntity> implements net.minecraftforge.common.extensions.IForgeBlockEntity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private final BlockEntityType<?> type;
   @Nullable
   protected Level level;
   protected final BlockPos worldPosition;
   protected boolean remove;
   private BlockState blockState;
   private CompoundTag customTileData;

   public BlockEntity(BlockEntityType<?> pType, BlockPos pWorldPosition, BlockState pBlockState) {
      super(BlockEntity.class);
      this.type = pType;
      this.worldPosition = pWorldPosition.immutable();
      this.blockState = pBlockState;
      this.gatherCapabilities();
   }

   public static BlockPos getPosFromTag(CompoundTag p_187473_) {
      return new BlockPos(p_187473_.getInt("x"), p_187473_.getInt("y"), p_187473_.getInt("z"));
   }

   @Nullable
   public Level getLevel() {
      return this.level;
   }

   public void setLevel(Level pLevel) {
      this.level = pLevel;
   }

   /**
    * @return whether this BlockEntity's level has been set
    */
   public boolean hasLevel() {
      return this.level != null;
   }

   public void load(CompoundTag pTag) {
      if (pTag.contains("ForgeData")) this.customTileData = pTag.getCompound("ForgeData");
      if (getCapabilities() != null && pTag.contains("ForgeCaps")) deserializeCaps(pTag.getCompound("ForgeCaps"));
   }

   protected void saveAdditional(CompoundTag pTag) {
   }

   public final CompoundTag saveWithFullMetadata() {
      CompoundTag compoundtag = this.saveWithoutMetadata();
      this.saveMetadata(compoundtag);
      return compoundtag;
   }

   public final CompoundTag saveWithId() {
      CompoundTag compoundtag = this.saveWithoutMetadata();
      this.saveId(compoundtag);
      return compoundtag;
   }

   public final CompoundTag saveWithoutMetadata() {
      CompoundTag compoundtag = new CompoundTag();
      this.saveAdditional(compoundtag);
      return compoundtag;
   }

   private void saveId(CompoundTag p_187475_) {
      ResourceLocation resourcelocation = BlockEntityType.getKey(this.getType());
      if (resourcelocation == null) {
         throw new RuntimeException(this.getClass() + " is missing a mapping! This is a bug!");
      } else {
         p_187475_.putString("id", resourcelocation.toString());
      }
   }

   public static void addEntityType(CompoundTag p_187469_, BlockEntityType<?> p_187470_) {
      p_187469_.putString("id", BlockEntityType.getKey(p_187470_).toString());
   }

   public void saveToItem(ItemStack p_187477_) {
      BlockItem.setBlockEntityData(p_187477_, this.getType(), this.saveWithoutMetadata());
   }

   private void saveMetadata(CompoundTag p_187479_) {
      this.saveId(p_187479_);
      p_187479_.putInt("x", this.worldPosition.getX());
      p_187479_.putInt("y", this.worldPosition.getY());
      p_187479_.putInt("z", this.worldPosition.getZ());
         if (this.customTileData != null) p_187479_.put("ForgeData", this.customTileData.copy());
         if (getCapabilities() != null) p_187479_.put("ForgeCaps", serializeCaps());
   }

   @Nullable
   public static BlockEntity loadStatic(BlockPos pPos, BlockState pState, CompoundTag pTag) {
      String s = pTag.getString("id");
      ResourceLocation resourcelocation = ResourceLocation.tryParse(s);
      if (resourcelocation == null) {
         LOGGER.error("Block entity has invalid type: {}", (Object)s);
         return null;
      } else {
         return Registry.BLOCK_ENTITY_TYPE.getOptional(resourcelocation).map((p_155240_) -> {
            try {
               return p_155240_.create(pPos, pState);
            } catch (Throwable throwable) {
               LOGGER.error("Failed to create block entity {}", s, throwable);
               return null;
            }
         }).map((p_155249_) -> {
            try {
               p_155249_.load(pTag);
               return p_155249_;
            } catch (Throwable throwable) {
               LOGGER.error("Failed to load data for block entity {}", s, throwable);
               return null;
            }
         }).orElseGet(() -> {
            LOGGER.warn("Skipping BlockEntity with id {}", (Object)s);
            return null;
         });
      }
   }

   /**
    * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it
    * hasn't changed and skip it.
    */
   public void setChanged() {
      if (this.level != null) {
         setChanged(this.level, this.worldPosition, this.blockState);
      }

   }

   protected static void setChanged(Level pLevel, BlockPos pPos, BlockState pState) {
      pLevel.blockEntityChanged(pPos);
      if (!pState.isAir()) {
         pLevel.updateNeighbourForOutputSignal(pPos, pState.getBlock());
      }

   }

   public BlockPos getBlockPos() {
      return this.worldPosition;
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   /**
    * Retrieves packet to send to the client whenever this Tile Entity is resynced via World.notifyBlockUpdate. For
    * modded TE's, this packet comes back to you clientside in {@link #onDataPacket}
    */
   @Nullable
   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return null;
   }

   /**
    * Get an NBT compound to sync to the client with SPacketChunkData, used for initial loading of the chunk or when
    * many blocks change at once. This compound comes back to you clientside in {@link handleUpdateTag}
    */
   public CompoundTag getUpdateTag() {
      return new CompoundTag();
   }

   public boolean isRemoved() {
      return this.remove;
   }

   /**
    * Marks this {@code BlockEntity} as no longer valid (removed from the level).
    */
   public void setRemoved() {
      this.remove = true;
      this.invalidateCaps();
      requestModelDataUpdate();
   }

   @Override
   public void onChunkUnloaded() {
      this.invalidateCaps();
   }

   /**
    * Marks this {@code BlockEntity} as valid again (no longer removed from the level).
    */
   public void clearRemoved() {
      this.remove = false;
   }

   public boolean triggerEvent(int pId, int pType) {
      return false;
   }

   public void fillCrashReportCategory(CrashReportCategory pReportCategory) {
      pReportCategory.setDetail("Name", () -> {
         return Registry.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
      });
      if (this.level != null) {
         CrashReportCategory.populateBlockDetails(pReportCategory, this.level, this.worldPosition, this.getBlockState());
         CrashReportCategory.populateBlockDetails(pReportCategory, this.level, this.worldPosition, this.level.getBlockState(this.worldPosition));
      }
   }

   public boolean onlyOpCanSetNbt() {
      return false;
   }

   public BlockEntityType<?> getType() {
      return this.type;
   }

   @Override
   public CompoundTag getTileData() {
      if (this.customTileData == null)
         this.customTileData = new CompoundTag();
      return this.customTileData;
   }

   /** @deprecated */
   @Deprecated
   public void setBlockState(BlockState pBlockState) {
      this.blockState = pBlockState;
   }
}
