package com.slotmachine.mod.screen;

import com.slotmachine.mod.menu.SuperCraftingMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Client screen for the custom five-by-five crafting menu. */
public class SuperCraftingTableScreen extends AbstractContainerScreen<SuperCraftingMenu> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            "slotmachine", "textures/gui/super_crafting_table.png");

    public SuperCraftingTableScreen(SuperCraftingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 256, 230);
        this.titleLabelX = 12;
        this.titleLabelY = 6;
        this.inventoryLabelX = 47;
        this.inventoryLabelY = 134;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.leftPos, this.topPos, 0.0F, 0.0F,
                this.imageWidth, this.imageHeight, 256, 256);
    }
}
