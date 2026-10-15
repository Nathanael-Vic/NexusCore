package com.nexussmp.nexuscore.backrooms.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.nexussmp.nexuscore.backrooms.BackroomsArea;
import com.nexussmp.nexuscore.backrooms.BackroomsData;
import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.backrooms.BackroomsManager;
import com.nexussmp.nexuscore.backrooms.BackroomsSettings;
import com.nexussmp.nexuscore.backrooms.SkinwalkerDirector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/** {@code /nexus backrooms ...}: gerar o andar, mandar alguem para la e trazer de volta. */
public final class BackroomsCommands {
    private static final int OPERATOR_LEVEL = 2;
    private static final int MIN_SIZE = 100;
    private static final int MAX_SIZE = 700;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("backrooms")
                .requires(source -> source.hasPermission(OPERATOR_LEVEL))
                .then(Commands.literal("gerar")
                        .executes(BackroomsCommands::generate)
                        .then(Commands.argument("tamanho", IntegerArgumentType.integer(MIN_SIZE, MAX_SIZE))
                                .executes(BackroomsCommands::generate)
                                .then(Commands.argument("portas", IntegerArgumentType.integer(0, 32))
                                        .executes(BackroomsCommands::generate)
                                        .then(Commands.argument("teto", BoolArgumentType.bool())
                                                .executes(BackroomsCommands::generate)
                                                .then(Commands.argument("semente", LongArgumentType.longArg())
                                                        .executes(BackroomsCommands::generate))))))
                .then(Commands.literal("enviar")
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> send(context.getSource(),
                                        EntityArgument.getPlayers(context, "jogadores")))))
                .then(Commands.literal("voltar")
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> bringBack(context.getSource(),
                                        EntityArgument.getPlayers(context, "jogadores")))))
                .then(Commands.literal("teto")
                        .then(Commands.literal("ligar").executes(context -> ceiling(context.getSource(), true)))
                        .then(Commands.literal("desligar").executes(context -> ceiling(context.getSource(), false))))
                .then(Commands.literal("skinwalker")
                        .then(Commands.argument("jogadores", EntityArgument.players())
                                .executes(context -> skinwalker(context.getSource(),
                                        EntityArgument.getPlayers(context, "jogadores")))))
                .then(Commands.literal("limpar").executes(context -> clearHeads(context.getSource())))
                .then(Commands.literal("info").executes(context -> info(context.getSource())));
    }

    /** {@code null} quando a opcao nao foi escrita. */
    @Nullable
    private static <T> T optional(CommandContext<CommandSourceStack> context, String name, Class<T> type) {
        try {
            return context.getArgument(name, type);
        } catch (IllegalArgumentException absent) {
            return null;
        }
    }

    private static int generate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!BackroomsSettings.enabled()) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.disabled"));
            return 0;
        }
        ServerLevel backrooms = source.getServer().getLevel(BackroomsDimensions.BACKROOMS);
        if (backrooms == null) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.no_dimension"));
            return 0;
        }
        if (BackroomsManager.busy()) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.busy"));
            return 0;
        }

        Integer size = optional(context, "tamanho", Integer.class);
        Integer doors = optional(context, "portas", Integer.class);
        Boolean ceiling = optional(context, "teto", Boolean.class);
        Long seed = optional(context, "semente", Long.class);

        // Dado de dentro, o comando refaz o andar onde a pessoa esta, no mesmo lugar. De fora, abre
        // um andar novo num canto livre.
        ServerPlayer player = source.getPlayer();
        BackroomsArea replaced = player != null && BackroomsManager.isInBackrooms(player)
                ? BackroomsData.get(source.getServer()).areaAt(player.getBlockX(), player.getBlockZ())
                : null;
        BackroomsArea area = BackroomsManager.generate(backrooms,
                size == null ? BackroomsSettings.defaultSize() : size,
                BackroomsSettings.cellSize(),
                doors == null ? BackroomsSettings.defaultDoors() : doors,
                ceiling == null ? BackroomsSettings.defaultCeiling() : ceiling,
                seed == null ? backrooms.getRandom().nextLong() : seed,
                player, replaced);
        source.sendSuccess(() -> Component.translatable("nexuscore.backrooms.generate.started",
                area.size(), area.seed()), true);
        return 1;
    }

    private static int send(CommandSourceStack source, Collection<ServerPlayer> players) {
        if (BackroomsData.get(source.getServer()).newest() == null) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.none"));
            return 0;
        }
        int sent = 0;
        for (ServerPlayer player : players) {
            if (BackroomsManager.startFall(player)) {
                sent++;
            }
        }
        int total = sent;
        source.sendSuccess(() -> Component.translatable("nexuscore.backrooms.sent", total), true);
        return sent;
    }

    private static int bringBack(CommandSourceStack source, Collection<ServerPlayer> players) {
        int count = 0;
        for (ServerPlayer player : players) {
            // Quem nunca caiu fica onde esta: nao ha de onde voltar.
            if (BackroomsManager.involved(player) && BackroomsManager.bringBack(player)) {
                count++;
            }
        }
        int total = count;
        source.sendSuccess(() -> Component.translatable("nexuscore.backrooms.returned", total), true);
        return count;
    }

    /** Chama o skinwalker agora, sem esperar o tempo de solidao. Serve para testar o susto. */
    private static int skinwalker(CommandSourceStack source, Collection<ServerPlayer> players) {
        ServerLevel backrooms = source.getServer().getLevel(BackroomsDimensions.BACKROOMS);
        if (backrooms == null) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.no_dimension"));
            return 0;
        }
        int summoned = 0;
        for (ServerPlayer player : players) {
            if (BackroomsManager.isInBackrooms(player) && SkinwalkerDirector.summon(backrooms, player)) {
                summoned++;
            }
        }
        int total = summoned;
        source.sendSuccess(() -> Component.translatable("nexuscore.backrooms.skinwalker.summoned", total), true);
        return summoned;
    }

    /** Tira do chao as cabecas de quem foi pego. Refazer um andar tambem leva as dele. */
    private static int clearHeads(CommandSourceStack source) {
        ServerLevel backrooms = source.getServer().getLevel(BackroomsDimensions.BACKROOMS);
        BackroomsData data = BackroomsData.get(source.getServer());
        if (backrooms == null) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.no_dimension"));
            return 0;
        }
        int removed = 0;
        for (BlockPos head : List.copyOf(data.heads())) {
            if (backrooms.getBlockState(head).is(Blocks.PLAYER_HEAD)) {
                backrooms.removeBlock(head, false);
                removed++;
            }
        }
        data.forgetHeads();
        int total = removed;
        source.sendSuccess(() -> Component.translatable("nexuscore.backrooms.heads_cleared", total), true);
        return removed;
    }

    private static int ceiling(CommandSourceStack source, boolean enabled) {
        ServerLevel backrooms = source.getServer().getLevel(BackroomsDimensions.BACKROOMS);
        BackroomsData data = BackroomsData.get(source.getServer());
        BackroomsArea area = area(source, data);
        if (backrooms == null || area == null) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.none"));
            return 0;
        }
        if (BackroomsManager.busy()) {
            source.sendFailure(Component.translatable("nexuscore.backrooms.busy"));
            return 0;
        }
        area.setCeiling(enabled);
        BackroomsManager.rebuildCeiling(backrooms, area, source.getPlayer());
        source.sendSuccess(() -> Component.translatable(enabled
                ? "nexuscore.backrooms.ceiling.on" : "nexuscore.backrooms.ceiling.off"), true);
        return 1;
    }

    /** O andar em que a pessoa esta; de fora das backrooms, o mais novo. */
    @Nullable
    private static BackroomsArea area(CommandSourceStack source, BackroomsData data) {
        ServerPlayer player = source.getPlayer();
        if (player != null && BackroomsManager.isInBackrooms(player)) {
            BackroomsArea here = data.areaAt(player.getBlockX(), player.getBlockZ());
            if (here != null) {
                return here;
            }
        }
        return data.newest();
    }

    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        BackroomsData data = BackroomsData.get(source.getServer());
        if (data.areas().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.backrooms.none"), false);
            return 0;
        }
        for (BackroomsArea area : data.areas()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.backrooms.info",
                    area.size(), area.originX(), area.originZ(), area.doors().size(),
                    area.ceiling() ? "sim" : "nao", area.seed()), false);
        }
        return data.areas().size();
    }

    private BackroomsCommands() {}
}
