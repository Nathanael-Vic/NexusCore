package com.nexussmp.nexuscore.morph.client;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.List;

/** Parte de cliente do anel: desenhar a criatura no lugar da pessoa, e deixar ela viver a cada tick. */
public final class MorphClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.MORPH;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(ModRequirement.any("curios"));
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(RenderPlayerEvent.Pre.class, MorphRenderer::onRenderPlayer);
        game.addListener(ClientTickEvent.Post.class, MorphClient::onClientTick);
        game.addListener(RenderArmEvent.class, MorphRenderer::onRenderArm);
        game.addListener(RenderNameTagEvent.class, MorphRenderer::onNameTag);
        game.addListener(EntityJoinLevelEvent.class, MorphRenderer::onJoinLevel);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> MorphClient.clear());
    }
}
