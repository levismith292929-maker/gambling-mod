package com.slotmachine.mod.menu;

import com.slotmachine.mod.block.SimpleBlockContainer;
import com.slotmachine.mod.block.SlotMachineBlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 3x9 "Slot Machine" GUI built on vanilla's ChestMenu. Every slot of the machine's own
 * container except the payment slot is replaced with a LockedSlot (nothing can be put in
 * or taken out), and clicking the Spin slot triggers a spin on the real block entity.
 */
public class SlotMachineMenu extends ChestMenu {

    // Null on the client-side dummy instance.
    private final SlotMachineBlockEntity blockEntity;

    /** Client-side: built when the server tells the client to open this menu. */
    public SlotMachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory,
                new SimpleBlockContainer(SlotMachineBlockEntity.SIZE, () -> {}, player -> true), null);
    }

    /** Server-side: called from SlotMachineBlockEntity#createMenu with the real container. */
    public SlotMachineMenu(int containerId, Inventory playerInventory, Container container,
                           SlotMachineBlockEntity blockEntity) {
        super(ModMenus.SLOT_MACHINE, containerId, playerInventory, container, 3);
        this.blockEntity = blockEntity;
    }

    // Called by the super constructor while it builds the slots (before our fields are set),
    // so this must not touch instance fields.
    @Override
    protected Slot addSlot(Slot slot) {
        if (slot.container instanceof SimpleBlockContainer
                && slot.getContainerSlot() != SlotMachineBlockEntity.PAYMENT_SLOT) {
            slot = new LockedSlot(slot.container, slot.getContainerSlot(), slot.x, slot.y);
        }
        return super.addSlot(slot);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId == SlotMachineBlockEntity.SPIN_SLOT) {
            if (!player.level().isClientSide() && blockEntity != null) {
                blockEntity.trySpin(player);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // Shift-click must not pull the decorative reel/spin items out of the machine.
        if (slotIndex < SlotMachineBlockEntity.SIZE && slotIndex != SlotMachineBlockEntity.PAYMENT_SLOT) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, slotIndex);
    }

    /** A slot that never allows inserting or taking items - purely visual. */
    private static class LockedSlot extends Slot {
        LockedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public int getMaxStackSize() {
            // Stops shift-click from merging extra items into the displayed stack.
            return 1;
        }
    }
}
