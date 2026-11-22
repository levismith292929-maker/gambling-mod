package com.slotmachine.mod.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** A single-use playing card that triggers one randomized effect when right-clicked. */
public class PlayingCardItem extends Item {
    private static final int MAX_TRACKED_ATTRIBUTE_EFFECTS = 64;
    private static final String[] NEUTRAL_SOUNDS = {
            "minecraft:block.note_block.pling",
            "minecraft:block.amethyst_block.chime",
            "minecraft:entity.villager.no"
    };

    public PlayingCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        int cardIndex = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("CardIndex", -1);
        int rank = cardIndex >= 0 && cardIndex < 52 ? cardIndex % 13 : -1;
        boolean joker = cardIndex == 52 || cardIndex == 53;
        boolean royal = rank >= 9 && rank <= 11;

        Outcome outcome;
        if (joker) {
            int roll = player.getRandom().nextInt(5);
            outcome = roll < 3 ? Outcome.BAD : roll == 3 ? Outcome.GOOD : Outcome.NEUTRAL;
        } else if (royal) {
            int roll = player.getRandom().nextInt(5);
            outcome = roll < 3 ? Outcome.NEUTRAL : roll == 3 ? Outcome.GOOD : Outcome.BAD;
        } else {
            int roll = player.getRandom().nextInt(3);
            outcome = roll == 0 ? Outcome.BAD : roll == 1 ? Outcome.GOOD : Outcome.NEUTRAL;
        }

        MinecraftServer server = level.getServer();
        if (server != null) {
            if (outcome == Outcome.GOOD) {
                applyGood(server, serverPlayer);
            } else if (outcome == Outcome.BAD) {
                applyBad(server, serverPlayer);
            } else {
                applyNeutral(server, serverPlayer);
            }
        }

        String message = outcome == Outcome.GOOD
                ? "Good luck! Your card gave you a reward."
                : outcome == Outcome.BAD
                ? "Bad luck! Your card brought trouble."
                : "Nothing special... just a sound.";
        serverPlayer.sendSystemMessage(Component.literal(message), true);
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static void applyGood(MinecraftServer server, ServerPlayer player) {
        int choice = player.getRandom().nextInt(3);
        if (choice == 0) {
            // Smaller reward: one or two emeralds instead of one to five.
            int amount = 1 + player.getRandom().nextInt(2);
            run(server, player, "give @s minecraft:emerald " + amount);
        } else if (choice == 1) {
            int seconds = 15 + player.getRandom().nextInt(31);
            int amplifier = player.getRandom().nextInt(2);
            player.addTag("slotmachine_card_effect_speed");
            run(server, player, "effect give @s minecraft:speed " + seconds + " " + amplifier + " true");
        } else {
            double amount = 0.00025 + player.getRandom().nextDouble() * 0.00225;
            runAttribute(server, player, amount);
        }
    }

    private static void applyBad(MinecraftServer server, ServerPlayer player) {
        int choice = player.getRandom().nextInt(4);
        if (choice == 0) {
            int seconds = 10 + player.getRandom().nextInt(21);
            int amplifier = player.getRandom().nextInt(2);
            player.addTag("slotmachine_card_effect_slowness");
            run(server, player, "effect give @s minecraft:slowness " + seconds + " " + amplifier + " true");
        } else if (choice == 1) {
            clearOneOwnedItem(player);
        } else if (choice == 2) {
            int damage = 1 + player.getRandom().nextInt(6);
            run(server, player, "damage @s " + damage + " minecraft:generic");
        } else {
            double amount = -(0.00025 + player.getRandom().nextDouble() * 0.00225);
            runAttribute(server, player, amount);
        }
    }

    /** Selects only from eligible item stacks actually present in the player's inventory. */
    private static void clearOneOwnedItem(ServerPlayer player) {
        Items[] ignored = new Items[0]; // Keeps this method's choices explicitly item-based below.
        net.minecraft.world.item.Item[] choices = {
                Items.ROTTEN_FLESH, Items.DIRT, Items.COBBLESTONE, Items.GRAVEL, Items.SPIDER_EYE
        };
        java.util.ArrayList<Integer> eligibleSlots = new java.util.ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            for (net.minecraft.world.item.Item item : choices) {
                if (stack.is(item)) {
                    eligibleSlots.add(slot);
                    break;
                }
            }
        }
        if (!eligibleSlots.isEmpty()) {
            int slot = eligibleSlots.get(player.getRandom().nextInt(eligibleSlots.size()));
            player.getInventory().removeItem(slot, 1);
        }
    }

    private static void applyNeutral(MinecraftServer server, ServerPlayer player) {
        String sound = NEUTRAL_SOUNDS[player.getRandom().nextInt(NEUTRAL_SOUNDS.length)];
        run(server, player, "playsound " + sound + " master @s ~ ~ ~ 1 " +
                (0.8 + player.getRandom().nextDouble() * 0.4));
    }

    /** Uses numbered IDs so Life Force can reliably remove every card modifier. */
    private static void runAttribute(MinecraftServer server, ServerPlayer player, double amount) {
        for (int index = 0; index < MAX_TRACKED_ATTRIBUTE_EFFECTS; index++) {
            String tag = "slotmachine_card_modifier_" + index;
            String modifierId = "slotmachine:card_" + index;
            String command = "execute unless entity @s[tag=" + tag + "] run attribute @s " +
                    "minecraft:movement_speed modifier add " + modifierId + " " +
                    String.format(java.util.Locale.ROOT, "%.5f", amount) + " add_value";
            if (run(server, player, command) > 0) {
                player.addTag(tag);
                return;
            }
        }
    }

    private static int run(MinecraftServer server, ServerPlayer player, String command) {
        return server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(LevelBasedPermissionSet.GAMEMASTER), command);
    }

    private enum Outcome {
        GOOD, BAD, NEUTRAL
    }
}