package com.slotmachine.mod.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Removes the lasting effects created by playing cards. */
public class LifeForceItem extends Item {
    private static final int MAX_TRACKED_ATTRIBUTE_EFFECTS = 64;

    public LifeForceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        // Only clear potion effects when a playing card marked them as card effects.
        if (serverPlayer.entityTags().contains("slotmachine_card_effect_speed")) {
            serverPlayer.removeEffect(MobEffects.SPEED);
            serverPlayer.removeTag("slotmachine_card_effect_speed");
        }
        if (serverPlayer.entityTags().contains("slotmachine_card_effect_slowness")) {
            serverPlayer.removeEffect(MobEffects.SLOWNESS);
            serverPlayer.removeTag("slotmachine_card_effect_slowness");
        }

        // Remove every movement-speed modifier created by PlayingCardItem.
        AttributeInstance movementSpeed = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
        for (int index = 0; index < MAX_TRACKED_ATTRIBUTE_EFFECTS; index++) {
            Identifier modifierId = Identifier.fromNamespaceAndPath("slotmachine", "card_" + index);
            if (movementSpeed != null) {
                movementSpeed.removeModifier(modifierId);
            }
            serverPlayer.removeTag("slotmachine_card_modifier_" + index);
        }

        // Consume one Life Force and return an empty bucket. No health penalty in this first pass.
        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (stack.getCount() == 1) {
            serverPlayer.setItemInHand(hand, new ItemStack(Items.BUCKET));
        } else {
            stack.shrink(1);
            ItemStack bucket = new ItemStack(Items.BUCKET);
            if (!serverPlayer.getInventory().add(bucket)) {
                net.minecraft.world.Containers.dropItemStack(level, serverPlayer.getX(),
                        serverPlayer.getY() + 0.5, serverPlayer.getZ(), bucket);
            }
        }

        serverPlayer.sendSystemMessage(Component.literal("Card effects removed."), true);
        return InteractionResult.SUCCESS;
    }
}
