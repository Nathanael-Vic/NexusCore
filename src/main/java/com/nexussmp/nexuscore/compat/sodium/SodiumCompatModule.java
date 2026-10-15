package com.nexussmp.nexuscore.compat.sodium;

import com.nexussmp.nexuscore.compat.sodium.config.XsoConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

import java.util.List;

/**
 * Tela de opcoes do Sodium no estilo vanilla (port do Xander's Sodium Options, LGPL-3.0).
 * Os mixins ficam em {@code mixin.sodium} e so aplicam com Sodium na faixa testada.
 */
public final class SodiumCompatModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.COMPAT_SODIUM;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(CompatRequirements.SODIUM);
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        // Mesmo momento em que o XSO carregava: no construtor do mod.
        XsoConfig.load();
    }
}
