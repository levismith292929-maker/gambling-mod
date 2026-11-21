package com.slotmachine.mod.block;

import com.slotmachine.mod.menu.SuperCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A spruce-themed crafting table with a five-by-five crafting grid. */
public class SuperCraftingTableBlock extends Block {
    public SuperCraftingTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    private InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (!level.isClientSide()) {
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new SuperCraftingMenu(
                            containerId, inventory, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.slotmachine.super_crafting_table")
            ));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }
}
