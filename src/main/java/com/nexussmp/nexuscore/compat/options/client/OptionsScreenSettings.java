package com.nexussmp.nexuscore.compat.options.client;

import com.nexussmp.nexuscore.core.config.ClientConfig;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;

/** Leitura da opcao do ajuste da tela de opcoes. Consultado na montagem da tela, nao a cada frame. */
public final class OptionsScreenSettings {

    /** Tirar do cabecalho os botoes que outros mods penduram la. */
    public static boolean removeHeaderExtras() {
        return CommonConfig.isModuleEnabled(ModuleIds.COMPAT_OPTIONS)
                && ClientConfig.get(ClientConfig.OPTIONS_REMOVE_HEADER_EXTRAS);
    }

    private OptionsScreenSettings() {}
}
