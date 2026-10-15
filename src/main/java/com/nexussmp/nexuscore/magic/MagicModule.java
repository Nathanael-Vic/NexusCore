package com.nexussmp.nexuscore.magic;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModPresence;
import com.nexussmp.nexuscore.magic.client.MagicClient;
import com.nexussmp.nexuscore.magic.network.MagicPayloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Magia: o Iron's Spells 'n Spellbooks so para quem e mago.
 *
 * <p>O mod continua inteiro no mundo — estruturas, magos inimigos, livros nos baus, os mobs e o que
 * eles lancam. O que muda e quem pode <b>usar</b>: so quem a moderacao pos na lista de magos
 * ({@link Mages}, {@code /nexus mago}). Para o resto, toda magia e desconhecida — o nome embaralhado
 * o tempo todo, os numeros escondidos — e nada de magico responde ao clique: nem lancar, nem ler,
 * nem beber, nem vestir joia, nem abrir mesa de magia. Detalhes em {@code docs/MAGIA.md}.
 *
 * <p>Sem o Iron's Spells (ou fora da faixa testada) as regras nao entram e nenhuma classe dele chega
 * a ser carregada. O <b>pacote</b>, como no modulo de vampiro, e registrado sempre: o NeoForge
 * compara a lista de pacotes dos dois lados na entrada, e um registro de um lado so impede a conexao.
 */
public final class MagicModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.MAGIC;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // O lambda do cliente so executa la; o servidor dedicado nunca carrega essa classe.
            registrar.playToClient(MagicPayloads.Mage.TYPE, MagicPayloads.Mage.STREAM_CODEC,
                    (payload, context) -> MagicClient.apply(payload));
        });

        ModPresence.Result irons = ModPresence.check(CompatRequirements.IRONS_SPELLBOOKS);
        if (!irons.ok()) {
            NexusCore.LOGGER.info("[NexusCore] magic: regras desligadas ({})",
                    irons.describe(CompatRequirements.IRONS_SPELLBOOKS));
            return;
        }
        IEventBus game = NeoForge.EVENT_BUS;
        MagicRules.register(game);
        // O despertar de quem vira mago anda tick a tick.
        game.addListener(ServerTickEvent.Post.class, event -> MagicFx.tick(event.getServer()));
        // Quem entra, volta da morte ou troca de mundo: o cliente novo precisa saber de novo se le magia.
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> sync(event.getEntity()));
        game.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> sync(event.getEntity()));
        game.addListener(PlayerEvent.PlayerChangedDimensionEvent.class, event -> sync(event.getEntity()));
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> MagicFx.forget(event.getEntity().getUUID()));
        game.addListener(ServerStoppedEvent.class, event -> MagicFx.clear());
    }

    private static void sync(Player player) {
        if (player instanceof ServerPlayer server) {
            Mages.sync(server);
        }
    }
}
