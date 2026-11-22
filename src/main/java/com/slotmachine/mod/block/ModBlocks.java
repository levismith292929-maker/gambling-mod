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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class ModBlocks {
    public static final Block SLOT_MACHINE = registerBlock(
            "slot_machine", SlotMachineBlock::new,
            BlockBehaviour.Properties.of().strength(3.0f, 1200.0f));
    public static final Item SLOT_MACHINE_ITEM = registerBlockItem("slot_machine", SLOT_MACHINE);

    public static final Block SUPER_CRAFTING_TABLE = registerBlock(
            "super_crafting_table", SuperCraftingTableBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .sound(SoundType.WOOD).strength(2.5f, 6.0f));
    public static final Item SUPER_CRAFTING_TABLE_ITEM =
            registerBlockItem("super_crafting_table", SUPER_CRAFTING_TABLE);

    public static final Block PRINTER = registerBlock(
            "printer", PrinterBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL)
                    .sound(SoundType.METAL).strength(2.5f, 6.0f).noOcclusion());
    public static final Item PRINTER_ITEM = registerBlockItem("printer", PRINTER);

    public static final Block DECK = registerBlock(
            "deck", DeckBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                    .sound(SoundType.WOOD).strength(0.5f, 2.0f).noOcclusion());
    public static final Item DECK_ITEM = registerBlockItem("deck", DECK);

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