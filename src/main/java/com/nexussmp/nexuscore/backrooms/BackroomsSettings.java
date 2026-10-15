package com.nexussmp.nexuscore.backrooms;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;

/** Config das backrooms, lida na hora do uso (nada aqui roda por frame). */
public final class BackroomsSettings {

    public static boolean enabled() {
        return CommonConfig.isModuleEnabled(ModuleIds.BACKROOMS);
    }

    public static int defaultSize() {
        return ServerConfig.get(ServerConfig.BACKROOMS_DEFAULT_SIZE);
    }

    public static int defaultDoors() {
        return ServerConfig.get(ServerConfig.BACKROOMS_DEFAULT_DOORS);
    }

    public static boolean defaultCeiling() {
        return ServerConfig.get(ServerConfig.BACKROOMS_DEFAULT_CEILING);
    }

    /** Blocos de forro entre uma luz e a seguinte. */
    public static int lampSpacing() {
        return ServerConfig.get(ServerConfig.BACKROOMS_LAMP_SPACING);
    }

    /** Chance, em porcento, de um lugar de painel realmente ganhar luz. */
    public static int lampChance() {
        return ServerConfig.get(ServerConfig.BACKROOMS_LAMP_CHANCE);
    }

    public static int cellSize() {
        return ServerConfig.get(ServerConfig.BACKROOMS_CELL_SIZE);
    }

    public static boolean skinwalkerEnabled() {
        return ServerConfig.get(ServerConfig.BACKROOMS_SKINWALKER);
    }

    public static int skinwalkerDelayTicks() {
        return ServerConfig.get(ServerConfig.BACKROOMS_SKINWALKER_DELAY_SECONDS) * 20;
    }

    public static int skinwalkerStalkTicks() {
        return ServerConfig.get(ServerConfig.BACKROOMS_SKINWALKER_STALK_SECONDS) * 20;
    }

    public static int skinwalkerRevealTicks() {
        return ServerConfig.get(ServerConfig.BACKROOMS_SKINWALKER_REVEAL_SECONDS) * 20;
    }

    public static int chunksPerTick() {
        return ServerConfig.get(ServerConfig.BACKROOMS_CHUNKS_PER_TICK);
    }

    /** 1 em N por minuto, por pessoa. 0 = evento desligado. */
    public static int eventOdds() {
        return ServerConfig.get(ServerConfig.BACKROOMS_EVENT_ODDS);
    }

    public static int eventCooldownMinutes() {
        return ServerConfig.get(ServerConfig.BACKROOMS_EVENT_COOLDOWN_MINUTES);
    }

    private BackroomsSettings() {}
}
