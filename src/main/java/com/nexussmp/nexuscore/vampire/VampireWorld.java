package com.nexussmp.nexuscore.vampire;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import de.teamlapen.vampirism.api.entity.convertible.IConvertedCreature;
import de.teamlapen.vampirism.api.entity.factions.IFactionEntity;
import de.teamlapen.vampirism.api.entity.minion.IMinionEntity;
import de.teamlapen.vampirism.blocks.TotemBaseBlock;
import de.teamlapen.vampirism.blocks.TotemTopBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * O mundo sem o lado "jogo de mob" do Vampirism.
 *
 * <p>Aqui vampiro e jogador — lorde ou comum — e nada mais: nao existe vampiro de mato, cacador,
 * baron, treinador nem vila de faccao. A geracao nova ja nasce sem isso (as opcoes em
 * {@link VampirismSettings#applyWorldgen()} e os arquivos de {@code data/vampirism/} deste jar); o
 * que esta aqui cuida do que o mundo ja tinha antes:
 *
 * <ul>
 *   <li><b>Totem de vila.</b> Nao existe, de jeito nenhum: e ele que faz a vila ser "dos vampiros"
 *       ou "dos cacadores" e que poe mob de faccao nela. Todo totem — o que veio com a vila e o que
 *       alguem fabricou — sai, com a base, no tick seguinte ao chunk carregar; colocar um novo e
 *       recusado; e as receitas do topo e da base foram tiradas ({@code data/vampirism/recipe/}).</li>
 *   <li><b>Mob do Vampirism salvo no mundo.</b> Quem ja existia antes — o cacador do acampamento, o
 *       baron da floresta, o treinador da vila — some quando o chunk dele carrega.</li>
 * </ul>
 *
 * <p>O que fica: o minion de um lorde (e dele, e so aparece quando ele chama), a criatura que um
 * lorde converteu com a mordida, e o que um operador puser por comando ou ovo — esses ganham a marca
 * {@link #ALLOWED} ao nascer e continuam existindo depois de o servidor reiniciar.
 *
 * <p>As estruturas que ja foram geradas (tenda, cabana, cripta) continuam de pe como construcao, sem
 * ninguem dentro: apagar pedaco de mundo ja explorado nao e papel deste mod.
 */
final class VampireWorld {
    /** Marca de quem nasceu por um caminho permitido (comando, ovo, conversao) e pode ficar. */
    static final String ALLOWED = "nexuscore_vampirism_allowed";
    private static final String VAMPIRISM = "vampirism";
    /** Quantos totens saem por tick, no maximo: um mapa inteiro carregando de uma vez nao trava nada. */
    private static final int TOTEMS_PER_TICK = 16;

    /** Totens achados ao carregar chunk, esperando o tick para sair. */
    private static final Queue<GlobalPos> TOTEMS = new ConcurrentLinkedQueue<>();

    /**
     * Se esta entidade e um mob do lado "jogo" do Vampirism: de faccao, ou simplesmente do mod.
     *
     * <p>Ficam de fora o minion de lorde, a criatura convertida pela mordida e o aldeao — um aldeao
     * que a vila de faccao deixou agressivo continua sendo morador daquela vila.
     */
    static boolean isWildMob(Entity entity) {
        if (!(entity instanceof Mob) || entity instanceof IConvertedCreature<?> || entity instanceof IMinionEntity
                || entity instanceof AbstractVillager) {
            return false;
        }
        return entity instanceof IFactionEntity
                || VAMPIRISM.equals(EntityType.getKey(entity.getType()).getNamespace());
    }

    private static boolean off() {
        return !CommonConfig.isModuleEnabled(ModuleIds.VAMPIRE);
    }

    /**
     * A porta de entrada do mundo, para mob do Vampirism.
     *
     * <p>Quem chega <b>agora</b> ja passou pelas regras de nascimento ({@link VampireRules}): se nao
     * foi barrado la, veio por comando, ovo ou conversao, e ganha a marca para continuar existindo.
     * Quem chega <b>do disco</b> — ou da geracao de mundo, que o jogo entrega do mesmo jeito — sem a
     * marca e sobra de antes, e nao volta.
     */
    static void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel().isClientSide() || off() || !isWildMob(entity)) {
            return;
        }
        if (!event.loadedFromDisk()) {
            if (!((Mob) entity).isSpawnCancelled()) {
                entity.addTag(ALLOWED);
            }
            return;
        }
        if (!entity.getTags().contains(ALLOWED)) {
            event.setCanceled(true);
        }
    }

    /** Totem num chunk que acabou de carregar: anotado para sair no tick seguinte. */
    static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)
                || off()) {
            return;
        }
        // As posicoes de block entity, criadas ou ainda por criar: o topo do totem tem uma, entao e
        // so ali que ele pode estar. Olhar o bloco, e nao a block entity, vale mesmo antes de o jogo
        // montar as block entities do chunk.
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (chunk.getBlockState(pos).getBlock() instanceof TotemTopBlock) {
                TOTEMS.add(GlobalPos.of(level.dimension(), pos.immutable()));
            }
        }
    }

    /**
     * Tira os totens anotados. Fora do carregamento do chunk de proposito: mexer em bloco enquanto o
     * chunk ainda esta subindo e pedir problema.
     */
    static void onServerTick(ServerTickEvent.Post event) {
        if (TOTEMS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        for (int i = 0; i < TOTEMS_PER_TICK; i++) {
            GlobalPos totem = TOTEMS.poll();
            if (totem == null) {
                return;
            }
            ServerLevel level = server.getLevel(totem.dimension());
            BlockPos pos = totem.pos();
            if (level == null || !level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof TotemTopBlock)) {
                continue;
            }
            level.removeBlock(pos, false);
            if (level.getBlockState(pos.below()).getBlock() instanceof TotemBaseBlock) {
                level.removeBlock(pos.below(), false);
            }
            NexusCore.LOGGER.info("[NexusCore] vampiro: totem de vila removido em {} {}",
                    totem.dimension().location(), pos.toShortString());
        }
    }

    /** Totem nenhum e colocado: nem o topo, nem a base. Quem ainda tiver um na mao fica com ele. */
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Block block = event.getPlacedBlock().getBlock();
        if ((block instanceof TotemTopBlock || block instanceof TotemBaseBlock) && !off()) {
            event.setCanceled(true);
        }
    }

    static void clear() {
        TOTEMS.clear();
    }

    private VampireWorld() {}
}
