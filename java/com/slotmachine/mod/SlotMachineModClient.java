package com.slotmachine.mod;

import com.slotmachine.mod.menu.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

public class SlotMachineModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Our menu is a ChestMenu, so vanilla's chest screen renders it with no extra work.
        MenuScreens.register(ModMenus.SLOT_MACHINE, ContainerScreen::new);
    }
}
