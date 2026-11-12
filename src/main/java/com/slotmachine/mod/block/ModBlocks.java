package com.slotmachine.mod.block;

import com.slotmachine.mod.SlotMachineMod;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    // Placeholder bedrock-looking block for the test. No tool is required to get the drop.
    public static final Block SLOT_MACHINE = registerBlock(
            "slot_machine",
            SlotMachineBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 1200.0f)
    );

    public static final Item SLOT_MACHINE_ITEM = registerBlockItem("slot_machine", SLOT_MACHINE);

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory,
                                       BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    private static Item registerBlockItem(String name, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, name));
        return Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(block, new Item.Properties().setId(key).useBlockDescriptionPrefix()));
    }

    public static void registerModBlocks() {
        SlotMachineMod.LOGGER.info("Registering blocks for " + SlotMachineMod.MOD_ID);
    }
}
