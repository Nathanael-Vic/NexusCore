package com.nexussmp.nexuscore.radio.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.radio.BroadcastSession;
import com.nexussmp.nexuscore.radio.RadioHosts;
import com.nexussmp.nexuscore.radio.RadioManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * {@code /nexus radio ...}: abrir o ar, fechar o ar, entrar, sair e nomear locutor.
 *
 * <p>Quem abre o ar e <b>locutor</b> — nomeado pela moderacao — ou a propria moderacao. Ouvir e
 * sair sao de todo mundo, e e o mesmo caminho que o botao do chat e o da tela de pausa usam.
 */
public final class RadioCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("radio")
                .then(Commands.literal("iniciar")
                        // A permissao e conferida de novo dentro do comando; aqui ela so decide o
                        // que aparece no completar do jogador.
                        .requires(RadioCommands::mayBroadcast)
                        .then(Commands.argument("titulo", StringArgumentType.greedyString())
                                .executes(RadioCommands::start)))
                .then(Commands.literal("parar")
                        .requires(RadioCommands::mayBroadcast)
                        .executes(RadioCommands::stop))
                .then(Commands.literal("ouvir").executes(RadioCommands::listen))
                .then(Commands.literal("sair").executes(RadioCommands::leave))
                .then(Commands.literal("info").executes(RadioCommands::info))
                .then(Commands.literal("avisar")
                        .requires(source -> source.hasPermission(RadioManager.MODERATOR_LEVEL))
                        .executes(context -> announce(context.getSource(),
                                List.of(context.getSource().getPlayerOrException())))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> announce(context.getSource(),
                                        EntityArgument.getPlayers(context, "jogadores")))))
                .then(Commands.literal("locutor")
                        .requires(source -> source.hasPermission(RadioManager.MODERATOR_LEVEL))
                        .then(Commands.literal("adicionar")
                                .then(Commands.argument("jogadores", EntityArgument.players())
                                        .executes(context -> addHosts(context.getSource(),
                                                EntityArgument.getPlayers(context, "jogadores")))))
                        .then(Commands.literal("remover")
                                .then(Commands.argument("jogadores", EntityArgument.players())
                                        .executes(context -> removeHosts(context.getSource(),
                                                EntityArgument.getPlayers(context, "jogadores")))))
                        .then(Commands.literal("lista").executes(RadioCommands::listHosts)));
    }

    private static boolean mayBroadcast(CommandSourceStack source) {
        if (source.hasPermission(RadioManager.MODERATOR_LEVEL)) {
            return true;
        }
        ServerPlayer player = source.getPlayer();
        return player != null && RadioManager.mayBroadcast(player);
    }

    // ---- o ar ----

    private static int start(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer presenter = source.getPlayer();
        if (presenter == null) {
            source.sendFailure(Component.translatable("nexuscore.radio.needs_player"));
            return 0;
        }
        if (!CommonConfig.isModuleEnabled(ModuleIds.RADIO)) {
            source.sendFailure(Component.translatable("nexuscore.radio.disabled"));
            return 0;
        }
        if (!RadioManager.mayBroadcast(presenter)) {
            source.sendFailure(Component.translatable("nexuscore.radio.not_host"));
            return 0;
        }
        if (RadioManager.live() != null) {
            source.sendFailure(Component.translatable("nexuscore.radio.already_live"));
            return 0;
        }
        // Pelo comando o nome no ar e o nick; para escolher outro, a tela de abrir tem o campo.
        BroadcastSession session = RadioManager.start(presenter,
                StringArgumentType.getString(context, "titulo"), "");
        if (session == null) {
            source.sendFailure(Component.translatable("nexuscore.radio.empty_title"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.started", session.title()), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BroadcastSession session = RadioManager.live();
        if (session == null) {
            source.sendFailure(Component.translatable("nexuscore.radio.none"));
            return 0;
        }
        // Locutor fecha o proprio programa; o ar dos outros so a moderacao fecha.
        ServerPlayer player = source.getPlayer();
        boolean ownAir = player != null && session.presenterId().equals(player.getUUID());
        if (!ownAir && !source.hasPermission(RadioManager.MODERATOR_LEVEL)) {
            source.sendFailure(Component.translatable("nexuscore.radio.not_yours"));
            return 0;
        }
        RadioManager.stop(source.getServer());
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.stopped"), true);
        return 1;
    }

    private static int listen(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        BroadcastSession session = RadioManager.live();
        if (player == null) {
            source.sendFailure(Component.translatable("nexuscore.radio.needs_player"));
            return 0;
        }
        if (session == null || !RadioManager.listen(player, session.id())) {
            source.sendFailure(Component.translatable("nexuscore.radio.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.listening", session.title()), false);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("nexuscore.radio.needs_player"));
            return 0;
        }
        if (!RadioManager.leave(player)) {
            source.sendFailure(Component.translatable("nexuscore.radio.none"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.left"), false);
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        BroadcastSession session = RadioManager.live();
        if (session == null) {
            source.sendSuccess(() -> Component.translatable("nexuscore.radio.none"), false);
            return 0;
        }
        int minutes = session.elapsedSeconds(System.currentTimeMillis()) / 60;
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.info", session.title(),
                session.presenterName(), minutes, session.listenerCount()), false);
        return 1;
    }

    /**
     * Manda o aviso da transmissao de novo, para quem for pedido (sem argumento, para si mesmo).
     *
     * <p>Serve para testar o aviso sem depender de abrir uma transmissao nova a cada tentativa. Nao
     * substitui nada: quando alguem abre o ar, o aviso ja sai sozinho para o servidor inteiro.
     */
    private static int announce(CommandSourceStack source, Collection<ServerPlayer> players)
            throws CommandSyntaxException {
        if (RadioManager.live() == null) {
            source.sendFailure(Component.translatable("nexuscore.radio.none"));
            return 0;
        }
        int sent = 0;
        for (ServerPlayer player : players) {
            if (RadioManager.reannounce(player)) {
                sent++;
            }
        }
        int total = sent;
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.announced", total), false);
        return total;
    }

    // ---- locutores ----

    private static int addHosts(CommandSourceStack source, Collection<ServerPlayer> players) {
        RadioHosts hosts = RadioHosts.get(source.getServer());
        int added = 0;
        for (ServerPlayer player : players) {
            if (hosts.add(player)) {
                added++;
                player.sendSystemMessage(Component.translatable("nexuscore.radio.host.you_are"));
            }
            // O botao da pausa e o comando aparecem para ela agora, sem precisar relogar.
            RadioManager.refresh(player);
        }
        int total = added;
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.host.added", total), true);
        return total;
    }

    private static int removeHosts(CommandSourceStack source, Collection<ServerPlayer> players) {
        RadioHosts hosts = RadioHosts.get(source.getServer());
        int removed = 0;
        for (ServerPlayer player : players) {
            if (hosts.remove(player.getUUID())) {
                removed++;
            }
            RadioManager.refresh(player);
        }
        int total = removed;
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.host.removed", total), true);
        return total;
    }

    private static int listHosts(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Collection<String> names = RadioHosts.get(source.getServer()).names();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.radio.host.none"), false);
            return 0;
        }
        String joined = String.join(", ", names);
        source.sendSuccess(() -> Component.translatable("nexuscore.radio.host.list", names.size(), joined), false);
        return names.size();
    }

    private RadioCommands() {}
}
