package com.slotmachine.mod.item;
import com.slotmachine.mod.SlotMachineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
public class ModItems {
    public static final Item GEAR = register("gear");
    public static final Item SLOT_REEL = register("slot_reel");
    public static final Item SLOT_HANDLE = register("slot_handle");
    public static final Item COPPER_WIRE = register("copper_wire");
    public static final Item SLOTTOPHALF = register("slottophalf");
    public static final Item SLOTBOTTOMHALF = register("slotbottomhalf");
    public static final Item CLOVER = register("clover");
    public static final Item EMPTY_CHARM = register("empty_charm");
    private static Item register(String name) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
    }
    public static void registerModItems() {
        SlotMachineMod.LOGGER.info("Registering items for " + SlotMachineMod.MOD_ID);
    }
}
