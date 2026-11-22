package com.slotmachine.mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** A red milk item that removes card effects at the cost of permanent max health. */
public class LifeForceItem extends Item {
    private static final int MAX_TRACKED_ATTRIBUTE_EFFECTS = 64;

    public LifeForceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        MinecraftServer server = level.getServer();
        if (server == null) return InteractionResult.PASS;

        run(server, serverPlayer,
                "execute if entity @s[tag=slotmachine_card_effect_speed] run effect clear @s minecraft:speed");
        serverPlayer.removeTag("slotmachine_card_effect_speed");
        run(server, serverPlayer,
                "execute if entity @s[tag=slotmachine_card_effect_slowness] run effect clear @s minecraft:slowness");
        serverPlayer.removeTag("slotmachine_card_effect_slowness");

        for (int index = 0; index < MAX_TRACKED_ATTRIBUTE_EFFECTS; index++) {
            String tag = "slotmachine_card_modifier_" + index;
            String modifierId = "slotmachine:card_" + index;
            run(server, serverPlayer, "execute if entity @s[tag=" + tag +
                    "] run attribute @s minecraft:movement_speed modifier remove " + modifierId);
            serverPlayer.removeTag(tag);
        }

        String penaltyId = "slotmachine:life_force_" +
                java.util.UUID.randomUUID().toString().replace("-", "");
        run(server, serverPlayer, "attribute @s minecraft:max_health modifier add " +
                penaltyId + " -0.5 add_value");

        ItemStack stack = serverPlayer.getItemInHand(hand);
        if (stack.getCount() == 1) {
            serverPlayer.setItemInHand(hand, new ItemStack(Items.BUCKET));
        } else {
            stack.shrink(1);
            ItemStack bucket = new ItemStack(Items.BUCKET);
            if (!serverPlayer.getInventory().add(bucket)) {
                Containers.dropItemStack(level, serverPlayer.getX(), serverPlayer.getY() + 0.5,
                        serverPlayer.getZ(), bucket);
            }
        }

        serverPlayer.sendSystemMessage(
                Component.literal("Card effects removed. Maximum health reduced by 0.5 HP."), true);
        return InteractionResult.SUCCESS;
    }

    private static void run(MinecraftServer server, ServerPlayer player, String command) {
        server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER), command);
    }
}