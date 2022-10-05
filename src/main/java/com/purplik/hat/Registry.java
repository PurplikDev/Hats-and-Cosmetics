package com.purplik.hat;

import com.purplik.hat.item.HatItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class Registry {

    public static final CreativeModeTab COSMETICS_TAB = new CreativeModeTab("cosmetics_tab") {
        @Override
        public ItemStack makeIcon() {
            return new ItemStack(LABCOAT.get());
        }
    };

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Hat.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Hat.MOD_ID);

    //////////////////////////////

    public static final RegistryObject<Item> USHANKA = ITEMS.register("ushanka", () -> new HatItem(new Item.Properties(), "ushanka"));
    public static final RegistryObject<Item> TOPHAT = ITEMS.register("tophat", () -> new HatItem(new Item.Properties(), "tophat"));
    public static final RegistryObject<Item> GUP = ITEMS.register("gup", () -> new HatItem(new Item.Properties(), "gup"));
    public static final RegistryObject<Item> LABCOAT = ITEMS.register("labcoat", () -> new HatItem(new Item.Properties(), "labcoat"));
    public static final RegistryObject<Item> LAB_GOGGLES = ITEMS.register("lab_goggles", () -> new HatItem(new Item.Properties(), "lab_goggles"));
    public static final RegistryObject<Item> GOGGLES_OF_THAUMATURGY = ITEMS.register("goggles_of_thaumaturgy", () -> new HatItem(new Item.Properties(), "goggles_of_thaumaturgy"));
    public static final RegistryObject<Item> GOGGLES_OF_THAUMATURGY_STYLE_2 = ITEMS.register("goggles_of_thaumaturgy_2", () -> new HatItem(new Item.Properties(), "goggles_of_thaumaturgy_2")); // I will figure out a way to combine these into one later
    public static final RegistryObject<Item> BANDITS_HAT = ITEMS.register("bandits_hat", () -> new HatItem(new Item.Properties(), "bandits_hat"));
    public static final RegistryObject<Item> ARMORERHAT = ITEMS.register("armorerhat", () -> new HatItem(new Item.Properties(), "armorerhat"));
    public static final RegistryObject<Item> BUTCHERHAT = ITEMS.register("butcherhat", () -> new HatItem(new Item.Properties(), "butcherhat"));
    public static final RegistryObject<Item> FARMERHAT = ITEMS.register("farmerhat", () -> new HatItem(new Item.Properties(), "farmerhat"));
    public static final RegistryObject<Item> FISHERHAT = ITEMS.register("fisherhat", () -> new HatItem(new Item.Properties(), "fisherhat"));
    public static final RegistryObject<Item> FLETCHERHAT = ITEMS.register("fletcherhat", () -> new HatItem(new Item.Properties(), "fletcherhat"));
    public static final RegistryObject<Item> ENGINEERS_HAT = ITEMS.register("engineers_hat", () -> new HatItem(new Item.Properties(), "engineers_hat"));
    public static final RegistryObject<Item> MONOCLE = ITEMS.register("monocle", () -> new HatItem(new Item.Properties(), "monocle"));
    public static final RegistryObject<Item> PLAGUE_MASK = ITEMS.register("plague_mask", () -> new HatItem(new Item.Properties(), "plague_mask"));
    public static final RegistryObject<Item> PLAGUE_HAT = ITEMS.register("plague_hat", () -> new HatItem(new Item.Properties(), "plague_hat"));
    public static final RegistryObject<Item> RAT = ITEMS.register("rat", () -> new HatItem(new Item.Properties(), "rat"));

    //////////////////////////////
}
