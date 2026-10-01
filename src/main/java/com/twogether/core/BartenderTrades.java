package com.twogether.core;

import net.minecraft.world.entity.npc.VillagerTrades.ItemListing;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;

import java.util.List;

/**
 * The bartender's trades, level by level: buys the raw sugar and grain, sells glasses and yeast,
 * then bottles, cocktails, and finally aged spirits and the cask to make them.
 */
public final class BartenderTrades {

    private static final float PRICE_MULTIPLIER = 0.05F;

    private BartenderTrades() {
    }

    static void onVillagerTrades(VillagerTradesEvent event) {
        if (event.getType() != TwoGetherCoreMod.BARTENDER.get()) return;
        var trades = event.getTrades();

        trades.get(1).add(buy(Items.SUGAR, 20, 16, 2));
        trades.get(1).add(sell(() -> StemmedGlassBlock.glassStack(Drink.EMPTY).copyWithCount(4), 1, 16, 1));

        trades.get(2).add(buy(Items.WHEAT, 20, 16, 10));
        trades.get(2).add(buy(Items.SWEET_BERRIES, 16, 16, 10));
        trades.get(2).add(sell(() -> new ItemStack(TwoGetherCoreMod.YEAST.get(), 4), 1, 12, 5));

        trades.get(3).add(sell(() -> new ItemStack(TwoGetherCoreMod.WHISKY_BOTTLE.get()), 4, 12, 10));
        trades.get(3).add(sell(() -> new ItemStack(TwoGetherCoreMod.VODKA_BOTTLE.get()), 4, 12, 10));
        trades.get(3).add(sell(() -> new ItemStack(TwoGetherCoreMod.RUM_BOTTLE.get()), 4, 12, 10));

        // One random cocktail each time the slot is rolled.
        List<Drink> cocktails = List.of(Drink.values()).stream().filter(d -> d != Drink.EMPTY && !d.isSpirit()).toList();
        trades.get(4).add((entity, random) -> offer(StemmedGlassBlock.glassStack(cocktails.get(random.nextInt(cocktails.size()))), 6, 12, 15));
        trades.get(4).add((entity, random) -> offer(StemmedGlassBlock.glassStack(cocktails.get(random.nextInt(cocktails.size()))), 6, 12, 15));

        trades.get(5).add(sell(() -> new ItemStack(TwoGetherCoreMod.AGED_WHISKY_BOTTLE.get()), 10, 6, 30));
        trades.get(5).add(sell(() -> new ItemStack(TwoGetherCoreMod.AGING_CASK_ITEM.get()), 12, 3, 30));
    }

    private static ItemListing buy(ItemLike item, int count, int maxUses, int xp) {
        return (entity, random) -> new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD), maxUses, xp, PRICE_MULTIPLIER);
    }

    private static ItemListing sell(java.util.function.Supplier<ItemStack> result, int emeralds, int maxUses, int xp) {
        return (entity, random) -> offer(result.get(), emeralds, maxUses, xp);
    }

    private static MerchantOffer offer(ItemStack result, int emeralds, int maxUses, int xp) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), result, maxUses, xp, PRICE_MULTIPLIER);
    }
}
