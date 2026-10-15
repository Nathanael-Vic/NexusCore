package com.nexussmp.nexuscore.space.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.space.SpaceDimensions;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Parte de cliente da dimensao Space: o ceu.
 *
 * <p>No servidor nao roda nada; ele so informa o id do tipo de dimensao, e o cliente liga esse id
 * a estes efeitos.
 */
public final class SpaceClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.SPACE;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterDimensionSpecialEffectsEvent.class, event ->
                event.register(SpaceDimensions.SPACE_ID, new SpaceSkyEffects()));
        modBus.addListener(ModConfigEvent.Loading.class, event -> SpaceClientSettings.onConfigChanged(event.getConfig()));
        modBus.addListener(ModConfigEvent.Reloading.class, event -> SpaceClientSettings.onConfigChanged(event.getConfig()));

        NeoForge.EVENT_BUS.addListener(ViewportEvent.ComputeFogColor.class, SpaceSkyRenderer::onComputeFogColor);
    }
}
