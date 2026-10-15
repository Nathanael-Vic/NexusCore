package com.nexussmp.nexuscore.quest;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Uma quest do mural: alguem pede, alguem assume, e a recompensa e combinada entre os dois.
 *
 * <p>A recompensa e texto livre de proposito: num servidor de comunidade o combinado vale mais do que
 * um item automatico, e o mural serve para as pessoas se acharem e negociarem.
 *
 * <p>Existe um segundo tipo, que nao passa pelo mural: a <b>missao do servidor</b>, entregue por
 * comando direto no livro de alguem. Ela nao tem autor visivel (so aparecem as informacoes), nao fica
 * no mural de ninguem, e quem a recebe tem um botao para dizer que fez. Quem confere e o operador —
 * o pedido costuma ser uma coisa do mundo, que nenhum codigo saberia medir.
 */
public final class Quest {
    private final UUID id;
    private final UUID authorId;
    private final String authorName;
    private final long createdAt;

    private String title;
    private String description;
    private String reward;
    private int maxSlots;

    /** Quem assumiu, em ordem de chegada (id → nome na hora em que assumiu). */
    private final Map<UUID, String> accepted = new LinkedHashMap<>();
    /** Quem ja abriu o livro depois que esta quest entrou nele. O resto ganha o selo no botao. */
    private final Set<UUID> seen = new LinkedHashSet<>();

    /** Missao do servidor: sem autor, fora do mural, e conferida por operador. */
    private boolean fromServer;
    /**
     * Quem ja disse "fiz".
     *
     * <p>Vale para os dois tipos. Na quest de mural, era o autor que tinha de adivinhar quem havia
     * cumprido; agora quem cumpriu levanta a mao e o autor so confirma. Na missao do servidor a mao
     * levantada e para o operador ver.
     */
    private final Set<UUID> submissions = new LinkedHashSet<>();
    private long lastSubmissionAt;

    private QuestStatus status = QuestStatus.OPEN;
    @Nullable
    private UUID completedBy;
    private String completedByName = "";
    private long completedAt;

    public Quest(UUID id, ServerPlayer author, String title, String description, String reward, int maxSlots) {
        this.id = id;
        this.authorId = author.getUUID();
        this.authorName = author.getGameProfile().getName();
        this.createdAt = System.currentTimeMillis();
        this.title = title;
        this.description = description;
        this.reward = reward;
        this.maxSlots = maxSlots;
    }

    /**
     * Uma missao entregue por comando.
     *
     * <p>O autor e o proprio servidor, entao o campo fica no id vazio e o nome em branco — e o que
     * faz a tela nao ter de que mostrar. Uma vaga so e ja ocupada por quem recebeu: missao do
     * servidor e coisa entre o servidor e uma pessoa.
     */
    public static Quest forPlayer(UUID id, ServerPlayer target, String title, String description, String reward) {
        Quest quest = new Quest(id, Util.NIL_UUID, "", System.currentTimeMillis());
        quest.title = title;
        quest.description = description;
        quest.reward = reward;
        quest.maxSlots = 1;
        quest.fromServer = true;
        quest.accepted.put(target.getUUID(), target.getGameProfile().getName());
        return quest;
    }

