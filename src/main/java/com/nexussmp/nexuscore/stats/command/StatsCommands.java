package com.nexussmp.nexuscore.stats.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.nexussmp.nexuscore.stats.ExplorationNames;
import com.nexussmp.nexuscore.stats.StatsSettings;
import com.nexussmp.nexuscore.stats.StatsSync;
import com.nexussmp.nexuscore.stats.data.DimensionExploration;
import com.nexussmp.nexuscore.stats.data.ExplorationData;
import com.nexussmp.nexuscore.stats.data.PlayerExploration;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Collection;

/** {@code /nexus stats ...} Mostra so o que foi descoberto: a dimensao e desconhecida, nao existe total. */
public final class StatsCommands {
    private static final int ADMIN_LEVEL = 3;

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("estatisticas")
                .executes(context -> summary(context.getSource()))
                .then(Commands.literal("jogador")
                        .then(Commands.argument("jogador", GameProfileArgument.gameProfile())
                                .executes(context -> player(context.getSource(),
                                        GameProfileArgument.getGameProfiles(context, "jogador")))))
                .then(Commands.literal("zerar")
                        .requires(source -> source.hasPermission(ADMIN_LEVEL))
                        .then(Commands.argument("dimensao", DimensionArgument.dimension())
                                .executes(context -> resetWarning(context.getSource(),
                                        DimensionArgument.getDimension(context, "dimensao")))
                                .then(Commands.literal("confirmar")
                                        .executes(context -> reset(context.getSource(),
                                                DimensionArgument.getDimension(context, "dimensao"))))));
    }

    private static int summary(CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        StatsSettings settings = StatsSettings.current();
        ExplorationData data = ExplorationData.get(server);
        if (settings.trackedDimensions().isEmpty()) {
            source.sendSuccess(() -> Component.translatable("nexuscore.command.stats.no_dimensions"), false);
            return 0;
        }
        for (ResourceKey<Level> key : settings.trackedDimensions()) {
            ServerLevel level = server.getLevel(key);
            if (level == null) {
                continue;
            }
            DimensionExploration exploration = data.dimension(key.location());
            int biomes = exploration == null ? 0 : exploration.biomes().size();
            int types = exploration == null ? 0 : exploration.structureTypes().size();
            int structures = exploration == null ? 0 : exploration.structures().size();
            source.sendSuccess(() -> Component.translatable("nexuscore.command.stats.dimension",
                    ExplorationNames.dimension(key.location()), biomes, types, structures), false);
        }
        source.sendSuccess(() -> areaLine(settings), false);
        return 1;
    }

    private static int player(CommandSourceStack source, Collection<GameProfile> profiles) {
        MinecraftServer server = source.getServer();
        StatsSettings settings = StatsSettings.current();
        ExplorationData data = ExplorationData.get(server);
        for (GameProfile profile : profiles) {
            PlayerExploration player = data.player(profile.getId());
            if (player == null) {
                source.sendSuccess(() -> Component.translatable("nexuscore.command.stats.player.none", profile.getName()), false);
                continue;
            }
            source.sendSuccess(() -> Component.translatable("nexuscore.command.stats.player.header", profile.getName()), false);
            for (ResourceKey<Level> key : settings.trackedDimensions()) {
                PlayerExploration.Dimension dimension = player.dimensions().get(key.location());
                int biomes = dimension == null ? 0 : dimension.biomeCount();
                int types = dimension == null ? 0 : dimension.structureTypeCount();
                int structures = dimension == null ? 0 : dimension.structureCount();
                source.sendSuccess(() -> Component.translatable("nexuscore.command.stats.player.dimension",
                        ExplorationNames.dimension(key.location()), biomes, types, structures), false);
            }
        }
        return profiles.size();
    }

    private static int resetWarning(CommandSourceStack source, ServerLevel level) {
        source.sendSuccess(() -> Component.translatable("nexuscore.command.stats.reset.confirm",
                ExplorationNames.dimension(level.dimension().location()),
                "/nexus estatisticas zerar " + level.dimension().location() + " confirmar"), false);
        return 0;
    }

    private static int reset(CommandSourceStack source, ServerLevel level) {
        ResourceLocation id = level.dimension().location();
        boolean changed = ExplorationData.get(source.getServer()).resetDimension(id);
        StatsSync.markDirty();
        source.sendSuccess(() -> Component.translatable(changed
                ? "nexuscore.command.stats.reset.done" : "nexuscore.command.stats.reset.empty",
                ExplorationNames.dimension(id)), true);
        return changed ? 1 : 0;
    }

    private static Component areaLine(StatsSettings settings) {
        if (settings.areaRadius() == 0) {
            return Component.translatable("nexuscore.command.stats.area.unlimited");
        }
        long side = 2L * settings.areaRadius();
        return Component.translatable("nexuscore.command.stats.area", side, side,
                settings.areaCenterX(), settings.areaCenterZ());
    }

    private StatsCommands() {}
}
