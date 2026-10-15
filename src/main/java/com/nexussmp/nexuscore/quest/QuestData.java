package com.nexussmp.nexuscore.quest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O mural: uma lista so, para o servidor inteiro, salva junto com o mundo.
 *
 * <p>Todas as regras ficam aqui, e nao na tela: o cliente pede, o servidor decide. Quest cheia some do
 * mural dos outros, so o autor (ou um operador) apaga, e so o autor marca como concluida escolhendo
 * quem cumpriu.
 */
public final class QuestData extends SavedData {
    public static final String FILE_ID = "nexuscore_quests";
    private static final int DATA_VERSION = 1;

    private static final Factory<QuestData> FACTORY = new Factory<>(QuestData::new, QuestData::load);

    /** Resposta de uma acao; vira mensagem no chat. */
    public enum Result {
        OK,
        NOT_FOUND,
        FULL,
        ALREADY_ACCEPTED,
        OWN_QUEST,
        NOT_ACCEPTED,
        NOT_AUTHOR,
        LIMIT_REACHED,
        INVALID_TEXT,
        CLOSED,
        NO_BOARD,
        ALREADY_SUBMITTED,
        NOT_SERVER_QUEST
    }

    private final Map<UUID, Quest> quests = new LinkedHashMap<>();

    public static QuestData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    // ---- consultas ----

    public List<Quest> all() {
        return List.copyOf(quests.values());
    }

    @Nullable
    public Quest byId(UUID id) {
        return quests.get(id);
    }

