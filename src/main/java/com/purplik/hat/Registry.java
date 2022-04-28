package com.purplik.hat;

import com.purplik.hat.item.rare.Labcoat;
import com.purplik.hat.item.rare.Labgoggles;
import com.purplik.hat.item.villagerhats.*;
import com.purplik.hat.item.legendary.Tophat;
import com.purplik.hat.item.legendary.Ushanka;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class Registry {

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Hat.MOD_ID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Hat.MOD_ID);

    //////////////////////////////

    //Experimental for now

    //LEGENDARY HATS

    public static final RegistryObject<Item> USHANKA = ITEMS.register("ushanka", () -> new Ushanka(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> TOPHAT = ITEMS.register("tophat", () -> new Tophat(new Item.Properties().rarity(Rarity.EPIC)));



    //RARE HATS

    public static final RegistryObject<Item> LAB_COAT = ITEMS.register("lab_coat", () -> new Labcoat(new Item.Properties().rarity(Rarity.RARE)));
    public static final RegistryObject<Item> LAB_GOGGLES = ITEMS.register("lab_goggles", () -> new Labgoggles(new Item.Properties().rarity(Rarity.RARE)));

    //Ideas: Some glasses/goggles?
    //Definitely TF2 Hats


    //Pirate hat - For Dreadp1r4te#1324
    //Barnstormer - TF2 Hat - For Dreadp1r4te#1324



    //UNCOMMON HATS

    //Ideas: Villager hats

    public static final RegistryObject<Item> ARMORERHAT = ITEMS.register("armorerhat", () -> new ArmorerHat(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> BUTCHERHAT = ITEMS.register("butcherhat", () -> new ButcherHat(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FARMERHAT = ITEMS.register("farmerhat", () -> new FarmerHat(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FISHERHAT = ITEMS.register("fisherhat", () -> new FisherHat(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> FLETCHERHAT = ITEMS.register("fletcherhat", () -> new FletcherHat(new Item.Properties().rarity(Rarity.UNCOMMON)));


    //////////////////////////////

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
        BLOCKS.register(eventBus);
    }
}
