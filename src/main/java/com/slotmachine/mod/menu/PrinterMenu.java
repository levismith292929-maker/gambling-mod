package com.slotmachine.mod.menu;

import com.slotmachine.mod.block.PrinterBlockEntity;
import com.slotmachine.mod.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class PrinterMenu extends AbstractContainerMenu {
    private final Container printerInventory;
    private final PrinterBlockEntity printer;
    private final ContainerData inkData;

    public PrinterMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(5), new SimpleContainerData(4), null);
    }

    public PrinterMenu(int containerId, Inventory playerInventory, PrinterBlockEntity printer) {
        this(containerId, playerInventory, printer.getInventory(), printer.getInkData(), printer);
    }

    private PrinterMenu(int containerId, Inventory playerInventory, Container printerInventory,
                        ContainerData inkData, PrinterBlockEntity printer) {
        super(ModMenus.PRINTER, containerId);
        this.printerInventory = printerInventory;
        this.printer = printer;
        this.inkData = inkData;

        addSlot(new DyeSlot(printerInventory, 0, 46, 20, "black_dye"));
        addSlot(new DyeSlot(printerInventory, 1, 66, 20, "magenta_dye"));
        addSlot(new DyeSlot(printerInventory, 2, 86, 20, "yellow_dye"));
        addSlot(new DyeSlot(printerInventory, 3, 106, 20, "cyan_dye"));
        addSlot(new Slot(printerInventory, 4, 136, 88) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(ModItems.BLANK_CARD); }
            @Override public int getMaxStackSize() { return 64; }
        });

        SimpleContainer printButton = new SimpleContainer(1);
        ItemStack button = new ItemStack(Items.LIME_STAINED_GLASS_PANE);
        button.set(DataComponents.CUSTOM_NAME, Component.literal("Print"));
        printButton.setItem(0, button);
        addSlot(new Slot(printButton, 0, 181, 88) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        6 + column * 18, 136 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 6 + column * 18, 194));
        }
        addDataSlots(inkData);
    }

    public int getInk(int color) {
        return inkData.get(color);
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput clickType, Player player) {
        if (slotId == 5) {
            if (!player.level().isClientSide() && printer != null) printer.printCard(player);
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return printerInventory.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem().copy();

        if (slotIndex >= 6) {
            ItemStack moving = slot.getItem();
            boolean placed = false;
            for (int target = 0; target < 5; target++) {
                if (slots.get(target).mayPlace(moving)) {
                    if (moveItemStackTo(moving, target, target + 1, false)) {
                        placed = true;
                        break;
                    }
                }
            }
            if (!placed) return ItemStack.EMPTY;
        } else {
            if (!moveItemStackTo(slot.getItem(), 6, slots.size(), false)) return ItemStack.EMPTY;
        }
        if (slot.getItem().isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    private static class DyeSlot extends Slot {
        private final String dyeId;
        DyeSlot(Container container, int index, int x, int y, String dyeId) {
            super(container, index, x, y);
            this.dyeId = dyeId;
        }
        @Override public boolean mayPlace(ItemStack stack) {
            return stack.is(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(dyeId)));
        }
    }
}
