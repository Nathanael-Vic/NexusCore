package com.nexussmp.nexuscore.relic;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.relic.astral.AstralPayloads;
import com.nexussmp.nexuscore.relic.astral.AstralProjection;
import com.nexussmp.nexuscore.relic.client.AstralClient;
import com.nexussmp.nexuscore.relic.flight.WindFeather;
import com.nexussmp.nexuscore.relic.heart.EmberHeart;
import com.nexussmp.nexuscore.relic.purity.PurityAmulet;
import com.nexussmp.nexuscore.relic.refuge.Refuges;
import com.nexussmp.nexuscore.relic.stone.UnyieldingStoneBlock;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * As reliquias: itens unicos que um operador entrega, que fazem coisas que nada mais faz, e que
 * nunca saem de quem as carrega.
 *
 * <ul>
 *   <li><b>Chave do refugio</b> ({@code relic.refuge}) — uma porta para a sua sala particular.</li>
 *   <li><b>Pena dos ventos</b> ({@code relic.flight}) — planar sem elytra.</li>
 *   <li><b>Coracao de brasa</b> ({@code relic.heart}) — mais forte quanto mais perto de morrer.</li>
 *   <li><b>Mascara astral</b> ({@code relic.astral}) — sair do corpo, e perder a cabeca la fora.</li>
 *   <li><b>Amuleto da pureza</b> ({@code relic.purity}) — tres gotas que bebem efeitos ruins.</li>
 *   <li><b>Pedra inabalavel</b> ({@code relic.stone}) — um bloco que so o dono tira.</li>
 *   <li><b>Bolsa sem fundo</b> ({@code relic.bag}) — um bau duplo que nao cai na morte.</li>
 * </ul>
 *
 * <p>O que e de todas — ficar com o dono na morte, nao queimar, nao sumir — esta no
 * {@link RelicKeeper} e no {@link RelicItem}. Os registros ficam em {@code NexusRegistries}: com o
 * modulo desligado os itens existem, so nao fazem nada. Detalhes em {@code docs/RELIQUIAS.md}.
 */
public final class RelicModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.RELIC;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // O lambda do cliente so executa la; o servidor dedicado nunca carrega essa classe.
            registrar.playToClient(AstralPayloads.State.TYPE, AstralPayloads.State.STREAM_CODEC,
                    (payload, context) -> AstralClient.apply(payload));
            registrar.playToServer(AstralPayloads.Return.TYPE, AstralPayloads.Return.STREAM_CODEC,
                    (payload, context) -> {
                        if (context.player() instanceof ServerPlayer player) {
                            AstralProjection.onReturnRequest(player);
                        }
                    });
        });
        modBus.addListener(FMLCommonSetupEvent.class, event -> {
            if (RelicSlots.present()) {
                event.enqueueWork(RelicCurios::register);
            }
        });

        IEventBus game = NeoForge.EVENT_BUS;
        // Na frente de todo mundo: as reliquias saem dos drops antes de o Corpse montar o corpo.
        game.addListener(EventPriority.HIGHEST, LivingDropsEvent.class, RelicKeeper::onDrops);
        game.addListener(PlayerEvent.PlayerRespawnEvent.class, RelicKeeper::onRespawn);
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, RelicKeeper::onLogin);

        game.addListener(ServerTickEvent.Post.class, event -> {
            Refuges.tick(event.getServer());
            AstralProjection.tick(event.getServer());
        });
        game.addListener(PlayerTickEvent.Post.class, WindFeather::onPlayerTick);
        game.addListener(PlayerTickEvent.Post.class, EmberHeart::onPlayerTick);
        game.addListener(PlayerTickEvent.Post.class, PurityAmulet::onPlayerTick);

        game.addListener(PlayerInteractEvent.RightClickItem.class, WindFeather::onUseItem);
        game.addListener(EventPriority.HIGHEST, PlayerInteractEvent.RightClickBlock.class, AstralProjection::onRightClickBlock);
        game.addListener(LivingIncomingDamageEvent.class, EmberHeart::onIncomingDamage);
        game.addListener(MobEffectEvent.Applicable.class, PurityAmulet::onApplicable);
        game.addListener(EventPriority.HIGHEST, BlockEvent.BreakEvent.class, UnyieldingStoneBlock::onBreak);
        game.addListener(EventPriority.HIGHEST, BlockEvent.BreakEvent.class, Refuges::onBreak);

        // Quem morre fora do corpo volta para ele antes de tudo: e la que o corpo (e o resto) cai.
        game.addListener(EventPriority.HIGHEST, LivingDeathEvent.class, AstralProjection::onDeath);
        game.addListener(LivingDeathEvent.class, EmberHeart::onDeath);

        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, AstralProjection::onLogin);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, AstralProjection::onLogout);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            WindFeather.forget(event.getEntity().getUUID());
            EmberHeart.forget(event.getEntity().getUUID());
        });
        game.addListener(ServerStoppedEvent.class, event -> {
            AstralProjection.clear();
            WindFeather.clear();
            EmberHeart.clear();
        });
    }
}
