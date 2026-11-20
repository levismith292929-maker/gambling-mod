package com.slotmachine.mod.block;

import com.slotmachine.mod.SlotMachineMod;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlockEntities {

    public static final BlockEntityType<SlotMachineBlockEntity> SLOT_MACHINE_BLOCK_ENTITY = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            Identifier.of(SlotMachineMod.MOD_ID, "slot_machine_block_entity"),
            FabricBlockEntityTypeBuilder.create(SlotMachineBlockEntity::new, ModBlocks.SLOT_MACHINE).build()
    );

    public static void registerBlockEntities() {
        SlotMachineMod.LOGGER.info("Registering block entities for " + SlotMachineMod.MOD_ID);
    }
}
