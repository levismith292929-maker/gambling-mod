package com.slotmachine.mod;

import com.slotmachine.mod.screen.ModScreenHandlers;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class SlotMachineModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Our screen handler extends GenericContainerScreenHandler, so the
        // existing chest screen renders it with no extra texture/layout work.
        HandledScreens.register(ModScreenHandlers.SLOT_MACHINE_SCREEN_HANDLER, GenericContainerScreen::new);
    }
}
