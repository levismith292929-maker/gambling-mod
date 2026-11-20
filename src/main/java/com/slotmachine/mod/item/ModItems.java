package com.slotmachine.mod.item;

import com.slotmachine.mod.SlotMachineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModItems {
    public static final Item GEAR = register("gear");
    public static final Item SLOT_REEL = register("slot_reel");
    public static final Item SLOT_HANDLE = register("slot_handle");
    public static final Item COPPER_WIRE = register("copper_wire");
    public static final Item SLOTTOPHALF = register("slottophalf");
    public static final Item SLOTBOTTOMHALF = register("slotbottomhalf");
    public static final Item CLOVER = register("clover");
    public static final Item EMPTY_CHARM = register("empty_charm");
    public static final Item STONE_LUCK_CHARM = registerLuckCharm("stone_luck_charm", 10, 1);
    public static final Item IRON_LUCK_CHARM = registerLuckCharm("iron_luck_charm", 15, 2);
    public static final Item GOLD_LUCK_CHARM = registerLuckCharm("gold_luck_charm", 20, 3);
    public static final Item DIAMOND_LUCK_CHARM = registerLuckCharm("diamond_luck_charm", 40, 4);

    private static Item register(String name) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
    }

    private static Item registerLuckCharm(String name, int luck, int tier) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new LuckCharmItem(new Item.Properties().setId(key).stacksTo(1), luck, tier));
    }

    public static boolean isLuckCharm(ItemStack stack) {
        return stack.is(STONE_LUCK_CHARM) || stack.is(IRON_LUCK_CHARM)
                || stack.is(GOLD_LUCK_CHARM) || stack.is(DIAMOND_LUCK_CHARM);
    }

    public static int getLuckCharmTier(ItemStack stack) {
        if (stack.is(STONE_LUCK_CHARM)) return 1;
        if (stack.is(IRON_LUCK_CHARM)) return 2;
        if (stack.is(GOLD_LUCK_CHARM)) return 3;
        if (stack.is(DIAMOND_LUCK_CHARM)) return 4;
        return 0;
    }

    public static void registerModItems() {
        SlotMachineMod.LOGGER.info("Registering items for " + SlotMachineMod.MOD_ID);
    }
}
