package com.nexussmp.nexuscore.hologram;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.hologram.client.HologramClientAudio;
import com.nexussmp.nexuscore.hologram.network.HologramAudioPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Hologramas com captura de movimento: grava o que o jogador faz e reproduz no mundo.
 *
 * <p>A entidade e o pacote de audio sao sempre registrados (protocolo e registros nao podem variar com
 * config); com o modulo desligado, so o comando de reproduzir recusa.
 */
public final class HologramModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.HOLOGRAM;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(PROTOCOL_VERSION)
                // Só executa no cliente; o servidor dedicado nunca carrega a classe do audio.
                .playToClient(HologramAudioPayload.TYPE, HologramAudioPayload.STREAM_CODEC,
                        (payload, context) -> HologramClientAudio.play(payload)));
        modBus.addListener(ModConfigEvent.Loading.class, event -> onConfig(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> onConfig(event.getConfig()));
        modBus.addListener(ModConfigEvent.Unloading.class, event -> onConfig(event.getConfig()));

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(PlayerTickEvent.Post.class, HologramRecorder::onPlayerTick);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, HologramRecorder::onLoggedOut);
        game.addListener(PlayerEvent.StartTracking.class, HologramPlayback::onStartTracking);
        game.addListener(ServerStoppedEvent.class, event -> {
            HologramRecorder.clear();
            HologramStorage.clearCache();
        });
    }

    private static void onConfig(ModConfig config) {
        if (NexusCore.MOD_ID.equals(config.getModId())) {
            HologramSettings.refresh();
        }
    }
}
