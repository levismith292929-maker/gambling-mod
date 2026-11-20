package com.slotmachine.mod.block;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** The weighted outcomes shown on the reels. */
public enum ReelSymbol {
    DIRT(Items.DIRT), COPPER(Items.COPPER_INGOT), IRON(Items.IRON_INGOT),
    GOLD(Items.GOLD_INGOT), DIAMOND(Items.DIAMOND), EMERALD(Items.EMERALD),
    COMMAND_BLOCK(Items.COMMAND_BLOCK);

    private static final double[] BASE_CHANCES = {5.0, 40.99, 30.0, 15.0, 7.0, 2.0, 0.01};
    private static final int[] SPREAD_WEIGHTS = {10, 5, 3, 1};
    public final Item item;
    ReelSymbol(Item item) { this.item = item; }

    public static ReelSymbol roll(RandomSource random) { return roll(random, 0); }

    public static ReelSymbol roll(RandomSource random, int charmTier) {
        double[] chances = BASE_CHANCES.clone();
        int luck = switch (charmTier) { case 1 -> 10; case 2 -> 15; case 3 -> 20; case 4 -> 40; default -> 0; };
        if (luck > 0) {
            int spread = Math.round(luck / 3.0f);
            double moved = spread / 2.0;
            int unlocked = Math.min(4, charmTier + 1);
            int totalWeight = 0;
            for (int i = 0; i < unlocked; i++) totalWeight += SPREAD_WEIGHTS[i];
            chances[1] -= moved;
            for (int i = 0; i < unlocked; i++) {
                chances[i + 2] += moved * SPREAD_WEIGHTS[i] / totalWeight;
            }
        }
        double roll = random.nextDouble() * 100.0;
        double cumulative = 0.0;
        for (int i = 0; i < values().length; i++) {
            cumulative += chances[i];
            if (roll < cumulative) return values()[i];
        }
        return COPPER;
    }
}
