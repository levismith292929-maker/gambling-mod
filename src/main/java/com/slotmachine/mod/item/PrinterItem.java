package com.slotmachine.mod.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Printer item that displays the remaining amount of each ink in its tooltip. */
public class PrinterItem extends Item {
    public PrinterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> builder, TooltipFlag tooltipFlag) {
        builder.accept(inkLine("Black Ink: ", PrinterInkData.BLACK, stack, ChatFormatting.WHITE));
        builder.accept(inkLine("Magenta Ink: ", PrinterInkData.MAGENTA, stack, ChatFormatting.LIGHT_PURPLE));
        builder.accept(inkLine("Yellow Ink: ", PrinterInkData.YELLOW, stack, ChatFormatting.YELLOW));
        builder.accept(inkLine("Cyan Ink: ", PrinterInkData.CYAN, stack, ChatFormatting.AQUA));
    }

    private static Component inkLine(String label, String color, ItemStack stack, ChatFormatting formatting) {
        return Component.literal(label + PrinterInkData.get(stack, color) + "%").withStyle(formatting);
    }
}
