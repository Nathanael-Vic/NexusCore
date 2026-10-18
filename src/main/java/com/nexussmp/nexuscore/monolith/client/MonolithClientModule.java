package com.nexussmp.nexuscore.monolith.client;

import com.nexussmp.nexuscore.compat.xaero.XaeroMapsClient;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import com.nexussmp.nexuscore.monolith.MonolithSites;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;

import java.util.List;

/** Parte de cliente dos monolitos: a tela, o F3, os mapas e as criaturas sem textura. */
public final class MonolithClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.MONOLITH;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(ModRequirement.any(MonolithSites.NAMESPACE));
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ClientTickEvent.Post.class, EchoClient::onClientTick);
        game.addListener(RenderGuiEvent.Post.class, EchoClient::onRenderGui);
        game.addListener(CustomizeGuiOverlayEvent.DebugText.class, EchoClient::onDebugText);
        game.addListener(EntityLeaveLevelEvent.class, EchoClient::onEntityLeave);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> EchoClient.reset());
        // No "quebrado", sem minimapa nem mapa-mundi: o lugar nao aparece em mapa nenhum.
        XaeroMapsClient.blockWhen(player -> EchoClient.broken());
    }
}
