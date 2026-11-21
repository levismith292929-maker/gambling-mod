package com.slotmachine.mod.crafting;

import com.mojang.serialization.MapCodec;
import com.slotmachine.mod.SlotMachineMod;
import com.slotmachine.mod.block.ModBlocks;
import com.slotmachine.mod.item.ModItems;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Custom recipes for the large crafting table's ink tank and printer. */
public final class PrinterCraftingRecipes {
    public static final RecipeSerializer<InkTankRecipe> INK_TANK_SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(new InkTankRecipe()), StreamCodec.unit(new InkTankRecipe()));
    public static final RecipeSerializer<EmptyPrinterRecipe> EMPTY_PRINTER_SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(new EmptyPrinterRecipe()), StreamCodec.unit(new EmptyPrinterRecipe()));

    private PrinterCraftingRecipes() {}

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "ink_tank"), INK_TANK_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "empty_printer"), EMPTY_PRINTER_SERIALIZER);
        SlotMachineMod.LOGGER.info("Registered ink tank and printer crafting recipes");
    }

    private static boolean isItem(ItemStack stack, String id) {
        return stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    private static boolean matchesPattern(CraftingInput input, String[] pattern, String[][] ingredients) {
        int height = pattern.length;
        int width = pattern[0].length();
        if (input.width() < width || input.height() < height) return false;

        for (int offsetY = 0; offsetY <= input.height() - height; offsetY++) {
            for (int offsetX = 0; offsetX <= input.width() - width; offsetX++) {
                for (int mirror = 0; mirror < 2; mirror++) {
                    boolean matches = true;
                    for (int y = 0; y < input.height() && matches; y++) {
                        for (int x = 0; x < input.width(); x++) {
                            int localX = x - offsetX;
                            int localY = y - offsetY;
                            ItemStack stack = input.getItem(x + y * input.width());
                            if (localX < 0 || localX >= width || localY < 0 || localY >= height) {
                                if (!stack.isEmpty()) { matches = false; break; }
                                continue;
                            }
                            int patternX = mirror == 0 ? localX : width - 1 - localX;
                            char symbol = pattern[localY].charAt(patternX);
                            if (symbol == ' ') {
                                if (!stack.isEmpty()) { matches = false; break; }
                            } else {
                                boolean found = false;
                                String[] accepted = ingredients[symbol];
                                if (accepted != null) {
                                    for (String id : accepted) {
                                        if (isItem(stack, id)) { found = true; break; }
                                    }
                                }
                                if (!found) { matches = false; break; }
                            }
                        }
                    }
                    if (matches) return true;
                }
            }
        }
        return false;
    }

    public static class InkTankRecipe extends CustomRecipe {
        private static final String[] PATTERN = {"iIi", "p p", "p p", "p p", "iIi"};
        private static final String[][] INGREDIENTS = new String[128][];
        static {
            INGREDIENTS['i'] = new String[]{"minecraft:iron_nugget"};
            INGREDIENTS['I'] = new String[]{"minecraft:iron_ingot"};
            INGREDIENTS['p'] = new String[]{"minecraft:glass_pane"};
        }
        @Override public boolean matches(CraftingInput input, Level level) {
            return matchesPattern(input, PATTERN, INGREDIENTS);
        }
        @Override public ItemStack assemble(CraftingInput input) { return new ItemStack(ModItems.INK_TANK); }
        @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return INK_TANK_SERIALIZER; }
    }

    public static class EmptyPrinterRecipe extends CustomRecipe {
        private static final String[] PATTERN = {"fiiif", "ItttI", "IgwgI", "IIIII"};
        private static final String[][] INGREDIENTS = new String[128][];
        static {
            INGREDIENTS['i'] = new String[]{"minecraft:iron_nugget"};
            INGREDIENTS['I'] = new String[]{"minecraft:iron_ingot"};
            INGREDIENTS['t'] = new String[]{"slotmachine:ink_tank"};
            INGREDIENTS['f'] = new String[]{"minecraft:feather"};
            INGREDIENTS['g'] = new String[]{"slotmachine:gear"};
            INGREDIENTS['w'] = new String[]{"slotmachine:copper_wire"};
        }
        @Override public boolean matches(CraftingInput input, Level level) {
            return matchesPattern(input, PATTERN, INGREDIENTS);
        }
        @Override public ItemStack assemble(CraftingInput input) { return new ItemStack(ModBlocks.PRINTER_ITEM); }
        @Override public RecipeSerializer<? extends CustomRecipe> getSerializer() { return EMPTY_PRINTER_SERIALIZER; }
    }
}
