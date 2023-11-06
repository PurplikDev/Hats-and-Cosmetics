package com.purplik.hat.util;

import com.purplik.hat.Hat;
import com.purplik.hat.Registry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.common.BasicItemListing;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = Hat.MOD_ID)

public class ModEvents {

    @SubscribeEvent
    public static void addCustomTrades(VillagerTradesEvent event) {
        if(event.getType() == VillagerProfession.ARMORER) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            ItemStack stack = new ItemStack(Registry.ARMORERHAT.get(), 1);

            trades.get(5).add((pTrader, pRand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 32),
                    stack,1,3,0.2F
            ));
        }
        if(event.getType() == VillagerProfession.BUTCHER) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            ItemStack stack = new ItemStack(Registry.BUTCHERHAT.get(), 1);

            trades.get(5).add((pTrader, pRand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 32),
                    stack,1,3,0.2F
            ));
        }
        if(event.getType() == VillagerProfession.FARMER) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            ItemStack stack = new ItemStack(Registry.FARMERHAT.get(), 1);

            trades.get(5).add((pTrader, pRand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 32),
                    stack,1,3,0.2F
            ));
        }
        if(event.getType() == VillagerProfession.FISHERMAN) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            ItemStack stack = new ItemStack(Registry.FISHERHAT.get(), 1);

            trades.get(5).add((pTrader, pRand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 32),
                    stack,1,3,0.2F
            ));
        }
        if(event.getType() == VillagerProfession.FLETCHER) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            ItemStack stack = new ItemStack(Registry.FLETCHERHAT.get(), 1);

            trades.get(5).add((pTrader, pRand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 32),
                    stack,1,3,0.2F
            ));
        }
        if(event.getType() == VillagerProfession.CARTOGRAPHER) {
            Int2ObjectMap<List<VillagerTrades.ItemListing>> trades = event.getTrades();
            ItemStack stack = new ItemStack(Registry.MONOCLE.get(), 1);

            trades.get(5).add((pTrader, pRand) -> new MerchantOffer(
                    new ItemStack(Items.EMERALD, 32),
                    stack,1,3,0.2F
            ));
        }
    }

    @SubscribeEvent
    public static void registerWanderingTraderTrades(WandererTradesEvent event) {

        event.getGenericTrades().add(new BasicItemListing(8, new ItemStack(Registry.ENGINEERS_HAT.get(), 1), 1, 1, 1));

        event.getGenericTrades().add(new BasicItemListing(16, new ItemStack(Registry.BANDITS_HAT.get(), 1), 1, 1, 1));
        event.getGenericTrades().add(new BasicItemListing(16, new ItemStack(Registry.GOGGLES_OF_THAUMATURGY.get(), 1), 1, 1, 1));
        event.getGenericTrades().add(new BasicItemListing(16, new ItemStack(Registry.GOGGLES_OF_THAUMATURGY_STYLE_2.get(), 1), 1, 1, 1));
        event.getGenericTrades().add(new BasicItemListing(16, new ItemStack(Registry.LABCOAT.get(), 1), 1, 1, 1));
        event.getGenericTrades().add(new BasicItemListing(16, new ItemStack(Registry.LAB_GOGGLES.get(), 1), 1, 1, 1));
        event.getGenericTrades().add(new BasicItemListing(16, new ItemStack(Registry.BEANIE_POOF.get(), 1), 1, 1, 1));

        event.getRareTrades().add(new BasicItemListing(32, new ItemStack(Registry.GUP.get(), 1), 1, 1, 1));
        event.getRareTrades().add(new BasicItemListing(32, new ItemStack(Registry.TOPHAT.get(), 1), 1, 1, 1));
        event.getRareTrades().add(new BasicItemListing(32, new ItemStack(Registry.USHANKA.get(), 1), 1, 1, 1));

        event.getRareTrades().add(new BasicItemListing(32, new ItemStack(Registry.RAT.get(), 1), 1, 1, 1));
        event.getRareTrades().add(new BasicItemListing(32, new ItemStack(Registry.PLAGUE_HAT.get(), 1), 1, 1, 1));
        event.getRareTrades().add(new BasicItemListing(32, new ItemStack(Registry.PLAGUE_MASK.get(), 1), 1, 1, 1));
    }
}
