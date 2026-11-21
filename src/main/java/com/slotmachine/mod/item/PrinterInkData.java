package com.slotmachine.mod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Stores the four independent ink percentages on an individual printer item. */
public final class PrinterInkData {
    public static final String BLACK = "BlackInk";
    public static final String MAGENTA = "MagentaInk";
    public static final String YELLOW = "YellowInk";
    public static final String CYAN = "CyanInk";
    public static final String[] COLORS = { BLACK, MAGENTA, YELLOW, CYAN };

    private PrinterInkData() {}

    public static int get(ItemStack printer, String color) {
        CompoundTag tag = printer.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Math.max(0, Math.min(100, tag.getIntOr(color, 0)));
    }

    public static void set(ItemStack printer, String color, int amount) {
        CompoundTag tag = printer.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(color, Math.max(0, Math.min(100, amount)));
        printer.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static boolean canPrint(ItemStack printer) {
        for (String color : COLORS) {
            if (get(printer, color) < 10) return false;
        }
        return true;
    }

    public static void consumePrintInk(ItemStack printer) {
        for (String color : COLORS) {
            set(printer, color, get(printer, color) - 10);
        }
    }
}
