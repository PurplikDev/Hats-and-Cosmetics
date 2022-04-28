package com.purplik.hat.util;

import com.purplik.hat.Hat;
import com.purplik.hat.Registry;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
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
    }
}
