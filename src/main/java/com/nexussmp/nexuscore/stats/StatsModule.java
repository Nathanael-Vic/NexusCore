package com.nexussmp.nexuscore.stats;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.stats.client.ClientStatsCache;
import com.nexussmp.nexuscore.stats.network.StatsSnapshotPayload;
import com.nexussmp.nexuscore.stats.tracking.ExplorationTracker;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Estatisticas de exploracao (backend, servidor).
 *
 * <p>O pacote de rede e sempre registrado (o protocolo nao pode variar com config); com o modulo desligado
 * o rastreador simplesmente nao registra nada.
 */
public final class StatsModule implements NexusModule {
    /** Sobe quando o formato do pacote mudar; cliente e servidor precisam da mesma versao. (2: sem totais.) */
    private static final String PROTOCOL_VERSION = "2";

    @Override
    public String id() {
        return ModuleIds.STATS;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(PROTOCOL_VERSION)
                // O lambda so executa no cliente; a classe do cache nao usa tipos de cliente.
                .playToClient(StatsSnapshotPayload.TYPE, StatsSnapshotPayload.STREAM_CODEC,
                        (payload, context) -> ClientStatsCache.accept(payload)));
        modBus.addListener(ModConfigEvent.Loading.class, event -> onConfig(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> onConfig(event.getConfig()));
        modBus.addListener(ModConfigEvent.Unloading.class, event -> onConfig(event.getConfig()));

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(PlayerTickEvent.Post.class, ExplorationTracker::onPlayerTick);
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, StatsSync::onLoggedIn);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, ExplorationTracker::onLoggedOut);
        game.addListener(ServerTickEvent.Post.class, StatsSync::onServerTick);
        game.addListener(ServerStartedEvent.class, event -> StatsSync.markDirty());
        game.addListener(ServerStoppedEvent.class, event -> {
            ExplorationTracker.clear();
            StatsSync.clear();
        });
    }

    private static void onConfig(ModConfig config) {
        if (!NexusCore.MOD_ID.equals(config.getModId())) {
            return;
        }
        StatsSettings.refresh();
        // Area ou dimensoes podem ter mudado: o que os clientes veem precisa acompanhar.
        StatsSync.markDirty();
    }
}
