package com.nexussmp.nexuscore.quest.client;

import com.nexussmp.nexuscore.quest.client.screen.QuestBoardScreen;
import com.nexussmp.nexuscore.quest.network.QuestPayloads;
import net.minecraft.client.Minecraft;

/** Recebe os pacotes do mural no cliente. */
public final class QuestClientHandler {

    public static void sync(QuestPayloads.Sync payload) {
        QuestClientCache.accept(payload.quests(), payload.admin());
    }

    /** O servidor confirmou que o jogador esta num quest board valido. */
    public static void openBoard() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.setScreen(new QuestBoardScreen());
        }
    }

    private QuestClientHandler() {}
}
