package com.nexussmp.nexuscore.core.module;

import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.List;

/**
 * Um modulo do NexusCore. Contrato simples: identidade, o que precisa estar instalado, e o registro.
 *
 * <p>O {@link ModuleManager} so chama {@link #register} se todos os {@link #requirements()} estiverem
 * presentes e na faixa de versao. O liga/desliga da config e lido em tempo de execucao, porque as
 * configs do NeoForge so carregam depois do construtor do mod.
 */
public interface NexusModule {

    /** Ex.: {@code "compat.sodium"}. Tambem e a chave em {@code modules.<id>.enabled}. */
    String id();

    default List<ModRequirement> requirements() {
        return List.of();
    }

    default boolean clientOnly() {
        return false;
    }

    void register(IEventBus modBus, ModContainer container);
}
