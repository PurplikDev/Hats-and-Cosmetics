package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Convert any empty maps into explorer maps that lead to a structure that is nearest to the current {@linkplain
 * LootContextParams.ORIGIN}, if present.
 */
public class ExplorationMapFunction extends LootItemConditionalFunction {
   static final Logger LOGGER = LogManager.getLogger();
   public static final StructureFeature<?> DEFAULT_FEATURE = StructureFeature.BURIED_TREASURE;
   public static final String DEFAULT_DECORATION_NAME = "mansion";
   public static final MapDecoration.Type DEFAULT_DECORATION = MapDecoration.Type.MANSION;
   public static final byte DEFAULT_ZOOM = 2;
   public static final int DEFAULT_SEARCH_RADIUS = 50;
   public static final boolean DEFAULT_SKIP_EXISTING = true;
   final StructureFeature<?> destination;
   final MapDecoration.Type mapDecoration;
   final byte zoom;
   final int searchRadius;
   final boolean skipKnownStructures;

   ExplorationMapFunction(LootItemCondition[] pConditions, StructureFeature<?> pDestination, MapDecoration.Type pMapDecoration, byte pZoom, int pSearchRadius, boolean pSkipKnownStructures) {
      super(pConditions);
      this.destination = pDestination;
      this.mapDecoration = pMapDecoration;
      this.zoom = pZoom;
      this.searchRadius = pSearchRadius;
      this.skipKnownStructures = pSkipKnownStructures;
   }

   public LootItemFunctionType getType() {
      return LootItemFunctions.EXPLORATION_MAP;
   }

   /**
    * Get the parameters used by this object.
    */
   public Set<LootContextParam<?>> getReferencedContextParams() {
      return ImmutableSet.of(LootContextParams.ORIGIN);
   }

   /**
    * Called to perform the actual action of this function, after conditions have been checked.
    */
   public ItemStack run(ItemStack pStack, LootContext pContext) {
      if (!pStack.is(Items.MAP)) {
         return pStack;
      } else {
         Vec3 vec3 = pContext.getParamOrNull(LootContextParams.ORIGIN);
         if (vec3 != null) {
            ServerLevel serverlevel = pContext.getLevel();
            BlockPos blockpos = serverlevel.findNearestMapFeature(this.destination, new BlockPos(vec3), this.searchRadius, this.skipKnownStructures);
            if (blockpos != null) {
               ItemStack itemstack = MapItem.create(serverlevel, blockpos.getX(), blockpos.getZ(), this.zoom, true, true);
               MapItem.renderBiomePreviewMap(serverlevel, itemstack);
               MapItemSavedData.addTargetDecoration(itemstack, blockpos, "+", this.mapDecoration);
               itemstack.setHoverName(new TranslatableComponent("filled_map." + this.destination.getFeatureName().toLowerCase(Locale.ROOT)));
               return itemstack;
            }
         }

         return pStack;
      }
   }

   public static ExplorationMapFunction.Builder makeExplorationMap() {
      return new ExplorationMapFunction.Builder();
   }

   public static class Builder extends LootItemConditionalFunction.Builder<ExplorationMapFunction.Builder> {
      private StructureFeature<?> destination = ExplorationMapFunction.DEFAULT_FEATURE;
      private MapDecoration.Type mapDecoration = ExplorationMapFunction.DEFAULT_DECORATION;
      private byte zoom = 2;
      private int searchRadius = 50;
      private boolean skipKnownStructures = true;

      protected ExplorationMapFunction.Builder getThis() {
         return this;
      }

      public ExplorationMapFunction.Builder setDestination(StructureFeature<?> p_80572_) {
         this.destination = p_80572_;
         return this;
      }

      public ExplorationMapFunction.Builder setMapDecoration(MapDecoration.Type p_80574_) {
         this.mapDecoration = p_80574_;
         return this;
      }

      public ExplorationMapFunction.Builder setZoom(byte p_80570_) {
         this.zoom = p_80570_;
         return this;
      }

      public ExplorationMapFunction.Builder setSearchRadius(int p_165206_) {
         this.searchRadius = p_165206_;
         return this;
      }

      public ExplorationMapFunction.Builder setSkipKnownStructures(boolean p_80576_) {
         this.skipKnownStructures = p_80576_;
         return this;
      }

      public LootItemFunction build() {
         return new ExplorationMapFunction(this.getConditions(), this.destination, this.mapDecoration, this.zoom, this.searchRadius, this.skipKnownStructures);
      }
   }

   public static class Serializer extends LootItemConditionalFunction.Serializer<ExplorationMapFunction> {
      /**
       * Serialize the value by putting its data into the JsonObject.
       */
      public void serialize(JsonObject pJson, ExplorationMapFunction pValue, JsonSerializationContext pSerializationContext) {
         super.serialize(pJson, pValue, pSerializationContext);
         if (!pValue.destination.equals(ExplorationMapFunction.DEFAULT_FEATURE)) {
            pJson.add("destination", pSerializationContext.serialize(pValue.destination.getFeatureName()));
         }

         if (pValue.mapDecoration != ExplorationMapFunction.DEFAULT_DECORATION) {
            pJson.add("decoration", pSerializationContext.serialize(pValue.mapDecoration.toString().toLowerCase(Locale.ROOT)));
         }

         if (pValue.zoom != 2) {
            pJson.addProperty("zoom", pValue.zoom);
         }

         if (pValue.searchRadius != 50) {
            pJson.addProperty("search_radius", pValue.searchRadius);
         }

         if (!pValue.skipKnownStructures) {
            pJson.addProperty("skip_existing_chunks", pValue.skipKnownStructures);
         }

      }

      public ExplorationMapFunction deserialize(JsonObject pObject, JsonDeserializationContext pDeserializationContext, LootItemCondition[] pConditions) {
         StructureFeature<?> structurefeature = readStructure(pObject);
         String s = pObject.has("decoration") ? GsonHelper.getAsString(pObject, "decoration") : "mansion";
         MapDecoration.Type mapdecoration$type = ExplorationMapFunction.DEFAULT_DECORATION;

         try {
            mapdecoration$type = MapDecoration.Type.valueOf(s.toUpperCase(Locale.ROOT));
         } catch (IllegalArgumentException illegalargumentexception) {
            ExplorationMapFunction.LOGGER.error("Error while parsing loot table decoration entry. Found {}. Defaulting to {}", s, ExplorationMapFunction.DEFAULT_DECORATION);
         }

         byte b0 = GsonHelper.getAsByte(pObject, "zoom", (byte)2);
         int i = GsonHelper.getAsInt(pObject, "search_radius", 50);
         boolean flag = GsonHelper.getAsBoolean(pObject, "skip_existing_chunks", true);
         return new ExplorationMapFunction(pConditions, structurefeature, mapdecoration$type, b0, i, flag);
      }

      private static StructureFeature<?> readStructure(JsonObject p_80581_) {
         if (p_80581_.has("destination")) {
            String s = GsonHelper.getAsString(p_80581_, "destination");
            StructureFeature<?> structurefeature = StructureFeature.STRUCTURES_REGISTRY.get(s.toLowerCase(Locale.ROOT));
            if (structurefeature != null) {
               return structurefeature;
            }
         }

         return ExplorationMapFunction.DEFAULT_FEATURE;
      }
   }
}