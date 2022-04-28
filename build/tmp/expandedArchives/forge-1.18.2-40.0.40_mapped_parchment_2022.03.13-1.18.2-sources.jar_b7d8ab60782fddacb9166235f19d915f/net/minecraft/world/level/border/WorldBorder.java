package net.minecraft.world.level.border;

import com.google.common.collect.Lists;
import com.mojang.serialization.DynamicLike;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldBorder {
   public static final double MAX_SIZE = 5.9999968E7D;
   public static final double MAX_CENTER_COORDINATE = 2.9999984E7D;
   private final List<BorderChangeListener> listeners = Lists.newArrayList();
   private double damagePerBlock = 0.2D;
   private double damageSafeZone = 5.0D;
   private int warningTime = 15;
   private int warningBlocks = 5;
   private double centerX;
   private double centerZ;
   int absoluteMaxSize = 29999984;
   private WorldBorder.BorderExtent extent = new WorldBorder.StaticBorderExtent(5.9999968E7D);
   public static final WorldBorder.Settings DEFAULT_SETTINGS = new WorldBorder.Settings(0.0D, 0.0D, 0.2D, 5.0D, 5, 15, 5.9999968E7D, 0L, 0.0D);

   public boolean isWithinBounds(BlockPos pPos) {
      return (double)(pPos.getX() + 1) > this.getMinX() && (double)pPos.getX() < this.getMaxX() && (double)(pPos.getZ() + 1) > this.getMinZ() && (double)pPos.getZ() < this.getMaxZ();
   }

   public boolean isWithinBounds(ChunkPos pChunkPos) {
      return (double)pChunkPos.getMaxBlockX() > this.getMinX() && (double)pChunkPos.getMinBlockX() < this.getMaxX() && (double)pChunkPos.getMaxBlockZ() > this.getMinZ() && (double)pChunkPos.getMinBlockZ() < this.getMaxZ();
   }

   public boolean isWithinBounds(double pX, double pZ) {
      return pX > this.getMinX() && pX < this.getMaxX() && pZ > this.getMinZ() && pZ < this.getMaxZ();
   }

   public boolean isWithinBounds(double p_187563_, double p_187564_, double p_187565_) {
      return p_187563_ > this.getMinX() - p_187565_ && p_187563_ < this.getMaxX() + p_187565_ && p_187564_ > this.getMinZ() - p_187565_ && p_187564_ < this.getMaxZ() + p_187565_;
   }

   public boolean isWithinBounds(AABB pBox) {
      return pBox.maxX > this.getMinX() && pBox.minX < this.getMaxX() && pBox.maxZ > this.getMinZ() && pBox.minZ < this.getMaxZ();
   }

   public BlockPos clampToBounds(double p_187570_, double p_187571_, double p_187572_) {
      return new BlockPos(Mth.clamp(p_187570_, this.getMinX(), this.getMaxX()), p_187571_, Mth.clamp(p_187572_, this.getMinZ(), this.getMaxZ()));
   }

   public double getDistanceToBorder(Entity pEntity) {
      return this.getDistanceToBorder(pEntity.getX(), pEntity.getZ());
   }

   public VoxelShape getCollisionShape() {
      return this.extent.getCollisionShape();
   }

   public double getDistanceToBorder(double pX, double pZ) {
      double d0 = pZ - this.getMinZ();
      double d1 = this.getMaxZ() - pZ;
      double d2 = pX - this.getMinX();
      double d3 = this.getMaxX() - pX;
      double d4 = Math.min(d2, d3);
      d4 = Math.min(d4, d0);
      return Math.min(d4, d1);
   }

   public boolean isInsideCloseToBorder(Entity p_187567_, AABB p_187568_) {
      double d0 = Math.max(Mth.absMax(p_187568_.getXsize(), p_187568_.getZsize()), 1.0D);
      return this.getDistanceToBorder(p_187567_) < d0 * 2.0D && this.isWithinBounds(p_187567_.getX(), p_187567_.getZ(), d0);
   }

   public BorderStatus getStatus() {
      return this.extent.getStatus();
   }

   public double getMinX() {
      return this.extent.getMinX();
   }

   public double getMinZ() {
      return this.extent.getMinZ();
   }

   public double getMaxX() {
      return this.extent.getMaxX();
   }

   public double getMaxZ() {
      return this.extent.getMaxZ();
   }

   public double getCenterX() {
      return this.centerX;
   }

   public double getCenterZ() {
      return this.centerZ;
   }

   public void setCenter(double pX, double pZ) {
      this.centerX = pX;
      this.centerZ = pZ;
      this.extent.onCenterChange();

      for(BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onBorderCenterSet(this, pX, pZ);
      }

   }

   public double getSize() {
      return this.extent.getSize();
   }

   public long getLerpRemainingTime() {
      return this.extent.getLerpRemainingTime();
   }

   public double getLerpTarget() {
      return this.extent.getLerpTarget();
   }

   public void setSize(double pSize) {
      this.extent = new WorldBorder.StaticBorderExtent(pSize);

      for(BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onBorderSizeSet(this, pSize);
      }

   }

   public void lerpSizeBetween(double pOldSize, double pNewSize, long pTime) {
      this.extent = (WorldBorder.BorderExtent)(pOldSize == pNewSize ? new WorldBorder.StaticBorderExtent(pNewSize) : new WorldBorder.MovingBorderExtent(pOldSize, pNewSize, pTime));

      for(BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onBorderSizeLerping(this, pOldSize, pNewSize, pTime);
      }

   }

   protected List<BorderChangeListener> getListeners() {
      return Lists.newArrayList(this.listeners);
   }

   public void addListener(BorderChangeListener pListener) {
      this.listeners.add(pListener);
   }

   public void removeListener(BorderChangeListener pListener) {
      this.listeners.remove(pListener);
   }

   public void setAbsoluteMaxSize(int pSize) {
      this.absoluteMaxSize = pSize;
      this.extent.onAbsoluteMaxSizeChange();
   }

   public int getAbsoluteMaxSize() {
      return this.absoluteMaxSize;
   }

   public double getDamageSafeZone() {
      return this.damageSafeZone;
   }

   public void setDamageSafeZone(double pDamageSafeZone) {
      this.damageSafeZone = pDamageSafeZone;

      for(BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onBorderSetDamageSafeZOne(this, pDamageSafeZone);
      }

   }

   public double getDamagePerBlock() {
      return this.damagePerBlock;
   }

   public void setDamagePerBlock(double pDamagePerBlock) {
      this.damagePerBlock = pDamagePerBlock;

      for(BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onBorderSetDamagePerBlock(this, pDamagePerBlock);
      }

   }

   public double getLerpSpeed() {
      return this.extent.getLerpSpeed();
   }

   public int getWarningTime() {
      return this.warningTime;
   }

   public void setWarningTime(int pWarningTime) {
      this.warningTime = pWarningTime;

      for(BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onBorderSetWarningTime(this, pWarningTime);
      }

   }

   public int getWarningBlocks() {
      return this.warningBlocks;
   }

   public void setWarningBlocks(int pWarningDistance) {
      this.warningBlocks = pWarningDistance;

      for(BorderChangeListener borderchangelistener : this.getListeners()) {
         borderchangelistener.onBorderSetWarningBlocks(this, pWarningDistance);
      }

   }

   public void tick() {
      this.extent = this.extent.update();
   }

   public WorldBorder.Settings createSettings() {
      return new WorldBorder.Settings(this);
   }

   public void applySettings(WorldBorder.Settings pSerializer) {
      this.setCenter(pSerializer.getCenterX(), pSerializer.getCenterZ());
      this.setDamagePerBlock(pSerializer.getDamagePerBlock());
      this.setDamageSafeZone(pSerializer.getSafeZone());
      this.setWarningBlocks(pSerializer.getWarningBlocks());
      this.setWarningTime(pSerializer.getWarningTime());
      if (pSerializer.getSizeLerpTime() > 0L) {
         this.lerpSizeBetween(pSerializer.getSize(), pSerializer.getSizeLerpTarget(), pSerializer.getSizeLerpTime());
      } else {
         this.setSize(pSerializer.getSize());
      }

   }

   interface BorderExtent {
      double getMinX();

      double getMaxX();

      double getMinZ();

      double getMaxZ();

      double getSize();

      double getLerpSpeed();

      long getLerpRemainingTime();

      double getLerpTarget();

      BorderStatus getStatus();

      void onAbsoluteMaxSizeChange();

      void onCenterChange();

      WorldBorder.BorderExtent update();

      VoxelShape getCollisionShape();
   }

   class MovingBorderExtent implements WorldBorder.BorderExtent {
      private final double from;
      private final double to;
      private final long lerpEnd;
      private final long lerpBegin;
      private final double lerpDuration;

      MovingBorderExtent(double p_61979_, double p_61980_, long p_61981_) {
         this.from = p_61979_;
         this.to = p_61980_;
         this.lerpDuration = (double)p_61981_;
         this.lerpBegin = Util.getMillis();
         this.lerpEnd = this.lerpBegin + p_61981_;
      }

      public double getMinX() {
         return Mth.clamp(WorldBorder.this.getCenterX() - this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getMinZ() {
         return Mth.clamp(WorldBorder.this.getCenterZ() - this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getMaxX() {
         return Mth.clamp(WorldBorder.this.getCenterX() + this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getMaxZ() {
         return Mth.clamp(WorldBorder.this.getCenterZ() + this.getSize() / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
      }

      public double getSize() {
         double d0 = (double)(Util.getMillis() - this.lerpBegin) / this.lerpDuration;
         return d0 < 1.0D ? Mth.lerp(d0, this.from, this.to) : this.to;
      }

      public double getLerpSpeed() {
         return Math.abs(this.from - this.to) / (double)(this.lerpEnd - this.lerpBegin);
      }

      public long getLerpRemainingTime() {
         return this.lerpEnd - Util.getMillis();
      }

      public double getLerpTarget() {
         return this.to;
      }

      public BorderStatus getStatus() {
         return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
      }

      public void onCenterChange() {
      }

      public void onAbsoluteMaxSizeChange() {
      }

      public WorldBorder.BorderExtent update() {
         return (WorldBorder.BorderExtent)(this.getLerpRemainingTime() <= 0L ? WorldBorder.this.new StaticBorderExtent(this.to) : this);
      }

      public VoxelShape getCollisionShape() {
         return Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX()), Double.NEGATIVE_INFINITY, Math.floor(this.getMinZ()), Math.ceil(this.getMaxX()), Double.POSITIVE_INFINITY, Math.ceil(this.getMaxZ())), BooleanOp.ONLY_FIRST);
      }
   }

   public static class Settings {
      private final double centerX;
      private final double centerZ;
      private final double damagePerBlock;
      private final double safeZone;
      private final int warningBlocks;
      private final int warningTime;
      private final double size;
      private final long sizeLerpTime;
      private final double sizeLerpTarget;

      Settings(double pCenterX, double pCenterZ, double pDamagePerBlock, double pSafeZone, int pWarningBlocks, int pWarningTime, double pSize, long pSizeLerpTime, double pSizeLerpTarget) {
         this.centerX = pCenterX;
         this.centerZ = pCenterZ;
         this.damagePerBlock = pDamagePerBlock;
         this.safeZone = pSafeZone;
         this.warningBlocks = pWarningBlocks;
         this.warningTime = pWarningTime;
         this.size = pSize;
         this.sizeLerpTime = pSizeLerpTime;
         this.sizeLerpTarget = pSizeLerpTarget;
      }

      Settings(WorldBorder pBorder) {
         this.centerX = pBorder.getCenterX();
         this.centerZ = pBorder.getCenterZ();
         this.damagePerBlock = pBorder.getDamagePerBlock();
         this.safeZone = pBorder.getDamageSafeZone();
         this.warningBlocks = pBorder.getWarningBlocks();
         this.warningTime = pBorder.getWarningTime();
         this.size = pBorder.getSize();
         this.sizeLerpTime = pBorder.getLerpRemainingTime();
         this.sizeLerpTarget = pBorder.getLerpTarget();
      }

      public double getCenterX() {
         return this.centerX;
      }

      public double getCenterZ() {
         return this.centerZ;
      }

      public double getDamagePerBlock() {
         return this.damagePerBlock;
      }

      public double getSafeZone() {
         return this.safeZone;
      }

      public int getWarningBlocks() {
         return this.warningBlocks;
      }

      public int getWarningTime() {
         return this.warningTime;
      }

      public double getSize() {
         return this.size;
      }

      public long getSizeLerpTime() {
         return this.sizeLerpTime;
      }

      public double getSizeLerpTarget() {
         return this.sizeLerpTarget;
      }

      public static WorldBorder.Settings read(DynamicLike<?> pDynamic, WorldBorder.Settings pDefaultValue) {
         double d0 = Mth.clamp(pDynamic.get("BorderCenterX").asDouble(pDefaultValue.centerX), -2.9999984E7D, 2.9999984E7D);
         double d1 = Mth.clamp(pDynamic.get("BorderCenterZ").asDouble(pDefaultValue.centerZ), -2.9999984E7D, 2.9999984E7D);
         double d2 = pDynamic.get("BorderSize").asDouble(pDefaultValue.size);
         long i = pDynamic.get("BorderSizeLerpTime").asLong(pDefaultValue.sizeLerpTime);
         double d3 = pDynamic.get("BorderSizeLerpTarget").asDouble(pDefaultValue.sizeLerpTarget);
         double d4 = pDynamic.get("BorderSafeZone").asDouble(pDefaultValue.safeZone);
         double d5 = pDynamic.get("BorderDamagePerBlock").asDouble(pDefaultValue.damagePerBlock);
         int j = pDynamic.get("BorderWarningBlocks").asInt(pDefaultValue.warningBlocks);
         int k = pDynamic.get("BorderWarningTime").asInt(pDefaultValue.warningTime);
         return new WorldBorder.Settings(d0, d1, d5, d4, j, k, d2, i, d3);
      }

      public void write(CompoundTag pNbt) {
         pNbt.putDouble("BorderCenterX", this.centerX);
         pNbt.putDouble("BorderCenterZ", this.centerZ);
         pNbt.putDouble("BorderSize", this.size);
         pNbt.putLong("BorderSizeLerpTime", this.sizeLerpTime);
         pNbt.putDouble("BorderSafeZone", this.safeZone);
         pNbt.putDouble("BorderDamagePerBlock", this.damagePerBlock);
         pNbt.putDouble("BorderSizeLerpTarget", this.sizeLerpTarget);
         pNbt.putDouble("BorderWarningBlocks", (double)this.warningBlocks);
         pNbt.putDouble("BorderWarningTime", (double)this.warningTime);
      }
   }

   class StaticBorderExtent implements WorldBorder.BorderExtent {
      private final double size;
      private double minX;
      private double minZ;
      private double maxX;
      private double maxZ;
      private VoxelShape shape;

      public StaticBorderExtent(double p_62059_) {
         this.size = p_62059_;
         this.updateBox();
      }

      public double getMinX() {
         return this.minX;
      }

      public double getMaxX() {
         return this.maxX;
      }

      public double getMinZ() {
         return this.minZ;
      }

      public double getMaxZ() {
         return this.maxZ;
      }

      public double getSize() {
         return this.size;
      }

      public BorderStatus getStatus() {
         return BorderStatus.STATIONARY;
      }

      public double getLerpSpeed() {
         return 0.0D;
      }

      public long getLerpRemainingTime() {
         return 0L;
      }

      public double getLerpTarget() {
         return this.size;
      }

      private void updateBox() {
         this.minX = Mth.clamp(WorldBorder.this.getCenterX() - this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.minZ = Mth.clamp(WorldBorder.this.getCenterZ() - this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.maxX = Mth.clamp(WorldBorder.this.getCenterX() + this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.maxZ = Mth.clamp(WorldBorder.this.getCenterZ() + this.size / 2.0D, (double)(-WorldBorder.this.absoluteMaxSize), (double)WorldBorder.this.absoluteMaxSize);
         this.shape = Shapes.join(Shapes.INFINITY, Shapes.box(Math.floor(this.getMinX()), Double.NEGATIVE_INFINITY, Math.floor(this.getMinZ()), Math.ceil(this.getMaxX()), Double.POSITIVE_INFINITY, Math.ceil(this.getMaxZ())), BooleanOp.ONLY_FIRST);
      }

      public void onAbsoluteMaxSizeChange() {
         this.updateBox();
      }

      public void onCenterChange() {
         this.updateBox();
      }

      public WorldBorder.BorderExtent update() {
         return this;
      }

      public VoxelShape getCollisionShape() {
         return this.shape;
      }
   }
}