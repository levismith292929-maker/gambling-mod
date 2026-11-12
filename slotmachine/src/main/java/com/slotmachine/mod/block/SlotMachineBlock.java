package com.slotmachine.mod.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.event.GameEvent;

/**
 * Placeholder/test block for the slot machine: two blocks tall, and breaking
 * EITHER half removes the whole thing with a single item drop — unlike
 * vanilla doors, which only cascade one direction (bottom -> top).
 *
 * Right-click opens the Slot Machine GUI; shift-right-click opens the
 * separate Luck Slot GUI. Both delegate to the block entity attached to the
 * LOWER half, so clicking either half opens the same inventory.
 */
public class SlotMachineBlock extends Block implements BlockEntityProvider {

    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;

    public SlotMachineBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        if (pos.getY() < ctx.getWorld().getTopY() - 1
                && ctx.getWorld().getBlockState(pos.up()).canReplace(ctx)) {
            return getDefaultState().with(HALF, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                 WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (direction.getAxis() == Direction.Axis.Y) {
            DoubleBlockHalf half = state.get(HALF);
            boolean neighborIsPartner = neighborState.isOf(this) && neighborState.get(HALF) != half;
            if (!neighborIsPartner) {
                return Blocks.AIR.getDefaultState();
            }
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient) {
            DoubleBlockHalf half = state.get(HALF);
            BlockPos otherPos = half == DoubleBlockHalf.LOWER ? pos.up() : pos.down();
            BlockState otherState = world.getBlockState(otherPos);
            if (otherState.isOf(this) && otherState.get(HALF) != half) {
                // Remove the partner half directly (no loot run) so exactly
                // one item still drops, from whichever half was actually harvested.
                world.setBlockState(otherPos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.emitGameEvent(player, GameEvent.BLOCK_DESTROY, otherPos);
            }
        }
        super.onBreak(world, pos, state, player);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        // Only the lower half carries the inventory/GUI; the upper half just redirects to it.
        if (state.get(HALF) == DoubleBlockHalf.LOWER) {
            return new SlotMachineBlockEntity(pos, state);
        }
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockPos lowerPos = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        BlockEntity blockEntity = world.getBlockEntity(lowerPos);
        if (blockEntity instanceof SlotMachineBlockEntity slotMachine) {
            if (player.isSneaking()) {
                player.openHandledScreen(slotMachine.getLuckScreenHandlerFactory());
            } else {
                player.openHandledScreen(slotMachine);
            }
        }
        return ActionResult.CONSUME;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient || state.get(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return (tickWorld, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof SlotMachineBlockEntity slotMachine) {
                SlotMachineBlockEntity.tick(tickWorld, pos, tickState, slotMachine);
            }
        };
    }
}
