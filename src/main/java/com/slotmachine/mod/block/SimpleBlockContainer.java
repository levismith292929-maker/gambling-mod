package com.slotmachine.mod.block;

import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Minimal Container backed by a NonNullList, with a callback for persistence
 * (BlockEntity#setChanged) and a check for whether a player may keep it open.
 */
public class SimpleBlockContainer implements Container {

    private final NonNullList<ItemStack> items;
    private final Runnable onChange;
    private final Predicate<Player> validity;

    public SimpleBlockContainer(int size, Runnable onChange, Predicate<Player> validity) {
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
        this.onChange = onChange;
        this.validity = validity;
    }

    public NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        int max = Math.min(getMaxStackSize(), stack.getMaxStackSize());
        if (stack.getCount() > max) {
            stack.setCount(max);
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        onChange.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return validity.test(player);
    }

    @Override
    public void clearContent() {
        items.replaceAll(stack -> ItemStack.EMPTY);
        setChanged();
    }
}
