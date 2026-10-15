package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.core.config.ServerConfig;

/** O que a configuracao do servidor deixa copiar. */
final class MorphSettings {

    static boolean allowPlayers() {
        return ServerConfig.get(ServerConfig.MORPH_ALLOW_PLAYERS);
    }

    /**
     * Se esta forma pode ser usada. Conferido ao copiar e de novo ao vestir: a lista pode ter mudado
     * depois que alguem guardou a forma no anel.
     */
    static boolean allowed(MorphForm form) {
        if (form.isPlayer()) {
            return allowPlayers();
        }
        return !ServerConfig.get(ServerConfig.MORPH_BLOCKED_ENTITIES).contains(form.type().toString());
    }

    private MorphSettings() {}
}
