package com.nexussmp.nexuscore.compat.envelope;

import com.nexussmp.nexuscore.compat.envelope.client.BatCourierClient;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;

/**
 * O correio do Envelope, ajustado para o servidor.
 *
 * <ul>
 *   <li>Carta na caixa e nenhum pombo por perto: um pombo carteiro vem de longe buscar
 *       ({@link CourierDispatch}).</li>
 *   <li>O pombo surge e some longe e fora da vista, nunca na frente de alguem (o mixin da distancia
 *       de subida e o {@link CourierGoal}).</li>
 *   <li>Carta de lorde vampiro vai de morcego, e so de noite ({@link BatCouriers}).</li>
 * </ul>
 *
 * <p>Mexe em classes internas do Envelope (mixins em {@code mixin/envelope}), por isso so liga com a
 * versao testada dele. Detalhes em {@code docs/ENVELOPE.md}.
 */
public final class EnvelopeCompatModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.COMPAT_ENVELOPE;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(CompatRequirements.ENVELOPE);
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // O lambda do cliente so executa la; o servidor dedicado nunca carrega essa classe.
            registrar.playToClient(EnvelopePayloads.BatCourier.TYPE, EnvelopePayloads.BatCourier.STREAM_CODEC,
                    (payload, context) -> BatCourierClient.apply(payload));
        });

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ServerTickEvent.Post.class, CourierDispatch::onServerTick);
        game.addListener(EntityJoinLevelEvent.class, CourierDispatch::onJoin);
        game.addListener(EntityTickEvent.Post.class, BatCouriers::onEntityTick);
        game.addListener(PlayerEvent.StartTracking.class, BatCouriers::onStartTracking);
        game.addListener(ServerStoppedEvent.class, event -> CourierDispatch.clear());
    }
}
