package com.nexussmp.nexuscore.backrooms.client;

import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import com.nexussmp.nexuscore.backrooms.client.render.SkinwalkerRenderer;
import com.nexussmp.nexuscore.compat.xaero.XaeroMapsClient;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.core.module.NexusModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Parte de cliente das backrooms: a animacao de atravessar o chao. */
public final class BackroomsClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.BACKROOMS;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(PlayerTickEvent.Post.class, BackroomsTransition::onPlayerTick);
        game.addListener(RenderGuiEvent.Post.class, BackroomsTransition::onRenderGui);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, BackroomsTransition::onLoggingOut);

        game.addListener(PlayerTickEvent.Post.class, SkinwalkerEffects::onPlayerTick);
        game.addListener(PlayerTickEvent.Post.class, BackroomsAudio::onPlayerTick);
        game.addListener(RenderGuiEvent.Post.class, SkinwalkerEffects::onRenderGui);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, SkinwalkerEffects::onLoggingOut);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> BackroomsAudio.stop());

        // O Xaero quase sempre e so de cliente: o bloqueio dos mapas precisa acontecer aqui.
        XaeroMapsClient.blockWhen(player -> player.level().dimension().equals(BackroomsDimensions.BACKROOMS));

        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerEntityRenderer(NexusRegistries.SKINWALKER.get(), SkinwalkerRenderer::new));
    }
}
