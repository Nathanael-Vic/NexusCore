package com.nexussmp.nexuscore.display.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.display.client.render.PanelRenderer;
import com.nexussmp.nexuscore.stats.client.ClientStatsCache;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte de cliente do painel: renderer da tela e limpeza do cache de estatisticas ao sair do servidor. */
public final class DisplayClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.DISPLAY;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerBlockEntityRenderer(NexusRegistries.EXPLORATION_PANEL_ENTITY.get(), PanelRenderer::new));
        modBus.addListener(RegisterClientReloadListenersEvent.class, event ->
                event.registerReloadListener((ResourceManagerReloadListener) resources -> DisplayClientSettings.onResourcesReloaded()));
        modBus.addListener(ModConfigEvent.Loading.class, event -> DisplayClientSettings.onConfigChanged(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> DisplayClientSettings.onConfigChanged(event.getConfig()));

        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> ClientStatsCache.clear());
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, PanelInput::onUseKey);
    }
}
