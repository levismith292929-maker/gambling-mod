package com.slotmachine.mod;

import com.slotmachine.mod.block.ModBlockEntities;
import com.slotmachine.mod.block.ModBlocks;
import com.slotmachine.mod.item.ModItemGroup;
import com.slotmachine.mod.item.ModItems;
import com.slotmachine.mod.screen.ModScreenHandlers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlotMachineMod implements ModInitializer {

    public static final String MOD_ID = "slotmachine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Slot Machine mod loaded!");
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModScreenHandlers.registerScreenHandlers();
        ModItems.registerModItems();
        ModItemGroup.registerItemGroups();
    }
}
