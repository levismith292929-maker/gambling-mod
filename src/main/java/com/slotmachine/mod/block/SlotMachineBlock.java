package com.slotmachine.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Placeholder/test block for the slot machine: two blocks tall, and breaking EITHER half
 * removes the whole thing with a single item drop.
 *
 * Right-click opens the Slot Machine GUI; shift-right-click (empty hand) opens the separate
 * Luck Slot GUI. Both go through the block entity on the LOWER half.
 */
public class SlotMachineBlock extends Block implements EntityBlock {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public SlotMachineBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (pos.getY() < level.getMaxY() && level.getBlockState(pos.above()).canBeReplaced(context)) {
            return defaultBlockState();
        }
        return null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        // Only the vertical neighbour that should be our other half matters: the lower half
        // looks up, the upper half looks down.
        if (directionToNeighbour.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (directionToNeighbour == Direction.UP)) {
            boolean partnerPresent = neighbourState.is(this) && neighbourState.getValue(HALF) != half;
            if (!partnerPresent) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos lowerPos = half == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos.below();

            // Don't lose a prize/payment (or luck items) sitting inside the machine.
            if (level.getBlockEntity(lowerPos) instanceof SlotMachineBlockEntity slotMachine) {
                slotMachine.dropContents(level, lowerPos);
            }

            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.is(this) && otherState.getValue(HALF) != half) {
                // Remove the partner half without drops so exactly one item drops,
                // from whichever half was actually harvested.
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(),
                        Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // Only the lower half carries the inventory; the upper half just redirects to it.
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return new SlotMachineBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockPos lowerPos = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        if (level.getBlockEntity(lowerPos) instanceof SlotMachineBlockEntity slotMachine) {
            if (player.isShiftKeyDown()) {
                player.openMenu(slotMachine.getLuckMenuProvider());
            } else {
                player.openMenu(slotMachine);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.LOWER
                || type != ModBlockEntities.SLOT_MACHINE_BLOCK_ENTITY) {
            return null;
        }
        return (tickLevel, tickPos, tickState, blockEntity) -> {
            if (blockEntity instanceof SlotMachineBlockEntity slotMachine) {
                SlotMachineBlockEntity.tick(tickLevel, tickPos, tickState, slotMachine);
            }
        };
    }
}
