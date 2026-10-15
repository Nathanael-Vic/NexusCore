package com.nexussmp.nexuscore.radio;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.radio.client.RadioClient;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Radio ao vivo: uma transmissao por vez, anunciada para o servidor inteiro.
 *
 * <p>Sessao, aviso, entrar, sair, o indicador, a musica e a <b>voz</b> — esta pelo Simple Voice
 * Chat, em {@code radio.voice}, e so quando ele esta instalado. O sistema de convidados entra depois,
 * e vai ler as mesmas marcas de
 * {@link Membership} que ja existem aqui.
 */
public final class RadioModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.RADIO;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // O lambda do cliente so executa la; o servidor dedicado nunca carrega essa classe.
            registrar.playToClient(RadioPayloads.State.TYPE, RadioPayloads.State.STREAM_CODEC,
                    (payload, context) -> RadioClient.apply(payload));
            registrar.playToServer(RadioPayloads.Action.TYPE, RadioPayloads.Action.STREAM_CODEC,
                    RadioServerHandler::action);
            registrar.playToServer(RadioPayloads.Start.TYPE, RadioPayloads.Start.STREAM_CODEC,
                    RadioServerHandler::start);
            registrar.playToServer(RadioPayloads.Mix.TYPE, RadioPayloads.Mix.STREAM_CODEC,
                    RadioServerHandler::mix);
        });

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RadioManager.onLoggedIn(player);
            }
        });
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RadioManager.onLoggedOut(player);
            }
        });
        // Transmissao e coisa do momento: o servidor que desliga leva o ar junto.
        game.addListener(ServerStoppedEvent.class, event -> RadioManager.clear());
    }
}
