package net.minecraft.world.entity.npc;

import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class VillagerProfession extends net.minecraftforge.registries.ForgeRegistryEntry<VillagerProfession> {
   public static final VillagerProfession NONE = register("none", PoiType.UNEMPLOYED, (SoundEvent)null);
   public static final VillagerProfession ARMORER = register("armorer", PoiType.ARMORER, SoundEvents.VILLAGER_WORK_ARMORER);
   public static final VillagerProfession BUTCHER = register("butcher", PoiType.BUTCHER, SoundEvents.VILLAGER_WORK_BUTCHER);
   public static final VillagerProfession CARTOGRAPHER = register("cartographer", PoiType.CARTOGRAPHER, SoundEvents.VILLAGER_WORK_CARTOGRAPHER);
   public static final VillagerProfession CLERIC = register("cleric", PoiType.CLERIC, SoundEvents.VILLAGER_WORK_CLERIC);
   public static final VillagerProfession FARMER = register("farmer", PoiType.FARMER, ImmutableSet.of(Items.WHEAT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.BONE_MEAL), ImmutableSet.of(Blocks.FARMLAND), SoundEvents.VILLAGER_WORK_FARMER);
   public static final VillagerProfession FISHERMAN = register("fisherman", PoiType.FISHERMAN, SoundEvents.VILLAGER_WORK_FISHERMAN);
   public static final VillagerProfession FLETCHER = register("fletcher", PoiType.FLETCHER, SoundEvents.VILLAGER_WORK_FLETCHER);
   public static final VillagerProfession LEATHERWORKER = register("leatherworker", PoiType.LEATHERWORKER, SoundEvents.VILLAGER_WORK_LEATHERWORKER);
   public static final VillagerProfession LIBRARIAN = register("librarian", PoiType.LIBRARIAN, SoundEvents.VILLAGER_WORK_LIBRARIAN);
   public static final VillagerProfession MASON = register("mason", PoiType.MASON, SoundEvents.VILLAGER_WORK_MASON);
   public static final VillagerProfession NITWIT = register("nitwit", PoiType.NITWIT, (SoundEvent)null);
   public static final VillagerProfession SHEPHERD = register("shepherd", PoiType.SHEPHERD, SoundEvents.VILLAGER_WORK_SHEPHERD);
   public static final VillagerProfession TOOLSMITH = register("toolsmith", PoiType.TOOLSMITH, SoundEvents.VILLAGER_WORK_TOOLSMITH);
   public static final VillagerProfession WEAPONSMITH = register("weaponsmith", PoiType.WEAPONSMITH, SoundEvents.VILLAGER_WORK_WEAPONSMITH);
   private final String name;
   private final PoiType jobPoiType;
   /** Defines items villagers of this profession can pick up and use. */
   private final ImmutableSet<Item> requestedItems;
   /** World blocks this profession interracts with. */
   private final ImmutableSet<Block> secondaryPoi;
   @Nullable
   private final SoundEvent workSound;

   public VillagerProfession(String p_35607_, PoiType p_35608_, ImmutableSet<Item> p_35609_, ImmutableSet<Block> p_35610_, @Nullable SoundEvent p_35611_) {
      this.name = p_35607_;
      this.jobPoiType = p_35608_;
      this.requestedItems = p_35609_;
      this.secondaryPoi = p_35610_;
      this.workSound = p_35611_;
   }

   public String getName() {
      return this.name;
   }

   public PoiType getJobPoiType() {
      return this.jobPoiType;
   }

   /**
    * @return A shared static immutable set of the specific items this profession can handle.
    */
   public ImmutableSet<Item> getRequestedItems() {
      return this.requestedItems;
   }

   /**
    * @return A shared static immutable set of the world blocks this profession interracts with beside job site block.
    */
   public ImmutableSet<Block> getSecondaryPoi() {
      return this.secondaryPoi;
   }

   @Nullable
   public SoundEvent getWorkSound() {
      return this.workSound;
   }

   public String toString() {
      return this.name;
   }

   static VillagerProfession register(String pName, PoiType pPointOfInterest, @Nullable SoundEvent pSound) {
      return register(pName, pPointOfInterest, ImmutableSet.of(), ImmutableSet.of(), pSound);
   }

   static VillagerProfession register(String pName, PoiType pPointOfInterest, ImmutableSet<Item> pSpecificItems, ImmutableSet<Block> pRelatedLevelBlocks, @Nullable SoundEvent pSound) {
      return Registry.register(Registry.VILLAGER_PROFESSION, new ResourceLocation(pName), new VillagerProfession(pName, pPointOfInterest, pSpecificItems, pRelatedLevelBlocks, pSound));
   }
}
