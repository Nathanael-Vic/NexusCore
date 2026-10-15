package com.nexussmp.nexuscore.magic.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Parte de cliente da magia: o que some ou embaralha na tela de quem nao e mago. */
public final class MagicClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.MAGIC;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(CompatRequirements.IRONS_SPELLBOOKS);
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(RenderGuiLayerEvent.Pre.class, MagicClient::onRenderLayer);
        // LOWEST: embaralha depois de todos os mods terem escrito as suas linhas.
        game.addListener(EventPriority.LOWEST, ItemTooltipEvent.class, MagicClient::onTooltip);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> MagicClient.clear());
    }
}
