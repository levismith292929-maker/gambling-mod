package com.slotmachine.mod.crafting;

import com.mojang.serialization.MapCodec;
import com.slotmachine.mod.SlotMachineMod;
import com.slotmachine.mod.item.ModItems;
import com.slotmachine.mod.item.PrinterInkData;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** Dynamic crafting recipes for filling the printer and printing playing cards. */
public final class PrinterCraftingRecipes {
    public static final RecipeSerializer<PrinterInkRecipe> PRINTER_INK_SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(new PrinterInkRecipe()),
                    StreamCodec.unit(new PrinterInkRecipe()));
    public static final RecipeSerializer<PrintCardRecipe> PRINT_CARD_SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(new PrintCardRecipe()),
                    StreamCodec.unit(new PrintCardRecipe()));
    public static final RecipeSerializer<InkTankRecipe> INK_TANK_SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(new InkTankRecipe()),
                    StreamCodec.unit(new InkTankRecipe()));
    public static final RecipeSerializer<EmptyPrinterRecipe> EMPTY_PRINTER_SERIALIZER =
            new RecipeSerializer<>(MapCodec.unit(new EmptyPrinterRecipe()),
                    StreamCodec.unit(new EmptyPrinterRecipe()));

    private PrinterCraftingRecipes() {}

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "printer_ink"),
                PRINTER_INK_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "print_card"),
                PRINT_CARD_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "ink_tank"),
                INK_TANK_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "empty_printer"),
                EMPTY_PRINTER_SERIALIZER);
        SlotMachineMod.LOGGER.info("Registered printer and large crafting recipes");
    }

    private static int countItems(CraftingInput input) {
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    private static int findPrinter(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).is(ModItems.EMPTY_PRINTER)) return i;
        }
        return -1;
    }

    private static int findBlankCard(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).is(ModItems.BLANK_CARD)) return i;
        }
        return -1;
    }

    private static String dyeColor(ItemStack stack) {
        if (stack.is(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("black_dye")))) return PrinterInkData.BLACK;
        if (stack.is(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("magenta_dye")))) return PrinterInkData.MAGENTA;
        if (stack.is(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("yellow_dye")))) return PrinterInkData.YELLOW;
        if (stack.is(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("cyan_dye")))) return PrinterInkData.CYAN;
        return null;
    }


    private static boolean isItem(ItemStack stack, String id) {
        return stack.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)));
    }

    /**
     * Checks a shaped pattern anywhere in the 5x5 Super Crafting Table.
     * The pattern may be mirrored, but every slot outside the pattern must be empty.
     */
    private static boolean matchesPattern(CraftingInput input, String[] pattern, String[][] ingredients) {
        int patternHeight = pattern.length;
        int patternWidth = pattern[0].length();
        if (input.width() < patternWidth || input.height() < patternHeight) return false;

        for (int offsetY = 0; offsetY <= input.height() - patternHeight; offsetY++) {
            for (int offsetX = 0; offsetX <= input.width() - patternWidth; offsetX++) {
                for (int mirror = 0; mirror < 2; mirror++) {
                    boolean matches = true;
                    for (int y = 0; y < input.height() && matches; y++) {
                        for (int x = 0; x < input.width(); x++) {
                            int localX = x - offsetX;
                            int localY = y - offsetY;
                            ItemStack stack = input.getItem(x + y * input.width());

                            if (localX < 0 || localX >= patternWidth
                                    || localY < 0 || localY >= patternHeight) {
                                if (!stack.isEmpty()) {
                                    matches = false;
                                    break;
                                }
                                continue;
                            }

                            int patternX = mirror == 0 ? localX : patternWidth - 1 - localX;
                            char symbol = pattern[localY].charAt(patternX);
                            if (symbol == ' ') {
                                if (!stack.isEmpty()) {
                                    matches = false;
                                    break;
                                }
                            } else {
                                String[] accepted = ingredients[symbol];
                                boolean found = false;
                                if (accepted != null) {
                                    for (String itemId : accepted) {
                                        if (isItem(stack, itemId)) {
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (!found) {
                                    matches = false;
                                    break;
                                }
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
        private static final String[] PATTERN = {
                "iIi",
                "p p",
                "p p",
                "p p",
                "iIi"
        };
        private static final String[][] INGREDIENTS = new String[128][];

        static {
            INGREDIENTS['i'] = new String[]{"minecraft:iron_nugget"};
            INGREDIENTS['I'] = new String[]{"minecraft:iron_ingot"};
            INGREDIENTS['p'] = new String[]{"minecraft:glass_pane"};
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return matchesPattern(input, PATTERN, INGREDIENTS);
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            return new ItemStack(ModItems.INK_TANK);
        }

        @Override
        public RecipeSerializer<? extends CustomRecipe> getSerializer() {
            return INK_TANK_SERIALIZER;
        }
    }

    public static class EmptyPrinterRecipe extends CustomRecipe {
        private static final String[] PATTERN = {
                "fiiif",
                "ItttI",
                "IgwgI",
                "IIIII"
        };
        private static final String[][] INGREDIENTS = new String[128][];

        static {
            INGREDIENTS['i'] = new String[]{"minecraft:iron_nugget"};
            INGREDIENTS['I'] = new String[]{"minecraft:iron_ingot"};
            INGREDIENTS['t'] = new String[]{"slotmachine:ink_tank"};
            INGREDIENTS['f'] = new String[]{"minecraft:feather"};
            INGREDIENTS['g'] = new String[]{"slotmachine:gear"};
            INGREDIENTS['w'] = new String[]{"slotmachine:copper_wire"};
        }

        @Override
        public boolean matches(CraftingInput input, Level level) {
            return matchesPattern(input, PATTERN, INGREDIENTS);
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            return new ItemStack(ModItems.EMPTY_PRINTER);
        }

        @Override
        public RecipeSerializer<? extends CustomRecipe> getSerializer() {
            return EMPTY_PRINTER_SERIALIZER;
        }
    }

    public static class PrinterInkRecipe extends CustomRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            if (countItems(input) != 2) return false;
            int printerSlot = findPrinter(input);
            if (printerSlot < 0) return false;

            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (i == printerSlot || stack.isEmpty()) continue;
                String color = dyeColor(stack);
                return color != null && PrinterInkData.get(input.getItem(printerSlot), color) <= 80;
            }
            return false;
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            int printerSlot = findPrinter(input);
            if (printerSlot < 0) return ItemStack.EMPTY;
            ItemStack result = input.getItem(printerSlot).copyWithCount(1);
            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);
                if (i == printerSlot || stack.isEmpty()) continue;
                String color = dyeColor(stack);
                if (color != null) PrinterInkData.set(result, color, PrinterInkData.get(result, color) + 20);
            }
            return result;
        }

        @Override
        public RecipeSerializer<? extends CustomRecipe> getSerializer() {
            return PRINTER_INK_SERIALIZER;
        }
    }

    public static class PrintCardRecipe extends CustomRecipe {
        @Override
        public boolean matches(CraftingInput input, Level level) {
            if (countItems(input) != 2) return false;
            int printerSlot = findPrinter(input);
            return printerSlot >= 0 && findBlankCard(input) >= 0
                    && PrinterInkData.canPrint(input.getItem(printerSlot));
        }

        @Override
        public ItemStack assemble(CraftingInput input) {
            return new ItemStack(ModItems.PLAYING_CARD);
        }

        @Override
        public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
            NonNullList<ItemStack> remaining = CraftingRecipe.defaultCraftingReminder(input);
            int printerSlot = findPrinter(input);
            if (printerSlot >= 0) {
                ItemStack returnedPrinter = input.getItem(printerSlot).copyWithCount(1);
                PrinterInkData.consumePrintInk(returnedPrinter);
                remaining.set(printerSlot, returnedPrinter);
            }
            return remaining;
        }

        @Override
        public RecipeSerializer<? extends CustomRecipe> getSerializer() {
            return PRINT_CARD_SERIALIZER;
        }
    }
}