    /**
     * O que este jogador enxerga: o mural aberto, mais o que ele assumiu ou criou.
     *
     * <p>Operador ve <b>tudo</b>, e e o que faz a aba de administracao existir: ela nao pede nada ao
     * servidor, so filtra o que ja tem em maos. A lista inteira cabe num pacote com folga, e assim a
     * tela de adm responde na hora, sem ida e volta a cada clique. O pacote leva ate 256 quests, das
     * mais novas para as mais velhas; passou disso, as mais antigas ficam so no comando de lista.
     */
    public List<Quest> visibleTo(UUID viewer, boolean admin) {
        List<Quest> visible = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (admin || quest.isVisibleOnBoard(viewer) || quest.hasAccepted(viewer) || quest.isAuthor(viewer)) {
                visible.add(quest);
            }
        }
        visible.sort(Comparator.comparingLong(Quest::createdAt).reversed());
        return visible;
    }

    /** As missoes do servidor de uma pessoa, da mais nova para a mais velha. */
    public List<Quest> givenTo(UUID player) {
        List<Quest> given = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.fromServer() && quest.hasAccepted(player)) {
                given.add(quest);
            }
        }
        given.sort(Comparator.comparingLong(Quest::createdAt).reversed());
        return given;
    }

    /** Tudo que tem mao levantada esperando conferencia — missao do servidor ou quest de mural. */
    public List<Quest> awaitingCheck() {
        List<Quest> waiting = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (quest.pending()) {
                waiting.add(quest);
            }
        }
        waiting.sort(Comparator.comparingLong(Quest::lastSubmissionAt));
        return waiting;
    }

    public int openCountOf(UUID author) {
        int count = 0;
        for (Quest quest : quests.values()) {
            if (quest.isAuthor(author) && quest.isOpen()) {
                count++;
            }
        }
        return count;
    }

    // ---- acoes ----

    public Result create(ServerPlayer author, String title, String description, String reward, int slots) {
        QuestSettings settings = QuestSettings.current();
        String cleanTitle = clean(title, settings.titleLength());
        String cleanDescription = clean(description, settings.descriptionLength());
        String cleanReward = clean(reward, settings.rewardLength());
        if (cleanTitle.isEmpty() || cleanReward.isEmpty()) {
            return Result.INVALID_TEXT;
        }
        if (openCountOf(author.getUUID()) >= settings.maxOpenPerPlayer()) {
            return Result.LIMIT_REACHED;
        }
        int maxSlots = Math.clamp(slots, 1, settings.maxSlots());
        Quest quest = new Quest(UUID.randomUUID(), author, cleanTitle, cleanDescription, cleanReward, maxSlots);
        quests.put(quest.id(), quest);
        setDirty();
        return Result.OK;
    }

    /**
     * Entrega uma missao direto no livro de alguem.
     *
     * <p>Uma por pessoa, e nao uma com varias vagas: cada um cumpre o seu pedido no seu tempo, e o
     * operador confere um de cada vez. Nao conta no limite de quests abertas de ninguem — o autor e
     * o servidor.
     *
     * <p>A recompensa pode ficar em branco, ao contrario da quest de mural: muita missao do servidor
     * <i>e</i> a propria recompensa ("voltar a ser humano").
     */
    public Result give(ServerPlayer target, String title, String description, String reward) {
        QuestSettings settings = QuestSettings.current();
        String cleanTitle = clean(title, settings.titleLength());
        if (cleanTitle.isEmpty()) {
            return Result.INVALID_TEXT;
        }
        Quest quest = Quest.forPlayer(UUID.randomUUID(), target, cleanTitle,
                clean(description, settings.descriptionLength()), clean(reward, settings.rewardLength()));
        quests.put(quest.id(), quest);
        setDirty();
        return Result.OK;
    }

    /** "Fiz o que foi pedido." Quem confere e o autor da quest, ou um operador. */
    public Result submit(ServerPlayer player, UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return Result.NOT_FOUND;
        }
        if (!quest.hasAccepted(player.getUUID())) {
            return Result.NOT_ACCEPTED;
        }
        if (!quest.isOpen()) {
            return Result.CLOSED;
        }
        if (!quest.submit(player.getUUID())) {
            return Result.ALREADY_SUBMITTED;
        }
        setDirty();
        return Result.OK;
    }

    /** Voltar atras no "fiz": mandou cedo demais, ou o combinado mudou. */
    public Result unsubmit(ServerPlayer player, UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return Result.NOT_FOUND;
        }
        if (!quest.unsubmit(player.getUUID())) {
            return Result.NOT_ACCEPTED;
        }
        setDirty();
        return Result.OK;
    }

    /**
     * Fecha a quest creditando alguem.
     *
     * <p>Quem pode: o <b>autor</b> da quest de mural, e o <b>operador</b> em qualquer uma. Missao do
     * servidor nao tem autor, entao e sempre operador — que e o ponto dela.
     */
    public Result confirm(ServerPlayer actor, UUID questId, UUID winner, boolean admin) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return Result.NOT_FOUND;
        }
        if (!admin && !quest.isAuthor(actor.getUUID())) {
            return Result.NOT_AUTHOR;
        }
        if (!quest.isOpen()) {
            return Result.CLOSED;
        }
        if (!quest.complete(winner)) {
            return Result.NOT_ACCEPTED;
        }
        pruneHistory();
        setDirty();
        return Result.OK;
    }

    /** Pelo comando: fecha creditando quem levantou a mao primeiro (ou o dono, na missao). */
    public Result closeGiven(UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return Result.NOT_FOUND;
        }
        if (!quest.isOpen()) {
            return Result.CLOSED;
        }
        UUID winner = quest.submissions().stream().findFirst().orElseGet(quest::owner);
        if (winner == null || !quest.complete(winner)) {
            return Result.NOT_ACCEPTED;
        }
        pruneHistory();
        setDirty();
        return Result.OK;
    }

    /** O livro foi aberto: nada mais do que esta pessoa tem e novidade. */
    public boolean markSeen(UUID player) {
        boolean changed = false;
        for (Quest quest : quests.values()) {
            changed |= quest.markSeen(player);
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    public Result accept(ServerPlayer player, UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return Result.NOT_FOUND;
        }
        if (!quest.isOpen()) {
            return Result.CLOSED;
        }
        if (quest.isAuthor(player.getUUID())) {
            return Result.OWN_QUEST;
        }
        if (quest.hasAccepted(player.getUUID())) {
            return Result.ALREADY_ACCEPTED;
        }
        if (quest.isFull()) {
            return Result.FULL;
        }
        quest.accept(player);
        setDirty();
        return Result.OK;
    }

    public Result abandon(ServerPlayer player, UUID questId) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return Result.NOT_FOUND;
        }
        if (!quest.abandon(player.getUUID())) {
            return Result.NOT_ACCEPTED;
        }
        setDirty();
        return Result.OK;
    }

    public Result delete(UUID questId, UUID requester, boolean admin) {
        Quest quest = quests.get(questId);
        if (quest == null) {
            return Result.NOT_FOUND;
        }
        if (!admin && !quest.isAuthor(requester)) {
            return Result.NOT_AUTHOR;
        }
        quests.remove(questId);
        setDirty();
        return Result.OK;
    }

    /** Historico nao cresce para sempre: as concluidas mais antigas saem. */
    private void pruneHistory() {
        int limit = QuestSettings.current().historyLimit();
        List<Quest> completed = new ArrayList<>();
        for (Quest quest : quests.values()) {
            if (!quest.isOpen()) {
                completed.add(quest);
            }
        }
        if (completed.size() <= limit) {
            return;
        }
        completed.sort(Comparator.comparingLong(Quest::completedAt));
        for (int i = 0; i < completed.size() - limit; i++) {
            quests.remove(completed.get(i).id());
        }
    }

    private static String clean(String text, int maxLength) {
        String single = text.replace('\r', ' ').replace('\n', ' ').trim();
        return single.length() <= maxLength ? single : single.substring(0, maxLength);
    }

    // ---- arquivo ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", DATA_VERSION);
        ListTag list = new ListTag();
        quests.values().forEach(quest -> list.add(quest.save()));
        tag.put("quests", list);
        return tag;
    }

    private static QuestData load(CompoundTag tag, HolderLookup.Provider registries) {
        QuestData data = new QuestData();
        ListTag list = tag.getList("quests", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Quest quest = Quest.load(list.getCompound(i));
            if (quest != null) {
                data.quests.put(quest.id(), quest);
            }
        }
        return data;
    }
}
