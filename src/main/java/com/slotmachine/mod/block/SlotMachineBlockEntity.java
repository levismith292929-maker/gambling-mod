package com.slotmachine.mod.block;

import com.slotmachine.mod.screen.ModScreenHandlers;
import com.slotmachine.mod.screen.SlotMachineScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Holds two separate 27-slot inventories:
 *  - the slot machine's own GUI (payment slot + decorative reel/spin slots)
 *  - the "Luck Slot" inventory, opened with a separate shift-right-click GUI
 *
 * Spin logic / payouts aren't wired up yet — right now this just gets both
 * GUIs opening and the payment slot functional, with everything else in the
 * slot machine GUI locked down per "nothing can be taken except the prize
 * winnings and the payment".
 */
public class SlotMachineBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    public static final int SIZE = 27;
    public static final int PAYMENT_SLOT = 10; // row 2, col 2 (0-indexed) in the screenshot
    public static final int[] REEL_SLOTS = {12, 13, 14};
    public static final int SPIN_SLOT = 16;

    private final SimpleBlockInventory mainInventory = new SimpleBlockInventory(SIZE, this::markDirty);
    private final SimpleBlockInventory luckInventory = new SimpleBlockInventory(SIZE, this::markDirty);

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SLOT_MACHINE_BLOCK_ENTITY, pos, state);
        resetDecoration();
    }

    /** Puts the fixed, non-removable display items (reels + Spin button) back in place. */
    private void resetDecoration() {
        for (int reelSlot : REEL_SLOTS) {
            mainInventory.setStack(reelSlot, new ItemStack(Items.DIAMOND_BLOCK));
        }
        mainInventory.setStack(SPIN_SLOT, new ItemStack(Items.EMERALD_BLOCK));
    }

    public Inventory getMainInventory() {
        return mainInventory;
    }

    public Inventory getLuckInventory() {
        return luckInventory;
    }

    @Override
    public Text getDisplayName() {
        return Text.literal("Slot Machine");
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SlotMachineScreenHandler(syncId, playerInventory, mainInventory);
    }

    /** Separate factory for the shift-right-click "Luck Slot" GUI — plain, unrestricted 27 slots. */
    public NamedScreenHandlerFactory getLuckScreenHandlerFactory() {
        return new NamedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("Luck Slot");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return GenericContainerScreenHandler.createGeneric9x3(syncId, playerInventory, luckInventory);
            }
        };
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.put("MainInventory", Inventories.writeNbt(new NbtCompound(), mainInventory.getItems(), registryLookup));
        nbt.put("LuckInventory", Inventories.writeNbt(new NbtCompound(), luckInventory.getItems(), registryLookup));
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        Inventories.readNbt(nbt.getCompound("MainInventory"), mainInventory.getItems(), registryLookup);
        Inventories.readNbt(nbt.getCompound("LuckInventory"), luckInventory.getItems(), registryLookup);
    }
}
