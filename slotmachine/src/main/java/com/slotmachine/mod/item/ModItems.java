package com.slotmachine.mod.item;

import com.slotmachine.mod.SlotMachineMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Registers every item for the slot machine mod.
 *
 * NOTE: the exact Item/Registry API shape can shift slightly between
 * Minecraft versions. This follows the pattern that's been stable since
 * 1.20.5 (Registries.ITEM + Item.Settings). If this doesn't compile
 * against 26.3 as-is, diff it against a freshly generated Fabric
 * template project (fabricmc.net -> Develop -> template generator,
 * targeting 26.3) and fix the imports/method names to match — the
 * registration logic itself won't need to change.
 */
public class ModItems {

    // Crafting components
    public static final Item GEAR = register("gear", new Item(new Item.Settings()));
    public static final Item SLOT_REEL = register("slot_reel", new Item(new Item.Settings()));
    public static final Item SLOT_HANDLE = register("slot_handle", new Item(new Item.Settings()));
    public static final Item COPPER_WIRE = register("copper_wire", new Item(new Item.Settings()));

    // Assembly halves
    public static final Item SLOTTOPHALF = register("slottophalf", new Item(new Item.Settings()));
    public static final Item SLOTBOTTOMHALF = register("slotbottomhalf", new Item(new Item.Settings()));

    // The finished slot machine is now a block (see ModBlocks.SLOT_MACHINE /
    // ModBlocks.SLOT_MACHINE_ITEM) — no crafting recipe yet for combining the
    // two halves into it; add one once you decide how that works.

    private static Item register(String name, Item item) {
        Identifier id = Identifier.of(SlotMachineMod.MOD_ID, name);
        return Registry.register(Registries.ITEM, id, item);
    }

    public static void registerModItems() {
        SlotMachineMod.LOGGER.info("Registering items for " + SlotMachineMod.MOD_ID);
    }
}
