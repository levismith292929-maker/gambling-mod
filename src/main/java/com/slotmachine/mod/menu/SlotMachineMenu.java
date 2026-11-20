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
import net.minecraft.world.item.Items;

/** Five-row slot-machine GUI with a dedicated payment input and prize output. */
public class SlotMachineMenu extends ChestMenu {

    private final SlotMachineBlockEntity blockEntity;

    public SlotMachineMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory,
                new SimpleBlockContainer(SlotMachineBlockEntity.SIZE, () -> {}, player -> true), null);
    }

    public SlotMachineMenu(int containerId, Inventory playerInventory, Container container,
                           SlotMachineBlockEntity blockEntity) {
        super(ModMenus.SLOT_MACHINE, containerId, playerInventory, container, 5);
        this.blockEntity = blockEntity;
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if (slot.container instanceof SimpleBlockContainer) {
            int index = slot.getContainerSlot();
            if (index == SlotMachineBlockEntity.PAYMENT_SLOT) {
                slot = new PaymentSlot(slot.container, index, slot.x, slot.y);
            } else if (index == SlotMachineBlockEntity.PRIZE_SLOT) {
                slot = new PrizeSlot(slot.container, index, slot.x, slot.y);
            } else {
                slot = new LockedSlot(slot.container, index, slot.x, slot.y);
            }
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
        if (slotIndex < SlotMachineBlockEntity.SIZE
                && slotIndex != SlotMachineBlockEntity.PAYMENT_SLOT
                && slotIndex != SlotMachineBlockEntity.PRIZE_SLOT) {
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, slotIndex);
    }

    private static class LockedSlot extends Slot {
        LockedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
        @Override public int getMaxStackSize() { return 1; }
    }

    private static class PaymentSlot extends Slot {
        PaymentSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return stack.is(Items.DIAMOND); }
    }

    private static class PrizeSlot extends Slot {
        PrizeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
