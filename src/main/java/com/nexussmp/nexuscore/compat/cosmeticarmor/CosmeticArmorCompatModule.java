package com.nexussmp.nexuscore.compat.cosmeticarmor;

import com.nexussmp.nexuscore.compat.cosmeticarmor.client.CosmeticArmorButtons;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

/**
 * Poe os botoes do Cosmetic Armor no mesmo desenho dos do Curios.
 *
 * <p>Os dois mods penduram botao no inventario e cada um desenha do seu jeito; lado a lado, a
 * diferenca aparece. O que muda aqui e so visual — icone, posicao e a dica que aparece ao passar o
 * mouse. O que os botoes fazem continua sendo do Cosmetic Armor, ver {@link CosmeticArmorButtons}.
 */
public final class CosmeticArmorCompatModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.COMPAT_COSMETICARMOR;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(ModRequirement.any("cosmeticarmorreworked"));
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        // LOWEST de proposito: o botao geral do Cosmetic Armor entra por este mesmo evento, em
        // prioridade normal. Precisa ja estar na tela para ser tirado dela.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ScreenEvent.Init.Post.class,
                CosmeticArmorButtons::onScreenInit);
        // O botaozinho de visibilidade e desenhado neste segundo passe, e nao junto dos widgets,
        // para o realce do slot sob o mouse nao passar por cima dele.
        NeoForge.EVENT_BUS.addListener(ContainerScreenEvent.Render.Foreground.class,
                CosmeticArmorButtons::onRenderForeground);
    }
}
