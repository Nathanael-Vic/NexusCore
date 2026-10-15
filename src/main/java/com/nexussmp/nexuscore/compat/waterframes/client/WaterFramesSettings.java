package com.nexussmp.nexuscore.compat.waterframes.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.ClientConfig;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import net.neoforged.fml.config.ModConfig;

/**
 * Valores de config lidos uma vez e guardados, atualizados quando a config carrega ou muda.
 * Os mixins consultam isto a cada frame, entao precisa ser so leitura de campo.
 */
public final class WaterFramesSettings {
    private static volatile boolean moduleEnabled = true;
    private static volatile boolean vanillaStyle = true;
    private static volatile boolean grayscaleIcons = true;
    private static volatile boolean keepErrorRed = true;
    private static volatile boolean swapResizeIcons = true;

    public static void onConfigChanged(ModConfig config) {
        if (NexusCore.MOD_ID.equals(config.getModId())) {
            refresh();
        }
    }

    private static void refresh() {
        moduleEnabled = CommonConfig.isModuleEnabled(ModuleIds.COMPAT_WATERFRAMES);
        vanillaStyle = ClientConfig.get(ClientConfig.WATERFRAMES_VANILLA_STYLE);
        grayscaleIcons = ClientConfig.get(ClientConfig.WATERFRAMES_GRAYSCALE_ICONS);
        keepErrorRed = ClientConfig.get(ClientConfig.WATERFRAMES_KEEP_ERROR_RED);
        swapResizeIcons = ClientConfig.get(ClientConfig.WATERFRAMES_SWAP_RESIZE_ICONS);
    }

    public static boolean vanillaStyle() {
        return moduleEnabled && vanillaStyle;
    }

    public static boolean grayscaleIcons() {
        return moduleEnabled && grayscaleIcons;
    }

    public static boolean keepErrorRed() {
        return keepErrorRed;
    }

    public static boolean swapResizeIcons() {
        return moduleEnabled && swapResizeIcons;
    }

    private WaterFramesSettings() {}
}
