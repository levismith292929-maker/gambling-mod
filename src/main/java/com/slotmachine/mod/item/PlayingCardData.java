package com.slotmachine.mod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Creates an individually identified playing card with its own artwork and enchantment glint. */
public final class PlayingCardData {
    private static final String[] SUITS = {"clubs", "diamonds", "hearts", "spades"};
    private static final String[] RANKS = {
            "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "jack", "queen", "king", "ace"
    };

    private PlayingCardData() {}

    public static ItemStack create(int cardIndex) {
        int safeIndex = Math.floorMod(cardIndex, 52);
        String suit = SUITS[safeIndex / 13];
        String rank = RANKS[safeIndex % 13];
        String cardName = rank + "_of_" + suit;

        ItemStack card = new ItemStack(ModItems.PLAYING_CARD);
        card.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("slotmachine", "cards/" + cardName));
        card.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("CardIndex", safeIndex);
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return card;
    }
}
