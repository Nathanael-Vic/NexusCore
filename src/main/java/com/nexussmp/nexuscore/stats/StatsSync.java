package com.nexussmp.nexuscore.stats;

import com.nexussmp.nexuscore.stats.data.DimensionExploration;
import com.nexussmp.nexuscore.stats.data.Discovery;
import com.nexussmp.nexuscore.stats.data.ExplorationData;
import com.nexussmp.nexuscore.stats.data.RecentDiscovery;
import com.nexussmp.nexuscore.stats.data.StructureInstanceId;
import com.nexussmp.nexuscore.stats.network.DimensionStatsView;
import com.nexussmp.nexuscore.stats.network.StatsSnapshotPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manda as estatisticas aos clientes.
 *
 * <p>O rastreador so marca "mudou". Depois de {@code syncDelayTicks}, um unico pacote vai para todos:
 * dez descobertas no mesmo segundo viram um envio so. O snapshot montado fica guardado ate a proxima mudanca,
 * entao jogadores entrando nao remontam nada.
 */
public final class StatsSync {
    private static final int RECENT_SENT = 20;

    // volatile: a config pode recarregar (e marcar mudanca) fora da thread do servidor.
    private static volatile boolean dirty;
    private static int ticksWaiting;
    private static volatile StatsSnapshotPayload cached;

    public static void markDirty() {
        dirty = true;
        cached = null;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!dirty || ++ticksWaiting < StatsSettings.current().syncDelayTicks()) {
            return;
        }
        dirty = false;
        ticksWaiting = 0;
        PacketDistributor.sendToAllPlayers(snapshot(event.getServer()));
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, snapshot(player.server));
        }
    }

    public static void clear() {
        dirty = false;
        ticksWaiting = 0;
        cached = null;
    }

    private static StatsSnapshotPayload snapshot(MinecraftServer server) {
        StatsSnapshotPayload snapshot = cached;
        if (snapshot == null) {
            snapshot = build(server);
            cached = snapshot;
        }
        return snapshot;
    }

    private static StatsSnapshotPayload build(MinecraftServer server) {
        StatsSettings settings = StatsSettings.current();
        ExplorationData data = ExplorationData.get(server);
        List<DimensionStatsView> views = new ArrayList<>();
        for (ResourceKey<Level> key : settings.trackedDimensions()) {
            ServerLevel level = server.getLevel(key);
            if (level != null) {
                views.add(view(settings, level, data.dimension(key.location())));
            }
        }
        return new StatsSnapshotPayload(List.copyOf(views));
    }

    private static DimensionStatsView view(StatsSettings settings, ServerLevel level, DimensionExploration exploration) {
        List<DimensionStatsView.Entry> biomes = new ArrayList<>();
        List<DimensionStatsView.Entry> structureTypes = new ArrayList<>();
        List<DimensionStatsView.Recent> recent = new ArrayList<>();
        int structureCount = 0;

        if (exploration != null) {
            exploration.biomes().forEach((id, discovery) -> biomes.add(entry(id, discovery, 0)));

            Map<ResourceLocation, Integer> perType = new HashMap<>();
            for (StructureInstanceId instance : exploration.structures().keySet()) {
                perType.merge(instance.type(), 1, Integer::sum);
            }
            exploration.structureTypes().forEach((id, discovery) ->
                    structureTypes.add(entry(id, discovery, perType.getOrDefault(id, 0))));
            structureCount = exploration.structures().size();

            for (RecentDiscovery entry : exploration.recent()) {
                if (recent.size() >= RECENT_SENT) {
                    break;
                }
                recent.add(new DimensionStatsView.Recent(entry.kind(), entry.id(), entry.playerName(), entry.epochMillis()));
            }
        }

        return new DimensionStatsView(level.dimension().location(),
                settings.areaCenterX(), settings.areaCenterZ(), settings.areaRadius(),
                List.copyOf(biomes), List.copyOf(structureTypes), structureCount, List.copyOf(recent));
    }

    private static DimensionStatsView.Entry entry(ResourceLocation id, Discovery discovery, int count) {
        return new DimensionStatsView.Entry(id, discovery.playerName(), discovery.epochMillis(), count);
    }

    private StatsSync() {}
}
