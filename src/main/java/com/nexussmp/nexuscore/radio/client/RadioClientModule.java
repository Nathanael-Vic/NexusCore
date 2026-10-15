package com.nexussmp.nexuscore.radio.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte de cliente da radio: o aviso, a plaquinha no canto, o botao da pausa e a musica. */
public final class RadioClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.RADIO;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ClientTickEvent.Post.class, RadioMusic::onClientTick);
        game.addListener(RenderGuiEvent.Post.class, RadioHud::onRenderGui);
        game.addListener(ScreenEvent.Init.Post.class, RadioPauseButton::onScreenInit);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> RadioClient.clear());
    }
}
