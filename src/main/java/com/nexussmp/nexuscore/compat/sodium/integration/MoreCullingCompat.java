/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.compat.sodium.integration;

import ca.fxco.moreculling.utils.CacheUtils;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import net.minecraft.network.chat.Component;

public class MoreCullingCompat {

    public static void addResetCacheButton(OptionGroup.Builder groupBuilder) {
        groupBuilder.option(ButtonOption.createBuilder()
                .name(Component.translatable("moreculling.config.resetCache"))
                .text(Component.literal("➔"))
                .description(OptionDescription.of(Component.translatable("options.moreculling.resetcache.description")))
                .action((screen, button) -> CacheUtils.resetAllCache())
                .build());
    }
}
