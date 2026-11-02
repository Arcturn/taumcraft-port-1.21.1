package thaumcraft.port.warp;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import thaumcraft.port.research.ResearchProgress;
import thaumcraft.port.research.ResearchStorage;

/** Explicit operator controls for inspecting and reproducing individual warp events. */
public final class WarpCommands {
    public static void register(RegisterCommandsEvent event) {
        var warp = Commands.literal("warp").requires(source -> source.hasPermission(2))
                .executes(context -> query(context.getSource(), List.of(context.getSource().getPlayerOrException())))
                .then(Commands.literal("get").then(Commands.argument("targets", EntityArgument.players())
                        .executes(context -> query(context.getSource(), EntityArgument.getPlayers(context, "targets")))));
        for (boolean add : new boolean[]{false, true}) {
            var targets = Commands.argument("targets", EntityArgument.players());
            for (String type : List.of("permanent", "normal", "temporary")) {
                targets.then(Commands.literal(type).then(Commands.argument("amount", IntegerArgumentType.integer(add ? -10000 : 0, 10000))
                        .executes(context -> change(context.getSource(), EntityArgument.getPlayers(context, "targets"), type,
                                IntegerArgumentType.getInteger(context, "amount"), add))));
            }
            warp.then(Commands.literal(add ? "add" : "set").then(targets));
        }
        warp.then(Commands.literal("event").then(Commands.argument("targets", EntityArgument.players())
                .then(Commands.argument("severity", IntegerArgumentType.integer(0, 200))
                        .executes(context -> trigger(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                IntegerArgumentType.getInteger(context, "severity"), 100))
                        .then(Commands.argument("strength", IntegerArgumentType.integer(1, 100))
                                .executes(context -> trigger(context.getSource(), EntityArgument.getPlayers(context, "targets"),
                                        IntegerArgumentType.getInteger(context, "severity"), IntegerArgumentType.getInteger(context, "strength")))))));
        event.getDispatcher().register(Commands.literal("thaumcraft").then(warp));
    }
    private static int query(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (var player : players) {
            var state = ResearchStorage.get(player.getServer()).player(player.getUUID());
            source.sendSuccess(() -> Component.translatable("command.thaumcraft.warp.status", player.getName(),
                    state.permanentWarp(), state.normalWarp(), state.temporaryWarp(), state.warpCounter()), false);
        }
        return players.size();
    }
    private static int change(CommandSourceStack source, Collection<ServerPlayer> players, String type, int amount, boolean add) {
        for (var player : players) {
            var storage = ResearchStorage.get(player.getServer());
            var state = storage.player(player.getUUID());
            int permanent = state.permanentWarp(), normal = state.normalWarp(), temporary = state.temporaryWarp();
            switch (type) {
                case "permanent" -> permanent = Math.clamp((add ? permanent : 0) + amount, 0, 10000);
                case "normal" -> normal = Math.clamp((add ? normal : 0) + amount, 0, 10000);
                case "temporary" -> temporary = Math.clamp((add ? temporary : 0) + amount, 0, 10000);
                default -> throw new IllegalArgumentException(type);
            }
            state.warp(permanent, normal, temporary);
            storage.setDirty();
            ResearchProgress.sync(player);
        }
        return query(source, players);
    }
    private static int trigger(CommandSourceStack source, Collection<ServerPlayer> players, int severity, int strength) {
        for (var player : players) {
            // Deliberately bypass chance/ward for an explicit operator reproduction; do not grant research.
            WarpPayload.send(player, 0);
            WarpEvents.dispatch(player, severity, strength);
        }
        source.sendSuccess(() -> Component.translatable("command.thaumcraft.warp.event", severity, strength, players.size()), true);
        return players.size();
    }
    private WarpCommands() {}
}
