package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.quest.network.QuestPayloads;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;
import java.util.UUID;

/** Atalhos para o cliente pedir as acoes. Quem valida e o servidor. */
public final class QuestActions {

    public static void accept(UUID questId) {
        send(QuestPayloads.Action.Kind.ACCEPT, questId, null);
    }

    public static void abandon(UUID questId) {
        send(QuestPayloads.Action.Kind.ABANDON, questId, null);
    }

    public static void delete(UUID questId) {
        send(QuestPayloads.Action.Kind.DELETE, questId, null);
        // Sai da lista agora: esperar a resposta deixava a quest apagada clicavel por um instante.
        QuestClientCache.forget(questId);
    }

    public static void complete(UUID questId, UUID winner) {
        send(QuestPayloads.Action.Kind.COMPLETE, questId, winner);
    }

    /** Missao do servidor: "fiz o que foi pedido". Quem confere e o operador. */
    public static void submit(UUID questId) {
        send(QuestPayloads.Action.Kind.SUBMIT, questId, null);
    }

    /** Voltar atras no "fiz": mandou cedo demais, ou o combinado mudou. */
    public static void unsubmit(UUID questId) {
        send(QuestPayloads.Action.Kind.UNSUBMIT, questId, null);
    }

    /** Operador entregando uma missao pela tela, em vez de pela barra de comando. */
    public static void give(String player, String title, String description, String reward) {
        PacketDistributor.sendToServer(new QuestPayloads.Give(player, title, description, reward));
    }

    /** O livro foi aberto: apaga o selo de novidade. Nao tem resposta e nao precisa ter. */
    public static void seen() {
        send(QuestPayloads.Action.Kind.SEEN, net.minecraft.Util.NIL_UUID, null);
    }

    public static void create(String title, String description, String reward, int slots) {
        PacketDistributor.sendToServer(new QuestPayloads.Create(title, description, reward, slots));
    }

    private static void send(QuestPayloads.Action.Kind kind, UUID questId, UUID target) {
        PacketDistributor.sendToServer(new QuestPayloads.Action(kind, questId, Optional.ofNullable(target)));
    }

    private QuestActions() {}
}
