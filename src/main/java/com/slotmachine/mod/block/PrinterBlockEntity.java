package com.slotmachine.mod.block;

import com.slotmachine.mod.item.ModItems;
import com.slotmachine.mod.menu.PrinterMenu;
import java.util.function.IntConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class PrinterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BLACK_SLOT = 0;
    public static final int MAGENTA_SLOT = 1;
    public static final int YELLOW_SLOT = 2;
    public static final int CYAN_SLOT = 3;
    public static final int BLANK_CARD_SLOT = 4;
    private static final String[] INK_KEYS = {"BlackInk", "MagentaInk", "YellowInk", "CyanInk"};
    private static final String[] DYE_IDS = {
            "minecraft:black_dye", "minecraft:magenta_dye",
            "minecraft:yellow_dye", "minecraft:cyan_dye"
    };

    private final SimpleBlockContainer inventory =
            new SimpleBlockContainer(5, this::setChanged,
                    player -> Container.stillValidBlockEntity(this, player));
    private final int[] ink = new int[4];

    private final ContainerData data = new ContainerData() {
        @Override public int get(int index) { return ink[index]; }
        @Override public void set(int index, int value) { setInk(index, value); }
        @Override public int getCount() { return ink.length; }
    };

    public PrinterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRINTER_BLOCK_ENTITY, pos, state);
    }

    public SimpleBlockContainer getInventory() {
        return inventory;
    }

    public ContainerData getInkData() {
        return data;
    }

    public int getInk(int color) {
        return ink[color];
    }

    private void setInk(int color, int amount) {
        ink[color] = Math.max(0, Math.min(100, amount));
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PrinterBlockEntity printer) {
        if (level.isClientSide()) return;
        printer.processDyeInputs();
    }

    private void processDyeInputs() {
        for (int color = 0; color < 4; color++) {
            ItemStack dye = inventory.getItem(color);
            if (dye.isEmpty() || ink[color] >= 100) continue;
            var expected = BuiltInRegistries.ITEM.getValue(Identifier.parse(DYE_IDS[color]));
            if (!dye.is(expected)) continue;

            int accepted = Math.min(dye.getCount(), (100 - ink[color] + 19) / 20);
            if (accepted <= 0) continue;
            ink[color] = Math.min(100, ink[color] + accepted * 20);
            dye.shrink(accepted);
            if (dye.isEmpty()) inventory.setItem(color, ItemStack.EMPTY);
            else inventory.setChanged();
            setChanged();
        }
    }

    public void printCard(Player player) {
        if (level == null || level.isClientSide()) return;
        ItemStack blankCard = inventory.getItem(BLANK_CARD_SLOT);
        if (blankCard.isEmpty() || !blankCard.is(ModItems.BLANK_CARD)) return;
        for (int amount : ink) {
            if (amount < 10) {
                player.displayClientMessage(Component.literal("The printer needs at least 10% of every ink."), true);
                return;
            }
        }

        blankCard.shrink(1);
        if (blankCard.isEmpty()) inventory.setItem(BLANK_CARD_SLOT, ItemStack.EMPTY);
        else inventory.setChanged();

        for (int i = 0; i < ink.length; i++) ink[i] -= 10;
        ItemStack printedCard = new ItemStack(ModItems.PLAYING_CARD);
        if (!player.getInventory().add(printedCard)) {
            Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5, printedCard);
        }
        setChanged();
        player.displayClientMessage(Component.literal("Printed a Playing Card!"), true);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Card Printer");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PrinterMenu(containerId, playerInventory, this);
    }

    public void dropContents() {
        if (level == null || level.isClientSide()) return;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5, stack.copy());
                inventory.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        net.minecraft.world.ContainerHelper.saveAllItems(output.child("PrinterInventory"), inventory.getItems());
        for (int i = 0; i < ink.length; i++) output.putInt(INK_KEYS[i], ink[i]);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inventory.getItems().replaceAll(stack -> ItemStack.EMPTY);
        net.minecraft.world.ContainerHelper.loadAllItems(input.childOrEmpty("PrinterInventory"), inventory.getItems());
        for (int i = 0; i < ink.length; i++) ink[i] = Math.max(0, Math.min(100, input.getIntOr(INK_KEYS[i], 0)));
    }
}
