package com.slotmachine.mod.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class LuckCharmItem extends Item {
    private final int displayedLuck;
    private final int tier;

    public LuckCharmItem(Properties properties, int displayedLuck, int tier) {
        super(properties);
        this.displayedLuck = displayedLuck;
        this.tier = tier;
    }

    public int getTier() { return tier; }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("+" + displayedLuck + "% Luck").withStyle(ChatFormatting.GRAY));
    }
}
