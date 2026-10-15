package com.nexussmp.nexuscore.vampire.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.vampire.Vampires;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte de cliente do vampiro: os botoes, as telas que nao abrem para quem nao e lorde e as gotas no AppleSkin. */
public final class VampireClientModule implements NexusModule {
    private static final String APPLESKIN = "appleskin";

    @Override
    public String id() {
        return ModuleIds.VAMPIRE;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> VampireClient.clear());
        if (!Vampires.installed()) {
            // Sem o mod nao ha botao para tirar nem tela para recusar — e a classe que mexe nisso
            // nem chega a ser carregada, que e o que importa.
            return;
        }
        // LOWEST de proposito: o botao do Vampirism precisa ja estar na tela para ser tirado dela.
        game.addListener(EventPriority.LOWEST, ScreenEvent.Init.Post.class,
                VampireSkillButton::onScreenInit);
        // Os botoes da tela do vampiro: so a moldura muda, ver VampireMenuButtons.
        game.addListener(EventPriority.LOWEST, ScreenEvent.Init.Post.class,
                VampireMenuButtons::onScreenInit);
        game.addListener(ScreenEvent.Opening.class, VampireSkillButton::onScreenOpening);
        // O contorno de saturacao no formato das gotas. Sem o AppleSkin nao ha contorno nenhum para
        // corrigir, e a classe que fala com ele nem e carregada.
        if (ModList.get().isLoaded(APPLESKIN)) {
            BloodSaturationOverlay.register(game);
        }
    }
}
