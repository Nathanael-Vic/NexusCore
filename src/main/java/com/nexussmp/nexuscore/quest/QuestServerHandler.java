package com.nexussmp.nexuscore.quest;

import com.nexussmp.nexuscore.quest.network.QuestPayloads;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Recebe os pedidos das telas, confere tudo e responde no chat. */
public final class QuestServerHandler {
    /**
     * O que se resolve pelo diario do inventario, de qualquer lugar do mundo.
     *
     * <p>So <b>assumir</b> continua pedindo um mural por perto — e a unica acao que e sobre estar
     * ali, olhando o quadro. Acompanhar o que voce ja pegou, dizer que fez, conferir o que pediram a
     * voce e apagar o que voce mesmo publicou sao coisas de diario, nao de quadro.
     */
    private static final Set<QuestPayloads.Action.Kind> AWAY_FROM_BOARD = Set.of(
            QuestPayloads.Action.Kind.ABANDON,
            QuestPayloads.Action.Kind.SUBMIT,
            QuestPayloads.Action.Kind.UNSUBMIT,
            QuestPayloads.Action.Kind.COMPLETE,
            QuestPayloads.Action.Kind.DELETE,
            QuestPayloads.Action.Kind.SEEN);

    public static void action(QuestPayloads.Action payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!QuestSettings.current().enabled()) {
            feedback(player, QuestData.Result.CLOSED);
            return;
        }
        // O que se faz pelo livro do inventario nao exige estar no mural; o resto exige.
        if (!AWAY_FROM_BOARD.contains(payload.kind()) && !QuestBoards.atBoard(player)) {
            feedback(player, QuestData.Result.NO_BOARD);
            return;
        }

        QuestData data = QuestData.get(player.server);
        UUID questId = payload.questId();
        // Abrir o livro nao e uma acao com resposta: ninguem precisa ler "ok" por ter olhado.
        if (payload.kind() == QuestPayloads.Action.Kind.SEEN) {
            if (data.markSeen(player.getUUID())) {
                QuestSync.send(player);
            }
            return;
        }

        boolean admin = player.hasPermissions(QuestSync.ADMIN_LEVEL);
        QuestData.Result result = switch (payload.kind()) {
            case ACCEPT -> data.accept(player, questId);
            case ABANDON -> data.abandon(player, questId);
            case DELETE -> data.delete(questId, player.getUUID(), admin);
            case COMPLETE -> payload.target()
                    .map(target -> data.confirm(player, questId, target, admin))
                    .orElse(QuestData.Result.NOT_ACCEPTED);
            case SUBMIT -> data.submit(player, questId);
            case UNSUBMIT -> data.unsubmit(player, questId);
            case SEEN -> QuestData.Result.OK;
        };

        feedback(player, result);
        if (result == QuestData.Result.OK) {
            QuestSync.markDirty();
        }
    }

    public static void create(QuestPayloads.Create payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!QuestSettings.current().enabled()) {
            feedback(player, QuestData.Result.CLOSED);
            return;
        }
        if (!QuestBoards.atBoard(player)) {
            feedback(player, QuestData.Result.NO_BOARD);
            return;
        }
        QuestData.Result result = QuestData.get(player.server)
                .create(player, payload.title(), payload.description(), payload.reward(), payload.slots());
        feedback(player, result);
        if (result == QuestData.Result.OK) {
            QuestSync.markDirty();
        }
    }

    /**
     * Um operador entrega uma missao pela tela.
     *
     * <p>O nome do jogador e resolvido aqui, do lado do servidor, e a permissao tambem: a tela so
     * pede. Nao existe atalho de "o cliente disse que e operador".
     */
    public static void give(QuestPayloads.Give payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!QuestSettings.current().enabled()) {
            feedback(player, QuestData.Result.CLOSED);
            return;
        }
        if (!player.hasPermissions(QuestSync.ADMIN_LEVEL)) {
            feedback(player, QuestData.Result.NOT_AUTHOR);
            return;
        }
        ServerPlayer target = player.server.getPlayerList().getPlayerByName(payload.player());
        if (target == null) {
            player.displayClientMessage(
                    Component.translatable("nexuscore.quest.result.no_player", payload.player()), true);
            return;
        }
        QuestData.Result result = QuestData.get(player.server)
                .give(target, payload.title(), payload.description(), payload.reward());
        feedback(player, result);
        if (result == QuestData.Result.OK) {
            target.sendSystemMessage(Component.translatable("nexuscore.quest.given_to_you", payload.title()));
            QuestSync.markDirty();
        }
    }

    private static void feedback(ServerPlayer player, QuestData.Result result) {
        player.displayClientMessage(
                Component.translatable("nexuscore.quest.result." + result.name().toLowerCase(Locale.ROOT)),
                true);
    }

    private QuestServerHandler() {}
}
