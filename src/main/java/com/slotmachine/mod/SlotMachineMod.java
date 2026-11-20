package com.slotmachine.mod;
import com.slotmachine.mod.block.ModBlockEntities;
import com.slotmachine.mod.block.ModBlocks;
import com.slotmachine.mod.item.ModItemGroup;
import com.slotmachine.mod.item.ModItems;
import com.slotmachine.mod.menu.ModMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class SlotMachineMod implements ModInitializer {
    public static final String MOD_ID = "slotmachine";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    @Override
    public void onInitialize() {
        LOGGER.info("Slot Machine mod loaded!");
        ModItems.registerModItems();
        ModBlocks.registerModBlocks();
        ModBlockEntities.registerBlockEntities();
        ModMenus.registerMenus();
        ModItemGroup.registerItemGroups();
        LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
            if (Blocks.SHORT_GRASS.getLootTable().orElseThrow().equals(key)) {
                LootPool.Builder cloverPool = LootPool.lootPool()
                        .add(LootItem.lootTableItem(ModItems.CLOVER))
                        .when(LootItemRandomChanceCondition.randomChance(0.05f));
                tableBuilder.withPool(cloverPool);
            }
        });
    }
}
