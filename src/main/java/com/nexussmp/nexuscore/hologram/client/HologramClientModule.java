package com.nexussmp.nexuscore.hologram.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.hologram.client.render.HologramRenderer;
import com.nexussmp.nexuscore.hologram.client.render.HologramShaders;
import com.nexussmp.nexuscore.hologram.client.render.HologramSkins;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte de cliente dos hologramas: shader, renderer e limpeza dos caches ao sair do servidor. */
public final class HologramClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.HOLOGRAM;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterShadersEvent.class, HologramShaders::onRegisterShaders);
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerEntityRenderer(NexusRegistries.HOLOGRAM.get(), HologramRenderer::new));
        modBus.addListener(ModConfigEvent.Loading.class, event -> HologramClientSettings.onConfigChanged(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> HologramClientSettings.onConfigChanged(event.getConfig()));

        NeoForge.EVENT_BUS.addListener(RenderLevelStageEvent.class, HologramShaders::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            HologramClientAudio.clear();
        });
    }
}
