package com.nexussmp.nexuscore.hologram.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.ClientConfig;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import net.neoforged.fml.config.ModConfig;

/**
 * O visual do holograma, lido da config e guardado em campos: o shader e o renderer consultam isto
 * a cada frame. Mexer nos valores em {@code nexuscore-client.toml} muda o efeito sem recompilar nada.
 */
public final class HologramClientSettings {
    private static volatile boolean enabled = true;
    private static volatile float red = 0.45F;
    private static volatile float green = 0.85F;
    private static volatile float blue = 1.0F;
    private static volatile float alpha = 0.62F;
    private static volatile float scanlines = 12.0F;
    private static volatile float flicker = 0.08F;
    private static volatile float rim = 0.55F;
    private static volatile boolean particles = true;
    private static volatile boolean items = true;

    static void onConfigChanged(ModConfig config) {
        if (!NexusCore.MOD_ID.equals(config.getModId())) {
            return;
        }
        enabled = CommonConfig.isModuleEnabled(ModuleIds.HOLOGRAM);
        int color = parseColor(ClientConfig.get(ClientConfig.HOLOGRAM_COLOR));
        red = ((color >> 16) & 0xFF) / 255.0F;
        green = ((color >> 8) & 0xFF) / 255.0F;
        blue = (color & 0xFF) / 255.0F;
        alpha = (float) ClientConfig.get(ClientConfig.HOLOGRAM_ALPHA);
        scanlines = (float) ClientConfig.get(ClientConfig.HOLOGRAM_SCANLINES);
        flicker = (float) ClientConfig.get(ClientConfig.HOLOGRAM_FLICKER);
        rim = (float) ClientConfig.get(ClientConfig.HOLOGRAM_RIM);
        particles = ClientConfig.get(ClientConfig.HOLOGRAM_PARTICLES);
        items = ClientConfig.get(ClientConfig.HOLOGRAM_ITEMS);
    }

    private static int parseColor(String value) {
        try {
            return Integer.parseInt(value.replace("#", "").trim(), 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0x74D8FF;
        }
    }

    public static boolean enabled() {
        return enabled;
    }

    public static float red() {
        return red;
    }

    public static float green() {
        return green;
    }

    public static float blue() {
        return blue;
    }

    public static float alpha() {
        return alpha;
    }

    public static float scanlines() {
        return scanlines;
    }

    public static float flicker() {
        return flicker;
    }

    public static float rim() {
        return rim;
    }

    public static boolean particles() {
        return particles;
    }

    public static boolean items() {
        return items;
    }

    private HologramClientSettings() {}
}
