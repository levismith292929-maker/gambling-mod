package com.slotmachine.mod.item;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** A single-use playing card that triggers one randomized effect when right-clicked. */
public class PlayingCardItem extends Item {
    private static final String[] CLEAR_ITEMS = {
            "minecraft:rotten_flesh", "minecraft:dirt", "minecraft:cobblestone",
            "minecraft:gravel", "minecraft:spider_eye"
    };
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

        int cardIndex = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr("CardIndex", -1);
        int rank = cardIndex >= 0 && cardIndex < 52 ? cardIndex % 13 : -1;
        boolean joker = cardIndex == 52 || cardIndex == 53;
        boolean royal = rank >= 9 && rank <= 11;

        int roll = player.getRandom().nextInt(5);
        Outcome outcome;
        if (joker) {
            outcome = roll < 3 ? Outcome.BAD : roll == 3 ? Outcome.GOOD : Outcome.NEUTRAL;
        } else if (royal) {
            outcome = roll < 3 ? Outcome.NEUTRAL : roll == 3 ? Outcome.GOOD : Outcome.BAD;
        } else {
            int evenRoll = player.getRandom().nextInt(3);
            outcome = evenRoll == 0 ? Outcome.BAD : evenRoll == 1 ? Outcome.GOOD : Outcome.NEUTRAL;
        }

        MinecraftServer server = level.getServer();
        if (server != null) {
            switch (outcome) {
                case GOOD -> applyGood(server, player, level);
                case BAD -> applyBad(server, player, level);
                case NEUTRAL -> applyNeutral(server, player, level);
            }
        }

        String message = switch (outcome) {
            case GOOD -> "Good luck! Your card gave you a reward.";
            case BAD -> "Bad luck! Your card brought trouble.";
            case NEUTRAL -> "Nothing special... just a sound.";
        };
        player.displayClientMessage(Component.literal(message), true);
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }

    private static void applyGood(MinecraftServer server, Player player, Level level) {
        int choice = player.getRandom().nextInt(3);
        if (choice == 0) {
            int amount = 1 + player.getRandom().nextInt(5);
            run(server, player, "give @s minecraft:emerald " + amount);
        } else if (choice == 1) {
            int seconds = 15 + player.getRandom().nextInt(31);
            int amplifier = player.getRandom().nextInt(2);
            run(server, player, "effect give @s minecraft:speed " + seconds + " " + amplifier + " true");
        } else {
            double amount = 0.05 + player.getRandom().nextDouble() * 0.25;
            runAttribute(server, player, amount);
        }
    }

    private static void applyBad(MinecraftServer server, ServerPlayer player, Level level) {
        switch (player.getRandom().nextInt(4)) {
            case 0 -> {
                int seconds = 10 + player.getRandom().nextInt(21);
                int amplifier = player.getRandom().nextInt(2);
                run(server, player, "effect give @s minecraft:slowness " + seconds + " " + amplifier + " true");
            }
            case 1 -> {
                String item = CLEAR_ITEMS[player.getRandom().nextInt(CLEAR_ITEMS.length)];
                run(server, player, "clear @s " + item + " 1");
            }
            case 2 -> {
                int damage = 1 + player.getRandom().nextInt(6);
                run(server, player, "damage @s " + damage + " minecraft:generic");
            }
            default -> {
                double amount = -(0.05 + player.getRandom().nextDouble() * 0.25);
                runAttribute(server, player, amount);
            }
        }
    }

    private static void applyNeutral(MinecraftServer server, ServerPlayer player, Level level) {
        String sound = NEUTRAL_SOUNDS[player.getRandom().nextInt(NEUTRAL_SOUNDS.length)];
        run(server, player, "playsound " + sound + " master @s ~ ~ ~ 1 " +
                (0.8 + player.getRandom().nextDouble() * 0.4));
    }

    private static void runAttribute(MinecraftServer server, ServerPlayer player, double amount) {
        String modifierId = "slotmachine:card_" + UUID.randomUUID().toString().replace("-", "");
        run(server, player, "attribute @s minecraft:movement_speed modifier add " +
                modifierId + " " + String.format(java.util.Locale.ROOT, "%.3f", amount) + " add_value");
    }

    private static void run(MinecraftServer server, Player player, String command) {
        server.getCommands().performPrefixedCommand(
                player.createCommandSourceStack().withPermission(2), command);
    }

    private enum Outcome {
        GOOD, BAD, NEUTRAL
    }
}
