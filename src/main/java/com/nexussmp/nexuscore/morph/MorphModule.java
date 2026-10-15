package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import com.nexussmp.nexuscore.morph.client.MorphClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

/**
 * O anel da metamorfose: copiar a forma de uma criatura — ou de outro jogador — e virar ela.
 *
 * <p>O que e dos dois lados fica aqui: copiar, vestir, o tamanho, os sons, as habilidades e o pacote.
 * O desenho e a parte do cliente, em {@code morph.client}; a troca dos sons de dano, morte e passo
 * mora nos mixins de {@code mixin.morph}. Detalhes em {@code docs/MORPH.md}.
 *
 * <p>Precisa do Curios, porque e a vaga de anel dele que transforma. O item existe sem ele (os
 * registros sao fixos), so nao tem onde vestir.
 */
public final class MorphModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.MORPH;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(ModRequirement.any("curios"));
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // O lambda do cliente so executa la; o servidor dedicado nunca carrega essa classe.
            registrar.playToClient(MorphPayloads.Sync.TYPE, MorphPayloads.Sync.STREAM_CODEC,
                    (payload, context) -> MorphClient.apply(payload));
        });

        // Clique direito com o anel na mao veste o anel, como armadura (o Curios faz a troca).
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() ->
                CuriosApi.registerCurio(NexusRegistries.MORPH_RING.get(), new MorphRingCurio())));

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(AttackEntityEvent.class, MorphCapture::onAttack);
        // Os dois lados: o tamanho da pessoa tem de ser o mesmo no servidor e em cada cliente.
        game.addListener(EntityEvent.Size.class, MorphState::onSize);
        game.addListener(PlayerTickEvent.Post.class, MorphEquip::onPlayerTick);
        game.addListener(PlayerTickEvent.Post.class, MorphSounds::onPlayerTick);
        game.addListener(LivingBreatheEvent.class, MorphAbilities::onBreathe);
        game.addListener(LivingIncomingDamageEvent.class, MorphAbilities::onIncomingDamage);
        game.addListener(PlayerEvent.StartTracking.class, MorphEquip::onStartTracking);
        game.addListener(PlayerEvent.PlayerRespawnEvent.class, MorphEquip::onRecreated);
        game.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, MorphEquip::onRecreated);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, MorphEquip::onLoggedOut);
        game.addListener(ServerStoppedEvent.class, event -> MorphEquip.clear());
    }
}
