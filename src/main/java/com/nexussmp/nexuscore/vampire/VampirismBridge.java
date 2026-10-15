package com.nexussmp.nexuscore.vampire;

import com.nexussmp.nexuscore.NexusCore;
import de.teamlapen.lib.lib.storage.ISyncable;
import de.teamlapen.vampirism.api.VReference;
import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFactionPlayerHandler;
import de.teamlapen.vampirism.api.entity.player.IFactionPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/**
 * O contato com o Vampirism. Ninguem chama esta classe sem antes confirmar que o mod esta la.
 *
 * <p>Cada metodo e curto e engole o que der errado: se uma versao nova do Vampirism mudar alguma
 * coisa por baixo, o servidor perde <i>esta</i> regra e escreve no log, em vez de cair.
 */
final class VampirismBridge {
    /** O nome do campo no pacote de sincronia do sangue do Vampirism. */
    private static final String BLOOD_SATURATION = "bloodSaturation";
    private static boolean saturationComplained;

    static boolean isVampire(Player player) {
        try {
            IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(player);
            return handler != null && handler.isInFaction(VReference.VAMPIRE_FACTION);
        } catch (Throwable error) {
            complain("ler a faccao", error);
            return false;
        }
    }

    static int level(Player player) {
        try {
            IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(player);
            return handler == null ? 0 : handler.getCurrentLevel(VReference.VAMPIRE_FACTION);
        } catch (Throwable error) {
            complain("ler o nivel", error);
            return 0;
        }
    }

    /** Nivel 0 e a saida da faccao: e assim que o proprio Vampirism cura alguem. */
    static boolean setLevel(Player player, int level) {
        try {
            IFactionPlayerHandler handler = VampirismAPI.factionPlayerHandler(player);
            return handler != null && handler.setFactionAndLevel(VReference.VAMPIRE_FACTION, level);
        } catch (Throwable error) {
            complain("mudar o nivel", error);
            return false;
        }
    }

    /** Apaga habilidades e pontos: o que foi aprendido como lorde nao sobrevive ao rebaixamento. */
    static void forget(Player player) {
        try {
            // vampirePlayer, e nao getVampirePlayer: o segundo esta marcado para sair e so embrulhava
            // o primeiro num Optional sempre cheio.
            IFactionPlayer<?> vampire = VampirismAPI.vampirePlayer(player);
            vampire.getSkillHandler().resetSkills();
            vampire.getSkillHandler().resetRefinements();
        } catch (Throwable error) {
            complain("apagar as habilidades", error);
        }
    }

    /**
     * A saturacao do sangue: o que o AppleSkin mostra como contorno amarelo, so que do vampiro.
     * {@code NaN} quando nao deu para ler.
     *
     * <p>O Vampirism nao tem um metodo para ela: o numero fica num campo privado. O caminho sem
     * reflexao e o pacote que ele mesmo monta para o cliente, que traz esse campo com o nome
     * {@code bloodSaturation} — e esse nome e estavel, porque e o mesmo com que ele grava o jogador
     * no disco. Chamado a cada poucos ticks, so para vampiros; o pacote dele tem tres campos.
     *
     * <p>Se falhar, reclama uma vez so: isto roda o tempo todo, e o log inteiro cheio da mesma linha
     * nao ajuda ninguem.
     */
    static float bloodSaturation(Player player) {
        try {
            if (VampirismAPI.vampirePlayer(player).getBloodStats() instanceof ISyncable stats) {
                CompoundTag tag = stats.serializeUpdateNBT(player.registryAccess());
                if (tag.contains(BLOOD_SATURATION)) {
                    return tag.getFloat(BLOOD_SATURATION);
                }
            }
        } catch (Throwable error) {
            if (!saturationComplained) {
                saturationComplained = true;
                complain("ler a saturacao do sangue", error);
            }
        }
        return Float.NaN;
    }

    private static void complain(String what, Throwable error) {
        NexusCore.LOGGER.warn("[NexusCore] vampiro: nao deu para {} pelo Vampirism", what, error);
    }

    private VampirismBridge() {}
}
