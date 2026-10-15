package com.nexussmp.nexuscore.relic.client;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.relic.heart.EmberHeartItem;
import com.nexussmp.nexuscore.relic.purity.PurityAmulet;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Parte de cliente das reliquias: o desenho do corpo astral, as texturas que mudam (o coracao em
 * cinzas, as gotas do amuleto), a tecla de voltar ao corpo, o voo da pena e a loucura na tela.
 */
public final class RelicClientModule implements NexusModule {

    @Override
    public String id() {
        return ModuleIds.RELIC;
    }

    @Override
    public boolean clientOnly() {
        return true;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event ->
                event.registerEntityRenderer(NexusRegistries.ASTRAL_BODY.get(), AstralBodyRenderer::new));
        modBus.addListener(RegisterKeyMappingsEvent.class, event -> event.register(AstralClient.RETURN_KEY));
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> {
            // O coracao em cinzas e as gotas acesas do amuleto: o modelo do item escolhe a textura.
            ItemProperties.register(NexusRegistries.EMBER_HEART.get(), NexusCore.id("dormant"),
                    (stack, level, entity, seed) -> level != null && EmberHeartItem.dormant(level, stack) ? 1.0F : 0.0F);
            ItemProperties.register(NexusRegistries.PURITY_AMULET.get(), NexusCore.id("drops"),
                    (stack, level, entity, seed) -> PurityAmulet.drops(stack) / (float) PurityAmulet.MAX_DROPS);
        }));

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(PlayerTickEvent.Pre.class, FeatherClient::onPlayerTick);
        game.addListener(ClientTickEvent.Post.class, AstralClient::onClientTick);
        game.addListener(RenderGuiEvent.Post.class, AstralClient::onRenderGui);
        game.addListener(ViewportEvent.ComputeCameraAngles.class, AstralClient::onCameraAngles);
        game.addListener(ViewportEvent.ComputeFov.class, AstralClient::onFov);
        game.addListener(ViewportEvent.RenderFog.class, AstralClient::onRenderFog);
        game.addListener(ViewportEvent.ComputeFogColor.class, AstralClient::onFogColor);
        game.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> AstralClient.stop());
    }
}
