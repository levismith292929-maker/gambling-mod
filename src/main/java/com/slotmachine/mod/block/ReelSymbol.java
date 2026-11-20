package com.slotmachine.mod.block;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
/** The weighted outcomes shown on the reels. */
public enum ReelSymbol {
    DIRT(Items.DIRT, 500),
    COPPER(Items.COPPER_INGOT, 5999),
    IRON(Items.IRON_INGOT, 2000),
    GOLD(Items.GOLD_INGOT, 1000),
    DIAMOND(Items.DIAMOND, 400),
    EMERALD(Items.EMERALD, 100),
    COMMAND_BLOCK(Items.COMMAND_BLOCK, 1);
    private static final int TOTAL_WEIGHT = 10000;
    public final Item item;
    public final int weight;
    ReelSymbol(Item item, int weight) { this.item = item; this.weight = weight; }
    public static ReelSymbol roll(RandomSource random) {
        int roll = random.nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (ReelSymbol symbol : values()) {
            cumulative += symbol.weight;
            if (roll < cumulative) return symbol;
        }
        return COPPER;
    }
}
