package com.nexussmp.nexuscore.parrot.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.parrot.ParrotColor;
import com.nexussmp.nexuscore.parrot.ParrotKeeper;
import com.nexussmp.nexuscore.parrot.ParrotOwners;
import com.nexussmp.nexuscore.parrot.voice.ParrotBrain;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@code /nexus papagaio ...}: por, tirar e listar os papagaios de ombro. So operador.
 *
 * <ul>
 *   <li>{@code colocar <jogadores> <cor> <nome>} — o nome e o resto da linha, com espacos. Quem ja
 *       tinha papagaio troca: o velho encolhe enquanto o novo chega.</li>
 *   <li>{@code tirar <jogadores>} — funciona com gente offline tambem; a memoria dele vai junto.</li>
 *   <li>{@code esquecer <jogadores>} — o papagaio fica, mas esquece tudo o que ouviu.</li>
 *   <li>{@code lista}</li>
 * </ul>
 */
public final class ParrotCommands {
    private static final int OPERATOR_LEVEL = 2;
    /** O nome aparece numa plaquinha do Jade: mais que isso nao cabe bonito. */
    private static final int MAX_NAME_LENGTH = 32;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> targets =
                Commands.argument("jogadores", EntityArgument.players());
        // Uma palavra por cor, em vez de texto livre: o autocompletar mostra as cinco.
        for (ParrotColor color : ParrotColor.values()) {
            targets.then(Commands.literal(color.id())
                    .then(Commands.argument("nome", StringArgumentType.greedyString())
                            .executes(context -> give(context.getSource(),
                                    EntityArgument.getPlayers(context, "jogadores"), color,
                                    StringArgumentType.getString(context, "nome")))));
        }
        return Commands.literal("papagaio")
                .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                .then(Commands.literal("colocar").then(targets))
                .then(Commands.literal("tirar")
                        .then(Commands.argument("jogadores", GameProfileArgument.gameProfile())
                                .executes(context -> take(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "jogadores")))))
                .then(Commands.literal("esquecer")
                        .then(Commands.argument("jogadores", GameProfileArgument.gameProfile())
                                .executes(context -> forget(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "jogadores")))))
                .then(Commands.literal("lista").executes(context -> list(context.getSource())));
    }

    private static int give(CommandSourceStack source, Collection<ServerPlayer> players, ParrotColor color, String rawName) {
        if (!CommonConfig.isModuleEnabled(ModuleIds.PARROT)) {
            source.sendFailure(Component.translatable("nexuscore.parrot.disabled"));
            return 0;
        }
        String name = rawName.strip();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) {
            source.sendFailure(Component.translatable("nexuscore.parrot.bad_name", MAX_NAME_LENGTH));
            return 0;
        }
        List<String> names = new ArrayList<>();
        for (ServerPlayer player : players) {
            ParrotKeeper.give(player, color, name);
            names.add(player.getGameProfile().getName());
            player.displayClientMessage(Component.translatable("nexuscore.parrot.landed", name)
                    .withStyle(ChatFormatting.GREEN), true);
        }
        String joined = String.join(", ", names);
        source.sendSuccess(() -> Component.translatable("nexuscore.parrot.given", name,
                Component.translatable("nexuscore.parrot.color." + color.id()), joined), true);
        return names.size();
    }

    private static int take(CommandSourceStack source, Collection<GameProfile> profiles) {
        List<String> names = new ArrayList<>();
        for (GameProfile profile : profiles) {
            if (ParrotKeeper.take(source.getServer(), profile.getId())) {
                names.add(profile.getName());
            }
        }
        if (names.isEmpty()) {
            source.sendFailure(Component.translatable("nexuscore.parrot.taken_none"));
            return 0;
        }
        String joined = String.join(", ", names);
        source.sendSuccess(() -> Component.translatable("nexuscore.parrot.taken", joined), true);
        return names.size();
    }

    private static int forget(CommandSourceStack source, Collection<GameProfile> profiles) {
        List<String> names = new ArrayList<>();
        ParrotOwners owners = ParrotOwners.get(source.getServer());
        for (GameProfile profile : profiles) {
            ParrotBrain.forget(profile.getId());
            if (owners.get(profile.getId()) != null) {
                names.add(profile.getName());
            }
        }
        if (names.isEmpty()) {
            source.sendFailure(Component.translatable("nexuscore.parrot.taken_none"));
            return 0;
        }
        String joined = String.join(", ", names);
        source.sendSuccess(() -> Component.translatable("nexuscore.parrot.forgot", joined), true);
        return names.size();
    }

    private static int list(CommandSourceStack source) {
        Collection<ParrotOwners.Perch> perches = ParrotOwners.get(source.getServer()).all();
        if (perches.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.parrot.list_empty"), false);
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("nexuscore.parrot.list", perches.size()), false);
        for (ParrotOwners.Perch perch : perches) {
            source.sendSuccess(() -> Component.translatable("nexuscore.parrot.list_entry", perch.ownerName(), perch.name(),
                    Component.translatable("nexuscore.parrot.color." + perch.color().id())), false);
        }
        return perches.size();
    }

    private ParrotCommands() {}
}
