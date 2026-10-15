package com.nexussmp.nexuscore.space.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import net.neoforged.fml.config.ModConfig;

/** Liga/desliga o ceu do Space, lido uma vez e guardado (o renderer consulta a cada frame). */
public final class SpaceClientSettings {
    private static volatile boolean enabled = true;

    static void onConfigChanged(ModConfig config) {
        if (NexusCore.MOD_ID.equals(config.getModId())) {
            enabled = CommonConfig.isModuleEnabled(ModuleIds.SPACE);
        }
    }

    /** Desligado, a dimensao continua existindo; so perde o ceu proprio (fundo liso, sem estrelas). */
    public static boolean enabled() {
        return enabled;
    }

    private SpaceClientSettings() {}
}
