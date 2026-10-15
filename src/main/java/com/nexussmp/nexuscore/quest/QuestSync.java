package com.nexussmp.nexuscore.quest;

import com.nexussmp.nexuscore.quest.network.QuestPayloads;
import com.nexussmp.nexuscore.quest.network.QuestView;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Manda o mural para os clientes.
 *
 * <p>Cada jogador recebe a **sua** lista: quest cheia some para quem nao assumiu, e quest concluida so
 * continua para o autor e para quem cumpriu. A filtragem e no servidor, entao a tela nao precisa
 * esconder nada (e nem adianta espiar o pacote).
 */
public final class QuestSync {
    /** Junta varias mudancas seguidas num envio so. */
    private static final int DELAY_TICKS = 10;
    /** Operador: ve tudo e tem as abas de administracao. */
    public static final int ADMIN_LEVEL = 2;

    private static volatile boolean dirty;
    private static int ticksWaiting;

    public static void markDirty() {
        dirty = true;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!dirty || ++ticksWaiting < DELAY_TICKS) {
            return;
        }
        dirty = false;
        ticksWaiting = 0;
        QuestData data = QuestData.get(event.getServer());
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            send(player, data);
        }
    }

    public static void onLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            send(player, QuestData.get(player.server));
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        QuestBoards.forget(event.getEntity().getUUID());
    }

    public static void send(ServerPlayer player) {
        send(player, QuestData.get(player.server));
    }

    private static void send(ServerPlayer player, QuestData data) {
        boolean admin = player.hasPermissions(ADMIN_LEVEL);
        List<Quest> visible = data.visibleTo(player.getUUID(), admin);
        List<QuestView> views = new ArrayList<>(visible.size());
        for (Quest quest : visible) {
            views.add(QuestView.of(quest, player.getUUID()));
        }
        PacketDistributor.sendToPlayer(player, new QuestPayloads.Sync(List.copyOf(views), admin));
    }

    public static void clear() {
        dirty = false;
        ticksWaiting = 0;
    }

    private QuestSync() {}
}
