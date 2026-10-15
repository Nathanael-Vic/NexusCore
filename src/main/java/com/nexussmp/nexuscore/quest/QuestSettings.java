package com.nexussmp.nexuscore.quest;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;

/** Regras do mural, lidas da config e guardadas. */
public record QuestSettings(boolean enabled, int maxOpenPerPlayer, int maxSlots, int boardRange,
                            int titleLength, int descriptionLength, int rewardLength, int historyLimit) {

    private static volatile QuestSettings current = read();

    public static QuestSettings current() {
        return current;
    }

    static void refresh() {
        current = read();
    }

    private static QuestSettings read() {
        return new QuestSettings(
                CommonConfig.isModuleEnabled(ModuleIds.QUEST),
                ServerConfig.get(ServerConfig.QUEST_MAX_OPEN_PER_PLAYER),
                ServerConfig.get(ServerConfig.QUEST_MAX_SLOTS),
                ServerConfig.get(ServerConfig.QUEST_BOARD_RANGE),
                64, 400, 96,
                ServerConfig.get(ServerConfig.QUEST_HISTORY_LIMIT));
    }
}
