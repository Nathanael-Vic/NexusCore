package com.nexussmp.nexuscore.magic.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModPresence;
import com.nexussmp.nexuscore.magic.MagicFx;
import com.nexussmp.nexuscore.magic.Mages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * {@code /nexus mago ...}: quem sabe magia.
 *
 * <p>Da para nomear e tirar mago de quem esta offline: o argumento aceita o nome de quem ja entrou
 * no servidor alguma vez. Quem estiver online sente na hora — a magia chegando ou indo embora.
 *
 * <p>Esta classe nao toca em nada do Iron's Spells: sem o mod, o comando existe e so avisa.
 */
public final class MagicCommands {
    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("mago")
                .then(Commands.literal("adicionar")
                        .requires(source -> source.hasPermission(Mages.MODERATOR_LEVEL))
                        .then(Commands.argument("jogadores", GameProfileArgument.gameProfile())
                                .executes(context -> add(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "jogadores")))))
                .then(Commands.literal("remover")
                        .requires(source -> source.hasPermission(Mages.MODERATOR_LEVEL))
                        .then(Commands.argument("jogadores", GameProfileArgument.gameProfile())
                                .executes(context -> remove(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "jogadores")))))
                .then(Commands.literal("lista")
                        .requires(source -> source.hasPermission(Mages.MODERATOR_LEVEL))
                        .executes(MagicCommands::list))
                .then(Commands.literal("info")
                        .executes(context -> info(context.getSource(), self(context)))
                        .then(Commands.argument("jogador", GameProfileArgument.gameProfile())
                                .requires(source -> source.hasPermission(Mages.MODERATOR_LEVEL))
                                .executes(context -> info(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "jogador")))));
    }

    /** Os dois avisos que impedem qualquer comando daqui de fazer sentido. */
    private static boolean unavailable(CommandSourceStack source) {
        if (!CommonConfig.isModuleEnabled(ModuleIds.MAGIC)) {
            source.sendFailure(Component.translatable("nexuscore.magic.disabled"));
            return true;
        }
        if (!ModPresence.check(CompatRequirements.IRONS_SPELLBOOKS).ok()) {
            source.sendFailure(Component.translatable("nexuscore.magic.no_mod"));
            return true;
        }
        return false;
    }

    private static int add(CommandSourceStack source, Collection<GameProfile> profiles) {
        if (unavailable(source)) {
            return 0;
        }
        Mages mages = Mages.get(source.getServer());
        int changed = 0;
        for (GameProfile profile : profiles) {
            if (mages.add(profile.getId(), profile.getName())) {
                changed++;
                ServerPlayer online = source.getServer().getPlayerList().getPlayer(profile.getId());
                if (online != null) {
                    Mages.sync(online);
                    MagicFx.awaken(online);
                }
            }
        }
        return report(source, changed, "nexuscore.magic.added");
    }

    private static int remove(CommandSourceStack source, Collection<GameProfile> profiles) {
        if (unavailable(source)) {
            return 0;
        }
        Mages mages = Mages.get(source.getServer());
        int changed = 0;
        for (GameProfile profile : profiles) {
            if (mages.remove(profile.getId())) {
                changed++;
                ServerPlayer online = source.getServer().getPlayerList().getPlayer(profile.getId());
                if (online != null) {
                    // O resto (joias de volta para a mochila, lancamento parado) o proprio modulo faz
                    // no proximo segundo, como para qualquer pessoa que nao e maga.
                    Mages.sync(online);
                    MagicFx.fade(online);
                }
            }
        }
        return report(source, changed, "nexuscore.magic.removed");
    }

    private static int report(CommandSourceStack source, int changed, String key) {
        if (changed == 0) {
            source.sendFailure(Component.translatable("nexuscore.magic.none_changed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable(key, changed), true);
        return changed;
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (unavailable(source)) {
            return 0;
        }
        Collection<String> names = Mages.get(source.getServer()).names();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.magic.list_empty"), false);
            return 0;
        }
        String joined = String.join(", ", names);
        source.sendSuccess(() -> Component.translatable("nexuscore.magic.list", names.size(), joined), false);
        return names.size();
    }

    /** {@code info} sem nome: a propria pessoa. Do console nao ha "propria pessoa". */
    private static Collection<GameProfile> self(CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        return player == null ? List.of() : List.of(player.getGameProfile());
    }

    private static int info(CommandSourceStack source, Collection<GameProfile> profiles) {
        if (unavailable(source)) {
            return 0;
        }
        if (profiles.isEmpty()) {
            source.sendFailure(Component.translatable("nexuscore.magic.needs_player"));
            return 0;
        }
        Mages mages = Mages.get(source.getServer());
        int found = 0;
        for (GameProfile profile : profiles) {
            boolean mage = mages.contains(profile.getId());
            String name = profile.getName();
            source.sendSuccess(() -> Component.translatable(mage ? "nexuscore.magic.info.mage" : "nexuscore.magic.info.common",
                    name), false);
            if (mage) {
                found++;
            }
        }
        return found;
    }

    private MagicCommands() {}
}
