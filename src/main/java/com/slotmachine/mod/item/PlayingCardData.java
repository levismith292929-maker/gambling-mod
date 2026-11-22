package com.slotmachine.mod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Creates individually identified cards for the deck and printer. */
public final class PlayingCardData {
    private static final String[] SUITS = {"clubs", "diamonds", "hearts", "spades"};
    private static final String[] RANKS = {
            "2", "3", "4", "5", "6", "7", "8", "9", "10",
            "jack", "queen", "king", "ace"
    };

    private PlayingCardData() {}

    /** A deck-drawn card has its face artwork and no enchantment glint. */
    public static ItemStack create(int cardIndex) {
        int safeIndex = Math.floorMod(cardIndex, 54);
        String cardName;
        if (safeIndex == 52) {
            cardName = "joker_red";
        } else if (safeIndex == 53) {
            cardName = "joker_black";
        } else {
            cardName = RANKS[safeIndex % 13] + "_of_" + SUITS[safeIndex / 13];
        }
        ItemStack card = new ItemStack(ModItems.PLAYING_CARD);
        card.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("slotmachine", "cards/" + cardName));
        saveIndex(card, safeIndex);
        return card;
    }

    /** A printer-made card looks like a yellow blank card and has an enchantment glint. */
    public static ItemStack createPrinted() {
        ItemStack card = new ItemStack(ModItems.PLAYING_CARD);
        card.set(DataComponents.ITEM_MODEL,
                Identifier.fromNamespaceAndPath("slotmachine", "printed_card"));
        card.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        saveIndex(card, -1);
        return card;
    }

    private static void saveIndex(ItemStack card, int index) {
        CompoundTag tag = card.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt("CardIndex", index);
        card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }
}
