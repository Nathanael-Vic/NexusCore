package com.nexussmp.nexuscore.vampire;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.vampire.client.VampireClient;
import com.nexussmp.nexuscore.vampire.network.VampirePayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Vampiro: o Vampirism cercado para caber neste servidor.
 *
 * <p>A ideia e uma so, e vale a pena dizer em voz alta: <b>o vampirismo aqui e raro e concedido</b>.
 * Um punhado de pessoas — os lordes — vive o mod inteiro; quem for mordido por elas pega a
 * maldicao, e so a maldicao: o sol queima, o sangue falta, e nao ha nivel nem poder no fim disso.
 * Cacador nao existe, e o mundo nao cria vampiro nenhum por conta propria.
 *
 * <p>Sem o Vampirism instalado as regras nao entram, e nenhuma classe que fala com ele chega a ser
 * carregada. O <b>pacote</b>, porem, e registrado sempre: o NeoForge compara a lista de pacotes do
 * cliente e do servidor na hora de entrar, e um registro que aparece so de um lado impede a
 * conexao. Registrar sempre custa nada e evita um "cliente incompativel" dificil de entender.
 */
public final class VampireModule implements NexusModule {
    /** 2: entrou o pacote da saturacao do sangue. Cliente e servidor precisam da mesma versao. */
    private static final String PROTOCOL_VERSION = "2";

    @Override
    public String id() {
        return ModuleIds.VAMPIRE;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // O lambda do cliente so executa la; o servidor dedicado nunca carrega essa classe.
            registrar.playToClient(VampirePayloads.Lord.TYPE, VampirePayloads.Lord.STREAM_CODEC,
                    (payload, context) -> VampireClient.apply(payload));
            registrar.playToClient(VampirePayloads.BloodSaturation.TYPE, VampirePayloads.BloodSaturation.STREAM_CODEC,
                    (payload, context) -> VampireClient.apply(payload));
        });

        if (!Vampires.installed()) {
            NexusCore.LOGGER.info("[NexusCore] vampiro: sem o Vampirism, so o comando fica de pe");
            return;
        }

        // A geracao de mundo do Vampirism le as opcoes dela depois do setup — o bioma no fim do
        // carregamento do jogo, as vilas quando o mundo abre —, entao ajustar aqui chega antes.
        modBus.addListener(FMLCommonSetupEvent.class, event -> {
            if (CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE)) {
                VampirismSettings.applyWorldgen();
            }
        });

        IEventBus game = NeoForge.EVENT_BUS;
        VampireRules.register(game);
        // A coroacao de lorde: o ritual e a revoada de morcegos andam tick a tick.
        game.addListener(ServerTickEvent.Post.class, event -> VampireFx.tick(event.getServer()));
        // O que o mundo ja tinha do lado "jogo de mob": totem de vila e mob salvo. Ver VampireWorld.
        game.addListener(EntityJoinLevelEvent.class, VampireWorld::onEntityJoin);
        game.addListener(ChunkEvent.Load.class, VampireWorld::onChunkLoad);
        game.addListener(BlockEvent.EntityPlaceEvent.class, VampireWorld::onPlace);
        game.addListener(ServerTickEvent.Post.class, VampireWorld::onServerTick);
        // Depois que o mundo abriu: e quando as configuracoes do Vampirism ja estao carregadas.
        game.addListener(ServerStartedEvent.class, event -> VampirismSettings.apply());
        game.addListener(ServerStoppedEvent.class, event -> {
            VampireRules.clear();
            BloodSaturationSync.clear();
            VampireWorld.clear();
            VampireFx.clear();
        });
        game.addListener(PlayerTickEvent.Post.class, BloodSaturationSync::onPlayerTick);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, event -> {
            BloodSaturationSync.forget(event.getEntity().getUUID());
            VampireRules.forget(event.getEntity().getUUID());
            VampireFx.forget(event.getEntity().getUUID());
        });
        game.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Vampires.sync(player);
            }
        });
        // Morrer e voltar recria o jogador: o cliente novo precisa saber de novo quem ele e.
        game.addListener(PlayerEvent.PlayerRespawnEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                Vampires.sync(player);
            }
        });
    }
}
