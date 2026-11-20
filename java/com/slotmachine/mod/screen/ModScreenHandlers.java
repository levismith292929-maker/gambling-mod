package com.slotmachine.mod.screen;

import com.slotmachine.mod.SlotMachineMod;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {

    // The Luck Slot GUI doesn't need its own type — it's opened directly as a
    // vanilla GenericContainerScreenHandler (GENERIC_9X3) with no slot restrictions.
    public static final ScreenHandlerType<SlotMachineScreenHandler> SLOT_MACHINE_SCREEN_HANDLER =
            ScreenHandlerRegistry.registerSimple(
                    Identifier.of(SlotMachineMod.MOD_ID, "slot_machine"),
                    SlotMachineScreenHandler::new
            );

    public static void registerScreenHandlers() {
        SlotMachineMod.LOGGER.info("Registering screen handlers for " + SlotMachineMod.MOD_ID);
    }
}
