package com.slotmachine.mod.screen;

import com.slotmachine.mod.block.SlotMachineBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Reuses GenericContainerScreenHandler (and therefore vanilla's chest screen
 * rendering) for the 3x9 "Slot Machine" GUI, then swaps every slot except the
 * payment slot for a LockedSlot so items can't be pulled out of or dropped
 * into the reel/Spin display slots.
 *
 * Clicking the Spin slot doesn't do anything yet beyond being un-interactive
 * — the actual spin trigger comes with the RNG/payout pass.
 */
public class SlotMachineScreenHandler extends GenericContainerScreenHandler {

    // Client-side: the server tells the client to open this type, and the client
    // builds a throwaway inventory just to construct matching slots; real
    // contents arrive via the normal slot-sync packets.
    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(SlotMachineBlockEntity.SIZE));
    }

    // Server-side: called from SlotMachineBlockEntity#createMenu with the real inventory.
    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ModScreenHandlers.SLOT_MACHINE_SCREEN_HANDLER, syncId, playerInventory, inventory, 3);
        lockDecorativeSlots(inventory);
    }

    private void lockDecorativeSlots(Inventory inventory) {
        for (int index = 0; index < SlotMachineBlockEntity.SIZE; index++) {
            if (index == SlotMachineBlockEntity.PAYMENT_SLOT) {
                continue;
            }
            Slot original = this.slots.get(index);
            this.slots.set(index, new LockedSlot(inventory, index, original.x, original.y));
        }
    }

    /** A slot that never allows inserting or taking items — purely visual. */
    private static class LockedSlot extends Slot {
        LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return false;
        }
    }
}
