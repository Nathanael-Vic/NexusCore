package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte de cliente do mural: o livro no inventario e a limpeza do cache ao sair do servidor. */
public final class QuestClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.QUEST;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ScreenEvent.Init.Post.class, InventoryQuestPanel::onScreenInit);
        // Clique e rolagem agora sao do proprio widget do caderno: sobra so a dica, que precisa ser
        // desenhada depois de tudo.
        game.addListener(ScreenEvent.Render.Post.class, InventoryQuestPanel::onScreenRender);
        game.addListener(ScreenEvent.KeyPressed.Pre.class, InventoryQuestPanel::onKeyPressed);
        game.addListener(ClientTickEvent.Post.class, InventoryQuestPanel::onClientTick);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            QuestClientCache.clear();
            InventoryQuestPanel.close();
        });
    }
}
