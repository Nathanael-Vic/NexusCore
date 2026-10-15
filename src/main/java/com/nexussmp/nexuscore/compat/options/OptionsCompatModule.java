package com.nexussmp.nexuscore.compat.options;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * Mantem a tela de opcoes do jogo do jeito que ela nasceu.
 *
 * <p>O trabalho todo esta no mixin {@code options.OptionsHeaderMixin}, que roda na montagem da tela;
 * o modulo existe para dar a chave de liga/desliga em {@code [modules.compat.options]} e a linha no
 * log de modulos. Por isso nao registra listener nenhum.
 */
public final class OptionsCompatModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.COMPAT_OPTIONS;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {}
}
