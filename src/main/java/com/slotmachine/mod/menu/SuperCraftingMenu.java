package com.slotmachine.mod.menu;

import java.util.List;
import java.util.Optional;

import com.slotmachine.mod.block.ModBlocks;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractCraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/** A vanilla-style crafting menu with a five-by-five input grid. */
public class SuperCraftingMenu extends AbstractCraftingMenu {
    private static final int RESULT_SLOT = 0;
    private static final int INPUT_START = 1;
    private static final int INPUT_END = 26;
    private static final int INVENTORY_START = INPUT_END;
    private static final int INVENTORY_END = INVENTORY_START + 36;

    private final ContainerLevelAccess access;
    private final Player player;
    private boolean placingRecipe;

    public SuperCraftingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public SuperCraftingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(ModMenus.SUPER_CRAFTING_TABLE, containerId, 5, 5);
        this.access = access;
        this.player = inventory.player;
        this.addResultSlot(this.player, 214, 59);
        this.addCraftingGridSlots(15, 23);
        this.addStandardInventorySlots(inventory, 47, 145);
    }

    private static void updateCraftingResult(SuperCraftingMenu menu, ServerLevel level, Player player,
                                             net.minecraft.world.inventory.ResultContainer resultSlots,
                                             RecipeHolder<CraftingRecipe> recipeHint) {
        CraftingInput input = menu.craftSlots.asCraftInput();
        ServerPlayer serverPlayer = (ServerPlayer) player;
        ItemStack result = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> recipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, level, recipeHint);

        if (recipe.isPresent()) {
            RecipeHolder<CraftingRecipe> holder = recipe.get();
            if (resultSlots.setRecipeUsed(serverPlayer, holder)) {
                ItemStack candidate = holder.value().assemble(input);
                if (candidate.isItemEnabled(level.enabledFeatures())) {
                    result = candidate;
                }
            }
        }

        resultSlots.setItem(0, result);
        menu.setRemoteSlot(RESULT_SLOT, result);
        serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                menu.containerId, menu.incrementStateId(), RESULT_SLOT, result));
    }

    @Override
    public void slotsChanged(Container container) {
        if (!this.placingRecipe) {
            this.access.execute((level, pos) -> {
                if (level instanceof ServerLevel serverLevel) {
                    updateCraftingResult(this, serverLevel, this.player, this.resultSlots, null);
                }
            });
        }
    }

    @Override
    public void beginPlacingRecipe() {
        this.placingRecipe = true;
    }

    @Override
    public void finishPlacingRecipe(ServerLevel level, RecipeHolder<CraftingRecipe> recipe) {
        this.placingRecipe = false;
        updateCraftingResult(this, level, this.player, this.resultSlots, recipe);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.SUPER_CRAFTING_TABLE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        ItemStack clicked = ItemStack.EMPTY;
        if (slotIndex < 0 || slotIndex >= this.slots.size()) {
            return clicked;
        }

        Slot slot = this.slots.get(slotIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            clicked = stack.copy();

            if (slotIndex == RESULT_SLOT) {
                stack.getItem().onCraftedBy(stack, player);
                if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(stack, clicked);
            } else if (slotIndex >= INVENTORY_START) {
                if (!this.moveItemStackTo(stack, INPUT_START, INPUT_END, false)) {
                    if (slotIndex < INVENTORY_START + 27) {
                        if (!this.moveItemStackTo(stack, INVENTORY_START + 27, INVENTORY_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_START + 27, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(stack, INVENTORY_START, INVENTORY_END, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stack.getCount() == clicked.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stack);
            if (slotIndex == RESULT_SLOT) {
                player.drop(stack, false, Prediction.PREDICTED);
            }
        }
        return clicked;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public Slot getResultSlot() {
        return this.slots.get(RESULT_SLOT);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return this.slots.subList(INPUT_START, INPUT_END);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return this.player;
    }
}