    private Quest(UUID id, UUID authorId, String authorName, long createdAt) {
        this.id = id;
        this.authorId = authorId;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    public UUID id() {
        return id;
    }

    public UUID authorId() {
        return authorId;
    }

    public String authorName() {
        return authorName;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public String reward() {
        return reward;
    }

    public int maxSlots() {
        return maxSlots;
    }

    public long createdAt() {
        return createdAt;
    }

    public QuestStatus status() {
        return status;
    }

    public Map<UUID, String> accepted() {
        return accepted;
    }

    public int acceptedCount() {
        return accepted.size();
    }

    public boolean isFull() {
        return accepted.size() >= maxSlots;
    }

    public boolean isOpen() {
        return status == QuestStatus.OPEN;
    }

    public boolean hasAccepted(UUID player) {
        return accepted.containsKey(player);
    }

    /** Missao do servidor nao tem autor: ninguem a gerencia pela tela, so operador por comando. */
    public boolean isAuthor(UUID player) {
        return !fromServer && authorId.equals(player);
    }

    public boolean fromServer() {
        return fromServer;
    }

    public boolean submitted(UUID player) {
        return submissions.contains(player);
    }

    public Set<UUID> submissions() {
        return submissions;
    }

    /** Alguem levantou a mao e ninguem conferiu ainda. */
    public boolean pending() {
        return isOpen() && !submissions.isEmpty();
    }

    public long lastSubmissionAt() {
        return lastSubmissionAt;
    }

    /** Quem recebeu a missao (missao do servidor tem exatamente um). */
    @Nullable
    public UUID owner() {
        return accepted.isEmpty() ? null : accepted.keySet().iterator().next();
    }

    public String ownerName() {
        return accepted.isEmpty() ? "" : accepted.values().iterator().next();
    }

    /** Entrou no livro e ainda nao foi aberta: e isso que acende o selo no botao. */
    public boolean isUnreadFor(UUID player) {
        return accepted.containsKey(player) && !seen.contains(player);
    }

    /** {@code true} se havia mesmo algo por ver. */
    boolean markSeen(UUID player) {
        return accepted.containsKey(player) && seen.add(player);
    }

    @Nullable
    public UUID completedBy() {
        return completedBy;
    }

    public String completedByName() {
        return completedByName;
    }

    public long completedAt() {
        return completedAt;
    }

    /**
     * Quem ve a quest no mural: some para os outros quando lota, mas continua para quem assumiu e
     * para o autor. Missao do servidor nunca aparece no mural — ela e de uma pessoa so.
     */
    public boolean isVisibleOnBoard(UUID viewer) {
        return !fromServer && isOpen() && (!isFull() || hasAccepted(viewer) || isAuthor(viewer));
    }

    boolean accept(ServerPlayer player) {
        if (!isOpen() || isFull() || hasAccepted(player.getUUID()) || isAuthor(player.getUUID())) {
            return false;
        }
        accepted.put(player.getUUID(), player.getGameProfile().getName());
        return true;
    }

    boolean abandon(UUID player) {
        seen.remove(player);
        submissions.remove(player);
        return accepted.remove(player) != null;
    }

    /** "Fiz o que foi pedido." Nao conclui nada: quem confere e o autor (ou um operador). */
    boolean submit(UUID player) {
        if (!isOpen() || !accepted.containsKey(player) || !submissions.add(player)) {
            return false;
        }
        lastSubmissionAt = System.currentTimeMillis();
        return true;
    }

    /** Voltar atras: mandou cedo demais, ou o combinado mudou. */
    boolean unsubmit(UUID player) {
        return isOpen() && submissions.remove(player);
    }

    boolean complete(UUID winner) {
        if (!isOpen() || !accepted.containsKey(winner)) {
            return false;
        }
        status = QuestStatus.COMPLETED;
        completedBy = winner;
        completedByName = accepted.get(winner);
        completedAt = System.currentTimeMillis();
        return true;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putUUID("author", authorId);
        tag.putString("authorName", authorName);
        tag.putLong("createdAt", createdAt);
        tag.putString("title", title);
        tag.putString("description", description);
        tag.putString("reward", reward);
        tag.putInt("maxSlots", maxSlots);
        tag.putString("status", status.id());
        if (fromServer) {
            tag.putBoolean("fromServer", true);
        }
        tag.putLong("lastSubmissionAt", lastSubmissionAt);

        ListTag submittedList = new ListTag();
        submissions.forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            submittedList.add(entry);
        });
        tag.put("submissions", submittedList);

        ListTag seenList = new ListTag();
        seen.forEach(id -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            seenList.add(entry);
        });
        tag.put("seen", seenList);

        ListTag acceptedList = new ListTag();
        accepted.forEach((uuid, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", uuid);
            entry.putString("name", name);
            acceptedList.add(entry);
        });
        tag.put("accepted", acceptedList);

        if (completedBy != null) {
            tag.putUUID("completedBy", completedBy);
            tag.putString("completedByName", completedByName);
            tag.putLong("completedAt", completedAt);
        }
        return tag;
    }

    @Nullable
    static Quest load(CompoundTag tag) {
        if (!tag.hasUUID("id") || !tag.hasUUID("author")) {
            return null;
        }
        Quest quest = new Quest(tag.getUUID("id"), tag.getUUID("author"), tag.getString("authorName"),
                tag.getLong("createdAt"));
        quest.title = tag.getString("title");
        quest.description = tag.getString("description");
        quest.reward = tag.getString("reward");
        quest.maxSlots = Math.max(1, tag.getInt("maxSlots"));
        quest.status = QuestStatus.byId(tag.getString("status"));
        quest.fromServer = tag.getBoolean("fromServer");
        quest.lastSubmissionAt = tag.getLong("lastSubmissionAt");

        for (Tag entry : tag.getList("submissions", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            if (compound.hasUUID("id")) {
                quest.submissions.add(compound.getUUID("id"));
            }
        }

        for (Tag entry : tag.getList("seen", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            if (compound.hasUUID("id")) {
                quest.seen.add(compound.getUUID("id"));
            }
        }

        ListTag acceptedList = tag.getList("accepted", Tag.TAG_COMPOUND);
        for (int i = 0; i < acceptedList.size(); i++) {
            CompoundTag entry = acceptedList.getCompound(i);
            if (entry.hasUUID("id")) {
                quest.accepted.put(entry.getUUID("id"), entry.getString("name"));
            }
        }

        if (tag.hasUUID("completedBy")) {
            quest.completedBy = tag.getUUID("completedBy");
            quest.completedByName = tag.getString("completedByName");
            quest.completedAt = tag.getLong("completedAt");
        }
        return quest;
    }
}
