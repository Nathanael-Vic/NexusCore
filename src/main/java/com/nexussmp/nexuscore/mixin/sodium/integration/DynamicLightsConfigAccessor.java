/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.mixin.sodium.integration;

import dev.lambdaurora.lambdynlights.ChunkRebuildSchedulerMode;
import dev.lambdaurora.lambdynlights.DynamicLightsConfig;
import dev.lambdaurora.lambdynlights.DynamicLightsMode;
import dev.lambdaurora.lambdynlights.ExplosiveLightingMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DynamicLightsConfig.class)
public interface DynamicLightsConfigAccessor {
    @Accessor("DEFAULT_DYNAMIC_LIGHTS_MODE")
    static DynamicLightsMode getDefaultDynamicLightsMode() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DEFAULT_CHUNK_REBUILD_SCHEDULER_MODE")
    static ChunkRebuildSchedulerMode getDefaultChunkRebuildSchedulerMode() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DEFAULT_SLOW_TICKING_DISTANCE")
    static int getDefaultSlowTickingDistance() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DEFAULT_SLOWER_TICKING_DISTANCE")
    static int getDefaultSlowerTickingDistance() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DEFAULT_CREEPER_LIGHTING_MODE")
    static ExplosiveLightingMode getDefaultCreeperLightingMode() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DEFAULT_TNT_LIGHTING_MODE")
    static ExplosiveLightingMode getDefaultTntLightingMode() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DEFAULT_DEBUG_CELL_DISPLAY_RADIUS")
    static int getDefaultDebugCellDisplayRadius() {
        throw new UnsupportedOperationException();
    }

    @Accessor("DEFAULT_DEBUG_LIGHT_LEVEL_RADIUS")
    static int getDefaultDebugLightLevelRadius() {
        throw new UnsupportedOperationException();
    }
}
