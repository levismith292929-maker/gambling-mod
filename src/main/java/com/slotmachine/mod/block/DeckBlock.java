package com.slotmachine.mod.block;

import com.slotmachine.mod.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A sealed deck that opens into 52 playing cards when right-clicked. */
public class DeckBlock extends Block {
    public DeckBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private InteractionResult openDeck(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack cards = new ItemStack(ModItems.PLAYING_CARD, 52);
        player.getInventory().placeItemBackInInventory(cards);

        // Remove the placed deck without dropping another deck item.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
        return InteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return openDeck(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return openDeck(level, pos, player);
    }
}
