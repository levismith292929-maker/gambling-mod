package com.slotmachine.mod.screen;

import com.slotmachine.mod.menu.PrinterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Printer screen with four live ink-level tanks. */
public class PrinterScreen extends AbstractContainerScreen<PrinterMenu> {
    private static final int[] TANK_COLORS = {
            0xFF333333, 0xFFE000A8, 0xFFFFD500, 0xFF00CFEA
    };

    public PrinterScreen(PrinterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 240, 240);
        this.titleLabelX = 8;
        this.titleLabelY = 7;
        this.inventoryLabelY = 300;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFBEBEBE);
        graphics.fill(x + 2, y + 2, x + 238, y + 238, 0xFFE0E0E0);
        graphics.fill(x + 4, y + 4, x + 236, y + 236, 0xFFBDBDBD);

        // Each tank fills upward from the bottom according to its stored percentage.
        for (int i = 0; i < 4; i++) {
            int tankX = x + 46 + i * 20;
            int tankTop = y + 43;
            int tankBottom = y + 125;
            graphics.fill(tankX, tankTop, tankX + 18, tankBottom, 0xFF7D8C8C);
            graphics.fill(tankX + 2, tankTop + 2, tankX + 16, tankBottom - 2, 0xFFBBD0D0);
            int fillHeight = (tankBottom - tankTop - 4) * menu.getInk(i) / 100;
            if (fillHeight > 0) {
                graphics.fill(tankX + 2, tankBottom - 2 - fillHeight,
                        tankX + 16, tankBottom - 2, TANK_COLORS[i]);
            }
            graphics.fill(tankX, tankTop, tankX + 18, tankTop + 2, 0xFF536060);
            graphics.fill(tankX, tankBottom - 2, tankX + 18, tankBottom, 0xFF536060);
        }

        graphics.fill(x + 134, y + 86, x + 154, y + 106, 0xFF777777);
        graphics.fill(x + 136, y + 88, x + 152, y + 104, 0xFFE8E8E8);
        graphics.fill(x + 179, y + 86, x + 199, y + 106, 0xFF4C8A43);
        graphics.fill(x + 181, y + 88, x + 197, y + 104, 0xFF65B84F);

        graphics.fill(x + 4, y + 134, x + 236, y + 214, 0xFF858585);
        graphics.fill(x + 6, y + 136, x + 234, y + 212, 0xFFC5C5C5);
    }
}
