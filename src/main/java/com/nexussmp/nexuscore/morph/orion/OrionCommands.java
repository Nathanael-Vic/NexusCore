package com.nexussmp.nexuscore.morph.orion;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * {@code /nexus orion ...}: so operador.
 *
 * <ul>
 *   <li>{@code virar [jogadores]} — vira o ORION (sem alvo, quem digitou).</li>
 *   <li>{@code desvirar [jogadores]} — volta ao normal.</li>
 * </ul>
 * A forma nao sai sozinha: continua depois de morrer e de sair e voltar ao servidor, e vale acima de
 * qualquer anel que a pessoa esteja vestindo. So o {@code desvirar} tira.
 */
public final class OrionCommands {
    private static final int OPERATOR_LEVEL = 2;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("orion")
                .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                .then(Commands.literal("virar")
                        .executes(context -> set(context.getSource(), List.of(context.getSource().getPlayerOrException()), true))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> set(context.getSource(), EntityArgument.getPlayers(context, "jogadores"), true))))
                .then(Commands.literal("desvirar")
                        .executes(context -> set(context.getSource(), List.of(context.getSource().getPlayerOrException()), false))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> set(context.getSource(), EntityArgument.getPlayers(context, "jogadores"), false))));
    }

    private static int set(CommandSourceStack source, Collection<ServerPlayer> players, boolean orion)
            throws CommandSyntaxException {
        int changed = 0;
        for (ServerPlayer player : players) {
            if (OrionMorph.forced(player) != orion) {
                OrionMorph.setForced(player, orion);
                changed++;
            }
        }
        int count = changed;
        String what = orion ? "virou o ORION" : "voltou ao normal";
        if (count == 0) {
            source.sendFailure(Component.literal(orion ? "Ja esta como ORION." : "Nao esta como ORION."));
        } else if (players.size() == 1) {
            String name = players.iterator().next().getGameProfile().getName();
            source.sendSuccess(() -> Component.literal(name + " " + what + ".").withStyle(ChatFormatting.AQUA), true);
        } else {
            source.sendSuccess(() -> Component.literal(count + " jogadores: " + what + ".").withStyle(ChatFormatting.AQUA), true);
        }
        return count;
    }

    private OrionCommands() {}
}
