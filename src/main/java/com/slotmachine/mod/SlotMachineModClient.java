package com.slotmachine.mod;

import com.slotmachine.mod.menu.ModMenus;
import com.slotmachine.mod.screen.PrinterScreen;
import com.slotmachine.mod.screen.SuperCraftingTableScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

public class SlotMachineModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModMenus.SLOT_MACHINE, ContainerScreen::new);
        MenuScreens.register(ModMenus.SUPER_CRAFTING_TABLE, SuperCraftingTableScreen::new);
        MenuScreens.register(ModMenus.PRINTER, PrinterScreen::new);
    }
}
