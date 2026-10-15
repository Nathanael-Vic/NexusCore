package com.nexussmp.nexuscore.quest;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.quest.client.QuestClientHandler;
import com.nexussmp.nexuscore.quest.network.QuestPayloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Mural de quests: lista unica do servidor, com as telas no cliente. */
public final class QuestModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.QUEST;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // Os lambdas do cliente so executam la; o servidor dedicado nunca carrega essas classes.
            registrar.playToClient(QuestPayloads.Sync.TYPE, QuestPayloads.Sync.STREAM_CODEC,
                    (payload, context) -> QuestClientHandler.sync(payload));
            registrar.playToClient(QuestPayloads.OpenBoard.TYPE, QuestPayloads.OpenBoard.STREAM_CODEC,
                    (payload, context) -> QuestClientHandler.openBoard());
            registrar.playToServer(QuestPayloads.Action.TYPE, QuestPayloads.Action.STREAM_CODEC,
                    QuestServerHandler::action);
            registrar.playToServer(QuestPayloads.Give.TYPE, QuestPayloads.Give.STREAM_CODEC,
                    QuestServerHandler::give);
            registrar.playToServer(QuestPayloads.Create.TYPE, QuestPayloads.Create.STREAM_CODEC,
                    QuestServerHandler::create);
        });
        modBus.addListener(ModConfigEvent.Loading.class, event -> onConfig(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> onConfig(event.getConfig()));
        modBus.addListener(ModConfigEvent.Unloading.class, event -> onConfig(event.getConfig()));

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ServerTickEvent.Post.class, QuestSync::onServerTick);
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, QuestSync::onLoggedIn);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, QuestSync::onLoggedOut);
        game.addListener(ServerStoppedEvent.class, event -> {
            QuestBoards.clear();
            QuestSync.clear();
        });
    }

    private static void onConfig(ModConfig config) {
        if (NexusCore.MOD_ID.equals(config.getModId())) {
            QuestSettings.refresh();
        }
    }
}
