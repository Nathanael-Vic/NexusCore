package com.nexussmp.nexuscore.stats;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.space.SpaceDimensions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Config do rastreamento lida uma vez e guardada num objeto imutavel.
 * O rastreador consulta isto a cada verificacao de jogador, entao precisa ser so leitura de campo.
 */
public record StatsSettings(
        boolean enabled,
        List<ResourceKey<Level>> trackedDimensions,
        Set<ResourceKey<Level>> trackedSet,
        int areaCenterX,
        int areaCenterZ,
        int areaRadius,
        int checkIntervalTicks,
        boolean countCreative,
        boolean countSpectator,
        int syncDelayTicks) {

    private static volatile StatsSettings current = read();

    public static StatsSettings current() {
        return current;
    }

    /** Chamado quando a config comum ou do servidor carrega/muda. */
    static void refresh() {
        current = read();
    }

    private static StatsSettings read() {
        List<ResourceKey<Level>> dimensions = new ArrayList<>();
        for (String id : ServerConfig.get(ServerConfig.TRACKED_DIMENSIONS)) {
            ResourceLocation location = ResourceLocation.tryParse(id);
            // O Space e lobby, nao area de exploracao: nunca conta, mesmo se alguem colocar na config.
            if (location != null && !location.equals(SpaceDimensions.SPACE_ID)) {
                ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, location);
                if (!dimensions.contains(key)) {
                    dimensions.add(key);
                }
            }
        }
        return new StatsSettings(
                CommonConfig.isModuleEnabled(ModuleIds.STATS),
                Collections.unmodifiableList(dimensions),
                Collections.unmodifiableSet(new LinkedHashSet<>(dimensions)),
                ServerConfig.get(ServerConfig.AREA_CENTER_X),
                ServerConfig.get(ServerConfig.AREA_CENTER_Z),
                ServerConfig.get(ServerConfig.AREA_RADIUS),
                ServerConfig.get(ServerConfig.CHECK_INTERVAL_TICKS),
                ServerConfig.get(ServerConfig.COUNT_CREATIVE),
                ServerConfig.get(ServerConfig.COUNT_SPECTATOR),
                ServerConfig.get(ServerConfig.SYNC_DELAY_TICKS));
    }

    public boolean tracks(ResourceKey<Level> dimension) {
        return trackedSet.contains(dimension);
    }

    /** Dentro do quadrado configurado (raio 0 = sem limite). */
    public boolean insideArea(int blockX, int blockZ) {
        return areaRadius == 0
                || (Math.abs((long) blockX - areaCenterX) <= areaRadius && Math.abs((long) blockZ - areaCenterZ) <= areaRadius);
    }

    public boolean counts(ServerPlayer player) {
        if (player.isSpectator()) {
            return countSpectator;
        }
        if (player.isCreative()) {
            return countCreative;
        }
        return true;
    }
}
