package com.slotmachine.mod.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.slotmachine.mod.block.ReelSymbol;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class ModCommands {

    private static final Map<UUID, PendingRig> PENDING_RIGS = new ConcurrentHashMap<>();
    private static final SimpleCommandExceptionType UNKNOWN_SYMBOL =
            new SimpleCommandExceptionType(Component.literal(
                    "Unknown symbol. Use dirt, copper, iron, gold, diamond, emerald, or command_block."));

    private ModCommands() {
    }

    public static void register() {
        SuggestionProvider<net.minecraft.commands.CommandSourceStack> symbolSuggestions =
                (context, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream(ReelSymbol.values())
                                .map(symbol -> symbol.name().toLowerCase(Locale.ROOT)),
                        builder);

        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, environment) ->
                dispatcher.register(Commands.literal("rig")
                        .requires(source -> source.permissions().hasPermission(
                                net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 5))
                                        .then(Commands.argument("slot1", StringArgumentType.word())
                                                .suggests(symbolSuggestions)
                                                .then(Commands.argument("slot2", StringArgumentType.word())
                                                        .suggests(symbolSuggestions)
                                                        .then(Commands.argument("slot3", StringArgumentType.word())
                                                                .suggests(symbolSuggestions)
                                                                .executes(context -> {
                                                                    ServerPlayer player = EntityArgument.getPlayer(context, "player");
                                                                    int amount = IntegerArgumentType.getInteger(context, "amount");
                                                                    ReelSymbol first = parseSymbol(StringArgumentType.getString(context, "slot1"));
                                                                    ReelSymbol second = parseSymbol(StringArgumentType.getString(context, "slot2"));
                                                                    ReelSymbol third = parseSymbol(StringArgumentType.getString(context, "slot3"));

                                                                    PENDING_RIGS.put(player.getUUID(),
                                                                            new PendingRig(amount, first, second, third));
                                                                    context.getSource().sendSuccess(
                                                                            () -> Component.literal("Rigged " + player.getGameProfile().name()
                                                                                    + "'s next " + amount + " spin(s) to "
                                                                                    + first.name().toLowerCase(Locale.ROOT) + " "
                                                                                    + second.name().toLowerCase(Locale.ROOT) + " "
                                                                                    + third.name().toLowerCase(Locale.ROOT) + "."),
                                                                            true);
                                                                    return 1;
                                                                })))))));
    }

    private static ReelSymbol parseSymbol(String name) throws CommandSyntaxException {
        try {
            return ReelSymbol.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw UNKNOWN_SYMBOL.create();
        }
    }

    /** Takes one queued rigged spin, if the player has one. */
    public static ReelSymbol[] takeRig(UUID playerId) {
        PendingRig pending = PENDING_RIGS.get(playerId);
        if (pending == null) {
            return null;
        }
        if (pending.remaining() <= 1) {
            PENDING_RIGS.remove(playerId, pending);
        } else {
            PENDING_RIGS.replace(playerId, pending,
                    new PendingRig(pending.remaining() - 1, pending.first(), pending.second(), pending.third()));
        }
        return new ReelSymbol[]{pending.first(), pending.second(), pending.third()};
    }

    private record PendingRig(int remaining, ReelSymbol first, ReelSymbol second, ReelSymbol third) {
    }
}
