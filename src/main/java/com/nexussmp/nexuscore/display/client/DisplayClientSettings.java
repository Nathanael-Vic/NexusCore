package com.nexussmp.nexuscore.display.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.ClientConfig;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import net.neoforged.fml.config.ModConfig;

/** Config do painel no cliente, guardada em campos: o renderer consulta a cada frame. */
public final class DisplayClientSettings {
    private static volatile boolean enabled = true;
    private static volatile int renderDistance = 48;
    /** Sobe a cada recarga de recursos (idioma, resource pack): textos traduzidos precisam ser refeitos. */
    private static volatile int resourceEpoch;

    static void onConfigChanged(ModConfig config) {
        if (NexusCore.MOD_ID.equals(config.getModId())) {
            enabled = CommonConfig.isModuleEnabled(ModuleIds.DISPLAY);
            renderDistance = ClientConfig.get(ClientConfig.PANEL_RENDER_DISTANCE);
        }
    }

    static void onResourcesReloaded() {
        resourceEpoch++;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int renderDistance() {
        return renderDistance;
    }

    public static int resourceEpoch() {
        return resourceEpoch;
    }

    private DisplayClientSettings() {}
}
