package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryWriteOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.feature.structures.JigsawJunction;
import net.minecraft.world.level.levelgen.feature.structures.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PoolElementStructurePiece extends StructurePiece {
   private static final Logger LOGGER = LogManager.getLogger();
   protected final StructurePoolElement element;
   protected BlockPos position;
   private final int groundLevelDelta;
   protected final Rotation rotation;
   private final List<JigsawJunction> junctions = Lists.newArrayList();
   private final StructureManager structureManager;

   public PoolElementStructurePiece(StructureManager pStructureManager, StructurePoolElement pElement, BlockPos pPosition, int pGroundLevelDelta, Rotation pRotation, BoundingBox pBox) {
      super(StructurePieceType.JIGSAW, 0, pBox);
      this.structureManager = pStructureManager;
      this.element = pElement;
      this.position = pPosition;
      this.groundLevelDelta = pGroundLevelDelta;
      this.rotation = pRotation;
   }

   public PoolElementStructurePiece(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      super(StructurePieceType.JIGSAW, pTag);
      this.structureManager = pContext.structureManager();
      this.position = new BlockPos(pTag.getInt("PosX"), pTag.getInt("PosY"), pTag.getInt("PosZ"));
      this.groundLevelDelta = pTag.getInt("ground_level_delta");
      RegistryReadOps<Tag> registryreadops = RegistryReadOps.create(NbtOps.INSTANCE, pContext.resourceManager(), pContext.registryAccess());
      this.element = StructurePoolElement.CODEC.parse(registryreadops, pTag.getCompound("pool_element")).resultOrPartial(LOGGER::error).orElseThrow(() -> {
         return new IllegalStateException("Invalid pool element found");
      });
      this.rotation = Rotation.valueOf(pTag.getString("rotation"));
      this.boundingBox = this.element.getBoundingBox(this.structureManager, this.position, this.rotation);
      ListTag listtag = pTag.getList("junctions", 10);
      this.junctions.clear();
      listtag.forEach((p_163128_) -> {
         this.junctions.add(JigsawJunction.deserialize(new Dynamic<>(registryreadops, p_163128_)));
      });
   }

   protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      pTag.putInt("PosX", this.position.getX());
      pTag.putInt("PosY", this.position.getY());
      pTag.putInt("PosZ", this.position.getZ());
      pTag.putInt("ground_level_delta", this.groundLevelDelta);
      RegistryWriteOps<Tag> registrywriteops = RegistryWriteOps.create(NbtOps.INSTANCE, pContext.registryAccess());
      StructurePoolElement.CODEC.encodeStart(registrywriteops, this.element).resultOrPartial(LOGGER::error).ifPresent((p_163125_) -> {
         pTag.put("pool_element", p_163125_);
      });
      pTag.putString("rotation", this.rotation.name());
      ListTag listtag = new ListTag();

      for(JigsawJunction jigsawjunction : this.junctions) {
         listtag.add(jigsawjunction.serialize(registrywriteops).getValue());
      }

      pTag.put("junctions", listtag);
   }

   public void postProcess(WorldGenLevel pLevel, StructureFeatureManager pStructureFeatureManager, ChunkGenerator pChunkGenerator, Random pRandom, BoundingBox pBox, ChunkPos pChunkPos, BlockPos pPos) {
      this.place(pLevel, pStructureFeatureManager, pChunkGenerator, pRandom, pBox, pPos, false);
   }

   public void place(WorldGenLevel pLevel, StructureFeatureManager pStructureFeatureManager, ChunkGenerator pChunkGenerator, Random pRandom, BoundingBox pBox, BlockPos pPos, boolean pKeepJigsaws) {
      this.element.place(this.structureManager, pLevel, pStructureFeatureManager, pChunkGenerator, this.position, pPos, this.rotation, pBox, pRandom, pKeepJigsaws);
   }

   public void move(int pX, int pY, int pZ) {
      super.move(pX, pY, pZ);
      this.position = this.position.offset(pX, pY, pZ);
   }

   public Rotation getRotation() {
      return this.rotation;
   }

   public String toString() {
      return String.format("<%s | %s | %s | %s>", this.getClass().getSimpleName(), this.position, this.rotation, this.element);
   }

   public StructurePoolElement getElement() {
      return this.element;
   }

   public BlockPos getPosition() {
      return this.position;
   }

   public int getGroundLevelDelta() {
      return this.groundLevelDelta;
   }

   public void addJunction(JigsawJunction pJunction) {
      this.junctions.add(pJunction);
   }

   public List<JigsawJunction> getJunctions() {
      return this.junctions;
   }
}