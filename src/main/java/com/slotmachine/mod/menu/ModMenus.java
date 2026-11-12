package com.slotmachine.mod.menu;

import com.slotmachine.mod.SlotMachineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {

    // Typed as MenuType<ChestMenu> so vanilla's ContainerScreen (a MenuAccess<ChestMenu>)
    // can be registered for it on the client.
    public static final MenuType<ChestMenu> SLOT_MACHINE = Registry.register(
            BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "slot_machine"),
            new MenuType<ChestMenu>((containerId, inventory) -> new SlotMachineMenu(containerId, inventory),
                    FeatureFlagSet.of())
    );

    public static void registerMenus() {
        SlotMachineMod.LOGGER.info("Registering menus for " + SlotMachineMod.MOD_ID);
    }
}
