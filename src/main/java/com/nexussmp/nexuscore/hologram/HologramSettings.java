package com.nexussmp.nexuscore.hologram;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;

/** Config dos hologramas lida uma vez e guardada. */
public record HologramSettings(boolean enabled, int maxRecordingTicks, float defaultScale,
                               int audioJoinWindowTicks, int audioRangeBlocks, int maxActive) {

    /**
     * O alcance de um som no jogo sai do volume: acima de 1.0 ele vale 16 blocos por unidade.
     * Por isso a distancia desejada vira volume aqui, em vez de ser um numero proprio.
     */
    public float audioVolume() {
        return Math.max(1.0F, audioRangeBlocks / 16.0F);
    }

    private static volatile HologramSettings current = read();

    public static HologramSettings current() {
        return current;
    }

    static void refresh() {
        current = read();
    }

    private static HologramSettings read() {
        return new HologramSettings(
                CommonConfig.isModuleEnabled(ModuleIds.HOLOGRAM),
                ServerConfig.get(ServerConfig.HOLOGRAM_MAX_RECORDING_SECONDS) * 20,
                ServerConfig.get(ServerConfig.HOLOGRAM_DEFAULT_SCALE).floatValue(),
                ServerConfig.get(ServerConfig.HOLOGRAM_AUDIO_JOIN_WINDOW_SECONDS) * 20,
                ServerConfig.get(ServerConfig.HOLOGRAM_AUDIO_RANGE_BLOCKS),
                ServerConfig.get(ServerConfig.HOLOGRAM_MAX_ACTIVE));
    }
}
