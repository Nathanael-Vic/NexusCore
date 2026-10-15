package com.nexussmp.nexuscore.quest.network;

import com.nexussmp.nexuscore.quest.Quest;
import com.nexussmp.nexuscore.quest.QuestStatus;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A quest do jeito que a tela precisa. O servidor manda so o que aquele jogador pode ver. */
public record QuestView(UUID id, String title, String description, String reward, UUID authorId, String authorName,
                        int maxSlots, List<Participant> accepted, boolean open, String completedByName, long createdAt,
                        boolean fromServer, List<Participant> submissions, boolean unread) {

    public record Participant(UUID id, String name) {}

    private static final int MAX_TEXT = 512;
    private static final int MAX_NAME = 64;
    private static final int MAX_PARTICIPANTS = 64;

    /** O {@code viewer} decide o que e novidade: "nao lida" e por pessoa, nao por quest. */
    public static QuestView of(Quest quest, UUID viewer) {
        List<Participant> participants = new ArrayList<>(quest.accepted().size());
        List<Participant> submitted = new ArrayList<>();
        quest.accepted().forEach((id, name) -> {
            participants.add(new Participant(id, name));
            if (quest.submitted(id)) {
                submitted.add(new Participant(id, name));
            }
        });
        return new QuestView(quest.id(), quest.title(), quest.description(), quest.reward(),
                quest.authorId(), quest.authorName(), quest.maxSlots(), List.copyOf(participants),
                quest.status() == QuestStatus.OPEN, quest.completedByName(), quest.createdAt(),
                quest.fromServer(), List.copyOf(submitted), quest.isUnreadFor(viewer));
    }

    public boolean submitted(UUID player) {
        for (Participant participant : submissions) {
            if (participant.id().equals(player)) {
                return true;
            }
        }
        return false;
    }

    /** Da para levantar a mao: esta aberta, e sua, e voce ainda nao levantou. */
    public boolean canSubmit(UUID player) {
        return open && hasAccepted(player) && !submitted(player);
    }

    /** Alguem levantou a mao e ninguem conferiu: e o que o autor (ou o adm) precisa ver. */
    public boolean pending() {
        return open && !submissions.isEmpty();
    }

    public boolean isAuthor(UUID player) {
        return authorId.equals(player);
    }

    public boolean hasAccepted(UUID player) {
        for (Participant participant : accepted) {
            if (participant.id().equals(player)) {
                return true;
            }
        }
        return false;
    }

    public boolean isFull() {
        return accepted.size() >= maxSlots;
    }

    /** {@code true} quando ainda da para assumir: aberta, com vaga, nao e sua e voce ainda nao assumiu. */
    public boolean canAccept(UUID player) {
        return open && !isFull() && !isAuthor(player) && !hasAccepted(player);
    }

    void write(FriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(title, MAX_TEXT);
        buf.writeUtf(description, MAX_TEXT);
        buf.writeUtf(reward, MAX_TEXT);
        buf.writeUUID(authorId);
        buf.writeUtf(authorName, MAX_NAME);
        buf.writeVarInt(maxSlots);
        int size = Math.min(accepted.size(), MAX_PARTICIPANTS);
        buf.writeVarInt(size);
        for (int i = 0; i < size; i++) {
            Participant participant = accepted.get(i);
            buf.writeUUID(participant.id());
            buf.writeUtf(participant.name(), MAX_NAME);
        }
        buf.writeBoolean(open);
        buf.writeUtf(completedByName, MAX_NAME);
        buf.writeLong(createdAt);
        buf.writeBoolean(fromServer);
        int submittedSize = Math.min(submissions.size(), MAX_PARTICIPANTS);
        buf.writeVarInt(submittedSize);
        for (int i = 0; i < submittedSize; i++) {
            Participant participant = submissions.get(i);
            buf.writeUUID(participant.id());
            buf.writeUtf(participant.name(), MAX_NAME);
        }
        buf.writeBoolean(unread);
    }

    static QuestView read(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String title = buf.readUtf(MAX_TEXT);
        String description = buf.readUtf(MAX_TEXT);
        String reward = buf.readUtf(MAX_TEXT);
        UUID authorId = buf.readUUID();
        String authorName = buf.readUtf(MAX_NAME);
        int maxSlots = buf.readVarInt();
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException("quest com " + size + " participantes");
        }
        List<Participant> accepted = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            accepted.add(new Participant(buf.readUUID(), buf.readUtf(MAX_NAME)));
        }
        boolean open = buf.readBoolean();
        String completedByName = buf.readUtf(MAX_NAME);
        long createdAt = buf.readLong();
        boolean fromServer = buf.readBoolean();
        int submittedSize = buf.readVarInt();
        if (submittedSize < 0 || submittedSize > MAX_PARTICIPANTS) {
            throw new IllegalArgumentException("quest com " + submittedSize + " entregas");
        }
        List<Participant> submissions = new ArrayList<>(submittedSize);
        for (int i = 0; i < submittedSize; i++) {
            submissions.add(new Participant(buf.readUUID(), buf.readUtf(MAX_NAME)));
        }
        return new QuestView(id, title, description, reward, authorId, authorName, maxSlots,
                List.copyOf(accepted), open, completedByName, createdAt,
                fromServer, List.copyOf(submissions), buf.readBoolean());
    }
}
