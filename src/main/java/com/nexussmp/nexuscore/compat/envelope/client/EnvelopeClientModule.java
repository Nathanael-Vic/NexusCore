package com.nexussmp.nexuscore.compat.envelope.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.List;

/** Parte de cliente do correio: o morcego desenhado no lugar do pombo do lorde. */
public final class EnvelopeClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.COMPAT_ENVELOPE;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(CompatRequirements.ENVELOPE);
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(RenderLivingEvent.Pre.class, BatCourierClient::onRenderLiving);
        game.addListener(ClientTickEvent.Post.class, BatCourierClient::onClientTick);
        game.addListener(EntityLeaveLevelEvent.class, BatCourierClient::onLeaveLevel);
        game.addListener(LevelEvent.Unload.class, event -> {
            if (event.getLevel().isClientSide()) {
                BatCourierClient.clear();
            }
        });
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> BatCourierClient.clear());
    }
}
