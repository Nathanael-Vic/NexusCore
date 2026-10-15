package com.nexussmp.nexuscore.quest;

import java.util.Locale;

/** Situacao de uma quest. Quest concluida sai do mural e fica no historico do autor e de quem cumpriu. */
public enum QuestStatus {
    OPEN,
    COMPLETED;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static QuestStatus byId(String id) {
        return COMPLETED.id().equals(id) ? COMPLETED : OPEN;
    }
}
