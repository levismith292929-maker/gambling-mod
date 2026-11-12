package com.slotmachine.mod.item;

import com.slotmachine.mod.SlotMachineMod;
import com.slotmachine.mod.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/** The "Gambling" creative inventory tab. */
public class ModItemGroup {

    public static final ResourceKey<CreativeModeTab> GAMBLING_TAB_KEY = ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "gambling"));

    public static final CreativeModeTab GAMBLING_TAB = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(ModBlocks.SLOT_MACHINE_ITEM))
            .title(Component.translatable("itemGroup.slotmachine.slot_machine_group"))
            .displayItems((params, output) -> {
                output.accept(ModItems.GEAR);
                output.accept(ModItems.SLOT_REEL);
                output.accept(ModItems.SLOT_HANDLE);
                output.accept(ModItems.COPPER_WIRE);
                output.accept(ModItems.SLOTTOPHALF);
                output.accept(ModItems.SLOTBOTTOMHALF);
                output.accept(ModBlocks.SLOT_MACHINE_ITEM);
            })
            .build();

    public static void registerItemGroups() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GAMBLING_TAB_KEY, GAMBLING_TAB);
        SlotMachineMod.LOGGER.info("Registering item groups for " + SlotMachineMod.MOD_ID);
    }
}
