package com.slotmachine.mod.menu;

import com.slotmachine.mod.block.SimpleBlockContainer;
import com.slotmachine.mod.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class LuckSlotMenu extends ChestMenu {
    public LuckSlotMenu(int containerId, Inventory playerInventory, Container container) {
        super(net.minecraft.world.inventory.MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
    }

    @Override
    protected Slot addSlot(Slot slot) {
        if (slot.container instanceof SimpleBlockContainer) {
            if (slot.getContainerSlot() == 13) {
                slot = new CharmSlot(slot.container, slot.getContainerSlot(), slot.x, slot.y);
            } else {
                slot = new BackgroundSlot(slot.container, slot.getContainerSlot(), slot.x, slot.y);
            }
        }
        return super.addSlot(slot);
    }

    private static class CharmSlot extends Slot {
        CharmSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return ModItems.isLuckCharm(stack); }
    }

    private static class BackgroundSlot extends Slot {
        private static final ItemStack BACKGROUND = new ItemStack(
                BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "black_stained_glass_pane")));
        BackgroundSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public ItemStack getItem() {
            ItemStack actual = super.getItem();
            return actual.isEmpty() ? BACKGROUND.copy() : actual;
        }
        @Override public boolean hasItem() { return !super.getItem().isEmpty(); }
        @Override public boolean mayPickup(Player player) {
            return !super.getItem().isEmpty() && super.mayPickup(player);
        }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
