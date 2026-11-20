package com.slotmachine.mod.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The 7 possible reel outcomes, in spin order. Weights are out of 10000 so the 0.01%
 * command block chance can be a whole number (1/10000).
 */
public enum ReelSymbol {
    DIRT(Items.DIRT, 500),                // 5%
    COPPER(Items.COPPER_BLOCK, 5999),     // 59.99%
    IRON(Items.IRON_BLOCK, 2000),         // 20%
    GOLD(Items.GOLD_BLOCK, 1000),         // 10%
    DIAMOND(Items.DIAMOND_BLOCK, 400),    // 4%
    EMERALD(Items.EMERALD_BLOCK, 100),    // 1%
    COMMAND_BLOCK(Items.COMMAND_BLOCK, 1); // 0.01%

    private static final int TOTAL_WEIGHT = 10000;

    public final Item item;
    public final int weight;

    ReelSymbol(Item item, int weight) {
        this.item = item;
        this.weight = weight;
    }

    public static ReelSymbol roll(RandomSource random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (ReelSymbol symbol : values()) {
            cumulative += symbol.weight;
            if (roll < cumulative) {
                return symbol;
            }
        }
        return COPPER; // unreachable: the weights sum to TOTAL_WEIGHT
    }
}
