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
import net.minecraft.world.item.Items;
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

    private PrinterCraftingRecipes() {}

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "printer_ink"),
                PRINTER_INK_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SlotMachineMod.MOD_ID, "print_card"),
                PRINT_CARD_SERIALIZER);
        SlotMachineMod.LOGGER.info("Registered printer crafting recipes");
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
        if (stack.is(Items.BLACK_DYE)) return PrinterInkData.BLACK;
        if (stack.is(Items.MAGENTA_DYE)) return PrinterInkData.MAGENTA;
        if (stack.is(Items.YELLOW_DYE)) return PrinterInkData.YELLOW;
        if (stack.is(Items.CYAN_DYE)) return PrinterInkData.CYAN;
        return null;
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
