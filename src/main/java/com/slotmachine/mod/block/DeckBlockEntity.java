package com.slotmachine.mod.block;

import com.slotmachine.mod.item.PlayingCardData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Stores an independently shuffled, persistent 52-card order for one placed deck. */
public class DeckBlockEntity extends BlockEntity {
    private static final int CARD_COUNT = 54;
    private static final String ORDER_PREFIX = "Card";
    private final int[] order = new int[CARD_COUNT];
    private int nextCard = 0;
    private boolean shuffled = false;

    public DeckBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECK_BLOCK_ENTITY, pos, state);
    }

    private void createShuffle() {
        List<Integer> cards = new ArrayList<>(CARD_COUNT);
        for (int i = 0; i < CARD_COUNT; i++) cards.add(i);
        Collections.shuffle(cards, new Random());
        for (int i = 0; i < CARD_COUNT; i++) order[i] = cards.get(i);
        nextCard = 0;
        shuffled = true;
        setChanged();
    }

    public ItemStack drawCard() {
        if (!shuffled) createShuffle();
        if (nextCard >= CARD_COUNT) return ItemStack.EMPTY;

        int cardIndex = order[nextCard++];
        setChanged();
        return PlayingCardData.create(cardIndex);
    }

    public boolean hasCardsRemaining() {
        return !shuffled || nextCard < CARD_COUNT;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Shuffled", shuffled);
        output.putInt("NextCard", nextCard);
        for (int i = 0; i < CARD_COUNT; i++) {
            output.putInt(ORDER_PREFIX + i, order[i]);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        shuffled = input.getBooleanOr("Shuffled", false);
        nextCard = Math.max(0, Math.min(CARD_COUNT, input.getIntOr("NextCard", 0)));
        boolean[] seen = new boolean[CARD_COUNT];
        boolean valid = true;
        for (int i = 0; i < CARD_COUNT; i++) {
            order[i] = input.getIntOr(ORDER_PREFIX + i, -1);
            if (order[i] < 0 || order[i] >= CARD_COUNT || seen[order[i]]) {
                valid = false;
            } else {
                seen[order[i]] = true;
            }
        }
        if (!valid) {
            shuffled = false;
            nextCard = 0;
        }
    }
}
