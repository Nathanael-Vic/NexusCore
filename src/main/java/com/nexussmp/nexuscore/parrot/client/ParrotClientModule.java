package com.nexussmp.nexuscore.parrot.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Parte de cliente do papagaio de ombro: so o desenho. */
public final class ParrotClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.PARROT;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerEntityRenderer(NexusRegistries.SHOULDER_PARROT.get(), ShoulderParrotRenderer::new));
    }
}
