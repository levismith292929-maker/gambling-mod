package com.slotmachine.mod.block;

import com.slotmachine.mod.SlotMachineMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    // Hardness/resistance borrowed loosely from bedrock-tier for the test —
    // tune these once the real block texture/behavior is finalized.
    public static final Block SLOT_MACHINE = registerBlock(
            "slot_machine",
            new SlotMachineBlock(AbstractBlock.Settings.create()
                    .strength(50.0f, 1200.0f)
                    .requiresTool())
    );

    public static final Item SLOT_MACHINE_ITEM = registerBlockItem("slot_machine", SLOT_MACHINE);

    private static Block registerBlock(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(SlotMachineMod.MOD_ID, name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        return Registry.register(Registries.ITEM, Identifier.of(SlotMachineMod.MOD_ID, name),
                new BlockItem(block, new Item.Settings()));
    }

    public static void registerModBlocks() {
        SlotMachineMod.LOGGER.info("Registering blocks for " + SlotMachineMod.MOD_ID);
    }
}
