package net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.SerializationTags;
import net.minecraft.tags.Tag;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

public class FluidPredicate {
   public static final FluidPredicate ANY = new FluidPredicate((Tag<Fluid>)null, (Fluid)null, StatePropertiesPredicate.ANY);
   @Nullable
   private final Tag<Fluid> tag;
   @Nullable
   private final Fluid fluid;
   private final StatePropertiesPredicate properties;

   public FluidPredicate(@Nullable Tag<Fluid> pTag, @Nullable Fluid pFluid, StatePropertiesPredicate pProperties) {
      this.tag = pTag;
      this.fluid = pFluid;
      this.properties = pProperties;
   }

   public boolean matches(ServerLevel pLevel, BlockPos pPos) {
      if (this == ANY) {
         return true;
      } else if (!pLevel.isLoaded(pPos)) {
         return false;
      } else {
         FluidState fluidstate = pLevel.getFluidState(pPos);
         Fluid fluid = fluidstate.getType();
         if (this.tag != null && !fluid.is(this.tag)) {
            return false;
         } else if (this.fluid != null && fluid != this.fluid) {
            return false;
         } else {
            return this.properties.matches(fluidstate);
         }
      }
   }

   public static FluidPredicate fromJson(@Nullable JsonElement pJson) {
      if (pJson != null && !pJson.isJsonNull()) {
         JsonObject jsonobject = GsonHelper.convertToJsonObject(pJson, "fluid");
         Fluid fluid = null;
         if (jsonobject.has("fluid")) {
            ResourceLocation resourcelocation = new ResourceLocation(GsonHelper.getAsString(jsonobject, "fluid"));
            fluid = Registry.FLUID.get(resourcelocation);
         }

         Tag<Fluid> tag = null;
         if (jsonobject.has("tag")) {
            ResourceLocation resourcelocation1 = new ResourceLocation(GsonHelper.getAsString(jsonobject, "tag"));
            tag = SerializationTags.getInstance().getTagOrThrow(Registry.FLUID_REGISTRY, resourcelocation1, (p_151160_) -> {
               return new JsonSyntaxException("Unknown fluid tag '" + p_151160_ + "'");
            });
         }

         StatePropertiesPredicate statepropertiespredicate = StatePropertiesPredicate.fromJson(jsonobject.get("state"));
         return new FluidPredicate(tag, fluid, statepropertiespredicate);
      } else {
         return ANY;
      }
   }

   public JsonElement serializeToJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonobject = new JsonObject();
         if (this.fluid != null) {
            jsonobject.addProperty("fluid", Registry.FLUID.getKey(this.fluid).toString());
         }

         if (this.tag != null) {
            jsonobject.addProperty("tag", SerializationTags.getInstance().getIdOrThrow(Registry.FLUID_REGISTRY, this.tag, () -> {
               return new IllegalStateException("Unknown fluid tag");
            }).toString());
         }

         jsonobject.add("state", this.properties.serializeToJson());
         return jsonobject;
      }
   }

   public static class Builder {
      @Nullable
      private Fluid fluid;
      @Nullable
      private Tag<Fluid> fluids;
      private StatePropertiesPredicate properties = StatePropertiesPredicate.ANY;

      private Builder() {
      }

      public static FluidPredicate.Builder fluid() {
         return new FluidPredicate.Builder();
      }

      public FluidPredicate.Builder of(Fluid pFluid) {
         this.fluid = pFluid;
         return this;
      }

      public FluidPredicate.Builder of(Tag<Fluid> pFluids) {
         this.fluids = pFluids;
         return this;
      }

      public FluidPredicate.Builder setProperties(StatePropertiesPredicate pProperties) {
         this.properties = pProperties;
         return this;
      }

      public FluidPredicate build() {
         return new FluidPredicate(this.fluids, this.fluid, this.properties);
      }
   }
}