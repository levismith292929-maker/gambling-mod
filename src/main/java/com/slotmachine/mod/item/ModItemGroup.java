package com.slotmachine.mod.item;

import com.slotmachine.mod.SlotMachineMod;
import com.slotmachine.mod.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroup {

    public static final ItemGroup SLOT_MACHINE_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            Identifier.of(SlotMachineMod.MOD_ID, "slot_machine_group"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.slotmachine.slot_machine_group"))
                    .icon(() -> new ItemStack(ModBlocks.SLOT_MACHINE_ITEM))
                    .entries((displayContext, entries) -> {
                        entries.add(ModItems.GEAR);
                        entries.add(ModItems.SLOT_REEL);
                        entries.add(ModItems.SLOT_HANDLE);
                        entries.add(ModItems.COPPER_WIRE);
                        entries.add(ModItems.SLOTTOPHALF);
                        entries.add(ModItems.SLOTBOTTOMHALF);
                        entries.add(ModBlocks.SLOT_MACHINE_ITEM);
                    })
                    .build()
    );

    public static void registerItemGroups() {
        SlotMachineMod.LOGGER.info("Registering item groups for " + SlotMachineMod.MOD_ID);
    }
}
