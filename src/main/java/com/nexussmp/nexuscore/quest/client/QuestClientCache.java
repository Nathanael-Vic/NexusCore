package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.quest.network.QuestView;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** A ultima lista que o servidor mandou. As telas so leem daqui. */
public final class QuestClientCache {
    /** Quanto tempo uma quest concluida ainda aparece na lista antes de sair de cena. */
    private static final long LINGER_MS = 20_000L;

    private static List<QuestView> quests = List.of();
    /** Sobe a cada atualizacao; as telas abertas se refazem quando isto muda. */
    private static int version;
    private static boolean admin;
    /**
     * Quando <i>este cliente</i> viu cada quest concluida pela primeira vez.
     *
     * <p>E o relogio da maquina de quem esta jogando, de proposito: comparar com a hora do servidor
     * dependeria dos dois relogios baterem, e basta o computador de alguem estar meia hora adiantado
     * para a quest sumir antes de a pessoa ler que foi confirmada.
     */
    private static final Map<UUID, Long> finishedAt = new HashMap<>();
    /**
     * O que este cliente ja pediu para apagar e ainda esta na lista que tem em maos.
     *
     * <p>Entre clicar em excluir e a lista nova chegar passa um instante — pouco, mas o bastante
     * para a quest apagada continuar na tela e aceitar mais um clique, abrindo uma ficha de uma
     * coisa que nao existe mais. Ela some daqui na hora, e a lista seguinte confirma.
     */
    private static final Set<UUID> dropped = new HashSet<>();

    /** Sumir agora, sem esperar a resposta do servidor. */
    public static void forget(UUID quest) {
        dropped.add(quest);
        version++;
    }

    static void accept(List<QuestView> received, boolean isAdmin) {
        quests = received;
        admin = isAdmin;
        dropped.clear();
        version++;
        long now = System.currentTimeMillis();
        for (QuestView quest : received) {
            if (!quest.open()) {
                finishedAt.putIfAbsent(quest.id(), now);
            }
        }
        finishedAt.keySet().removeIf(id -> byId(id) == null);
    }

    public static boolean admin() {
        return admin;
    }

    public static void clear() {
        quests = List.of();
        finishedAt.clear();
        dropped.clear();
        admin = false;
        version++;
    }

    /**
     * A quest concluida fica um instante e sai.
     *
     * <p>Sumir na hora esconderia o desfecho de quem estava olhando; ficar para sempre transformaria
     * o caderno num arquivo morto. Ela fica o tempo de ser lida e some sozinha — e continua no
     * historico, que e onde arquivo morto deve estar.
     */
    private static boolean lingers(QuestView quest) {
        if (dropped.contains(quest.id())) {
            return false;
        }
        if (quest.open()) {
            return true;
        }
        Long since = finishedAt.get(quest.id());
        return since == null || System.currentTimeMillis() - since < LINGER_MS;
    }

    public static int version() {
        return version;
    }

    public static List<QuestView> all() {
        if (dropped.isEmpty()) {
            return quests;
        }
        List<QuestView> alive = new ArrayList<>();
        for (QuestView quest : quests) {
            if (!dropped.contains(quest.id())) {
                alive.add(quest);
            }
        }
        return alive;
    }

    public static UUID self() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? new UUID(0, 0) : minecraft.player.getUUID();
    }

    /**
     * O mural: abertas, sem contar as suas e as que ja estao cheias sem voce.
     *
     * <p>Missao do servidor fica de fora: ela nunca passou pelo mural, e uma vaga so ja ocupada
     * por quem a recebeu — apareceria ali como uma quest cheia que ninguem pode assumir.
     */
    public static List<QuestView> board() {
        UUID self = self();
        List<QuestView> board = new ArrayList<>();
        for (QuestView quest : quests) {
            if (!quest.fromServer() && quest.open() && !quest.isAuthor(self)
                    && !dropped.contains(quest.id())
                    && (!quest.isFull() || quest.hasAccepted(self))) {
                board.add(quest);
            }
        }
        return board;
    }

    public static List<QuestView> createdByMe() {
        UUID self = self();
        List<QuestView> mine = new ArrayList<>();
        for (QuestView quest : quests) {
            if (quest.isAuthor(self) && lingers(quest)) {
                mine.add(quest);
            }
        }
        return mine;
    }

    public static List<QuestView> acceptedByMe() {
        UUID self = self();
        List<QuestView> mine = new ArrayList<>();
        for (QuestView quest : quests) {
            if (quest.hasAccepted(self) && lingers(quest)) {
                mine.add(quest);
            }
        }
        return mine;
    }

    /** Alguma coisa entrou no livro e ainda nao foi olhada: e o que acende o selo no botao. */
    public static boolean hasUnread() {
        for (QuestView quest : quests) {
            if (quest.unread()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static QuestView byId(@Nullable UUID id) {
        if (id == null || dropped.contains(id)) {
            return null;
        }
        for (QuestView quest : quests) {
            if (quest.id().equals(id)) {
                return quest;
            }
        }
        return null;
    }

    private QuestClientCache() {}
}
