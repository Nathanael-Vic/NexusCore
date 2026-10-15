package com.nexussmp.nexuscore.vampire.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.vampire.VampireFx;
import com.nexussmp.nexuscore.vampire.VampireLords;
import com.nexussmp.nexuscore.vampire.Vampires;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * {@code /nexus vampiro ...}: quem progride, quem vira vampiro e quem volta a ser gente.
 *
 * <p>Os dois primeiros comandos sao de coisas diferentes, e de proposito. <b>Lorde</b> e
 * permissao — quem pode subir de nivel. <b>Tornar</b> e estado — quem esta com a condicao. Um nao
 * implica o outro: da para nomear um lorde antes de ele ser mordido, e da para ter vampiro comum
 * que nunca sera lorde.
 *
 * <p>{@code tornar} existe porque, com as regras deste servidor, nao ha outra porta de entrada: nao
 * nasce vampiro no mundo e mordida de bicho nao transforma. A primeira pessoa a virar vampira e
 * sempre por aqui; dali para a frente, a mordida de um lorde faz o resto.
 */
public final class VampireCommands {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("vampiro")
                .then(Commands.literal("lorde")
                        .requires(source -> source.hasPermission(Vampires.MODERATOR_LEVEL))
                        .then(Commands.literal("adicionar")
                                .then(Commands.argument("jogadores", EntityArgument.players())
                                        .executes(context -> addLords(context.getSource(),
                                                EntityArgument.getPlayers(context, "jogadores")))))
                        .then(Commands.literal("remover")
                                .then(Commands.argument("jogadores", EntityArgument.players())
                                        .executes(context -> removeLords(context.getSource(),
                                                EntityArgument.getPlayers(context, "jogadores")))))
                        .then(Commands.literal("lista").executes(VampireCommands::listLords)))
                .then(Commands.literal("tornar")
                        .requires(source -> source.hasPermission(Vampires.MODERATOR_LEVEL))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> turn(context.getSource(),
                                        EntityArgument.getPlayers(context, "jogadores")))))
                .then(Commands.literal("curar")
                        .requires(source -> source.hasPermission(Vampires.MODERATOR_LEVEL))
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> cure(context.getSource(),
                                        EntityArgument.getPlayers(context, "jogadores")))))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource(), context.getSource().getPlayer()))
                        .then(Commands.argument("jogador", EntityArgument.player())
                                .requires(source -> source.hasPermission(Vampires.MODERATOR_LEVEL))
                                .executes(context -> info(context.getSource(),
                                        EntityArgument.getPlayer(context, "jogador")))));
    }

    /** Uma porta so para os dois avisos que impedem qualquer comando daqui de fazer sentido. */
    private static boolean unavailable(CommandSourceStack source) {
        if (!CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE)) {
            source.sendFailure(Component.translatable("nexuscore.vampire.disabled"));
            return true;
        }
        if (!Vampires.installed()) {
            source.sendFailure(Component.translatable("nexuscore.vampire.no_mod"));
            return true;
        }
        return false;
    }

    // ---- lordes ----

    private static int addLords(CommandSourceStack source, Collection<ServerPlayer> players) {
        if (unavailable(source)) {
            return 0;
        }
        VampireLords lords = VampireLords.get(source.getServer());
        int changed = 0;
        for (ServerPlayer player : players) {
            if (lords.add(player)) {
                changed++;
                Vampires.sync(player);
                // A mensagem vem no fim da coroacao, junto com o titulo.
                VampireFx.crown(player);
            }
        }
        int total = changed;
        source.sendSuccess(() -> Component.translatable("nexuscore.vampire.lord_added", total), true);
        return changed;
    }

    private static int removeLords(CommandSourceStack source, Collection<ServerPlayer> players) {
        if (unavailable(source)) {
            return 0;
        }
        VampireLords lords = VampireLords.get(source.getServer());
        int changed = 0;
        for (ServerPlayer player : players) {
            if (lords.remove(player.getUUID())) {
                changed++;
                // Sair do posto tem preco: o nivel volta ao de todo mundo e as habilidades somem.
                Vampires.demote(player);
                Vampires.sync(player);
                VampireFx.fade(player);
            }
        }
        int total = changed;
        source.sendSuccess(() -> Component.translatable("nexuscore.vampire.lord_removed", total), true);
        return changed;
    }

    private static int listLords(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (unavailable(source)) {
            return 0;
        }
        Collection<String> names = VampireLords.get(source.getServer()).names();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.vampire.lord_list_empty"), false);
            return 0;
        }
        String joined = String.join(", ", names);
        source.sendSuccess(() -> Component.translatable("nexuscore.vampire.lord_list", names.size(), joined), false);
        return names.size();
    }

    // ---- a condicao ----

    private static int turn(CommandSourceStack source, Collection<ServerPlayer> players) {
        if (unavailable(source)) {
            return 0;
        }
        int changed = 0;
        for (ServerPlayer player : players) {
            if (Vampires.turn(player)) {
                changed++;
                player.sendSystemMessage(Component.translatable("nexuscore.vampire.you_turned"));
            }
        }
        int total = changed;
        source.sendSuccess(() -> Component.translatable("nexuscore.vampire.turned", total), true);
        return changed;
    }

    private static int cure(CommandSourceStack source, Collection<ServerPlayer> players) {
        if (unavailable(source)) {
            return 0;
        }
        int changed = 0;
        for (ServerPlayer player : players) {
            if (Vampires.cure(player)) {
                changed++;
                player.sendSystemMessage(Component.translatable("nexuscore.vampire.you_cured"));
            }
        }
        int total = changed;
        source.sendSuccess(() -> Component.translatable("nexuscore.vampire.cured", total), true);
        return changed;
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        if (unavailable(source)) {
            return 0;
        }
        if (player == null) {
            source.sendFailure(Component.translatable("nexuscore.vampire.needs_player"));
            return 0;
        }
        String name = player.getGameProfile().getName();
        boolean lord = Vampires.isLord(source.getServer(), player);
        int level = Vampires.level(player);
        source.sendSuccess(() -> Component.translatable("nexuscore.vampire.info", name,
                Component.translatable(level > 0 ? "nexuscore.vampire.info.vampire" : "nexuscore.vampire.info.human"),
                level,
                Component.translatable(lord ? "nexuscore.vampire.info.lord" : "nexuscore.vampire.info.common")), false);
        return 1;
    }

    private VampireCommands() {}
}
