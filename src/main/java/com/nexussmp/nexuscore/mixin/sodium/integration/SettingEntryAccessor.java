/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.mixin.sodium.integration;

import dev.lambdaurora.lambdynlights.config.SettingEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SettingEntry.class)
public interface SettingEntryAccessor<T> {
    @Accessor("defaultValue")
    T getDefaultValue();
}
