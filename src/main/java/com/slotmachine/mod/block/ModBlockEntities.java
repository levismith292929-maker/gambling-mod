package com.slotmachine.mod.block;

import com.slotmachine.mod.SlotMachineMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final BlockEntityType<SlotMachineBlockEntity> SLOT_MACHINE_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "slot_machine_block_entity"),
            FabricBlockEntityTypeBuilder.<SlotMachineBlockEntity>create(
                    SlotMachineBlockEntity::new, ModBlocks.SLOT_MACHINE).build());

    public static final BlockEntityType<PrinterBlockEntity> PRINTER_BLOCK_ENTITY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "printer_block_entity"),
            FabricBlockEntityTypeBuilder.<PrinterBlockEntity>create(
                    PrinterBlockEntity::new, ModBlocks.PRINTER).build());

    public static void registerBlockEntities() {
        SlotMachineMod.LOGGER.info("Registering block entities for " + SlotMachineMod.MOD_ID);
    }
}
