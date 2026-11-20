package com.slotmachine.mod.menu;
import com.slotmachine.mod.block.SimpleBlockContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
/** Shows black glass in empty Luck Slot spaces without occupying the real inventory slots. */
public class LuckSlotMenu extends ChestMenu {
    public LuckSlotMenu(int containerId, Inventory playerInventory, Container container) {
        super(net.minecraft.world.inventory.MenuType.GENERIC_9x3, containerId, playerInventory, container, 3);
    }
    @Override
    protected Slot addSlot(Slot slot) {
        if (slot.container instanceof SimpleBlockContainer) {
            slot = new BackgroundSlot(slot.container, slot.getContainerSlot(), slot.x, slot.y);
        }
        return super.addSlot(slot);
    }
    private static class BackgroundSlot extends Slot {
        private static final ItemStack BACKGROUND = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", "black_stained_glass_pane")));
        BackgroundSlot(Container container, int slot, int x, int y) { super(container, slot, x, y); }
        @Override public ItemStack getItem() {
            ItemStack actual = super.getItem();
            return actual.isEmpty() ? BACKGROUND : actual;
        }
        @Override public boolean hasItem() { return !super.getItem().isEmpty(); }
        @Override public boolean mayPlace(ItemStack stack) { return true; }
    }
}
