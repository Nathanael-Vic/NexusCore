package com.nexussmp.nexuscore.vampire;

import com.nexussmp.nexuscore.NexusCore;
import de.teamlapen.vampirism.config.VampirismConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * As opcoes do Vampirism de que este servidor depende, aplicadas por conta propria.
 *
 * <p>Sao decisoes que o mod so oferece como configuracao, e que aqui nao sao opcionais: um vampiro
 * de nivel 1 <b>tem</b> que queimar no sol (senao "vampiro capado" vira "humano com barra de
 * sangue"), e nenhuma mordida de bicho nem dente no inventario pode transformar alguem — a unica
 * porta para o vampirismo e a mordida de um lorde.
 *
 * <p>Isso e mexido na memoria, e nao no arquivo: quem abrir o {@code vampirism-balance.toml} vai
 * continuar vendo o valor que escreveu la. E de proposito — nao e papel deste mod reescrever a
 * configuracao de outro. O que fica registrado e a linha no log dizendo o que foi mudado e por que.
 *
 * <p>A geracao de mundo tem hora propria ({@link #applyWorldgen()}): o Vampirism le essas opcoes uma
 * vez so, no fim do carregamento do jogo ou quando o mundo abre, e por isso elas sao ajustadas no
 * setup, antes de ele ler. A outra metade do corte da geracao — o totem e o treinador nas vilas, as
 * estruturas e o spawn natural — vai nos arquivos de {@code data/vampirism/} deste jar.
 */
public final class VampirismSettings {

    /**
     * O mundo nao gera nada do lado "jogo de mob" do Vampirism.
     *
     * <p>Aqui o vampirismo e dos jogadores — lorde e vampiro comum — e so. A floresta dos vampiros, o
     * acampamento de cacador, a igreja que o Vampirism poe no lugar do templo das vilas e o totem e o
     * treinador que ele pendura nas vilas nao tem lugar nisso.
     *
     * <p>Chamado no setup do jogo. O bioma entra no mundo no fim do carregamento, e as vilas quando o
     * mundo abre: os dois leem depois disto. Mundo que ja existe mantem o que ja foi gerado; so o chao
     * novo nasce sem.
     */
    public static void applyWorldgen() {
        force("addVampireForestToOverworld", VampirismConfig.COMMON.addVampireForestToOverworld, false);
        force("enableHunterTentGeneration", VampirismConfig.COMMON.enableHunterTentGeneration, false);
        force("villageReplaceTemples", VampirismConfig.COMMON.villageReplaceTemples, false);
        // Sem o Lithostitched, o Vampirism poe o totem e o treinador nas vilas por conta propria, com
        // estes pesos. Com ele, quem cala os dois sao os arquivos de data/vampirism/lithostitched/.
        force("villageTotemWeight", VampirismConfig.COMMON.villageTotemWeight, 0);
        force("villageHunterTrainerWeight", VampirismConfig.COMMON.villageHunterTrainerWeight, 0);
    }

    public static void apply() {
        // Nivel 1 ja queima. O padrao e 4 — o que deixaria o vampiro comum imune ao sol para sempre,
        // justamente por ele nunca passar do 1.
        force("sundamageMinLevel", VampirismConfig.BALANCE.vpSundamageMinLevel, 1);
        // As outras portas de entrada do vampirismo fecham: so a mordida de um lorde transforma.
        force("disableMobBiteInfection", VampirismConfig.SERVER.disableMobBiteInfection, true);
        force("disableFangInfection", VampirismConfig.SERVER.disableFangInfection, true);
    }

    private static void force(String name, ModConfigSpec.ConfigValue<Integer> option, int wanted) {
        try {
            if (option.get() == wanted) {
                return;
            }
            NexusCore.LOGGER.info("[NexusCore] vampiro: {} {} -> {}", name, option.get(), wanted);
            option.set(wanted);
        } catch (Throwable error) {
            NexusCore.LOGGER.warn("[NexusCore] vampiro: nao deu para ajustar {}", name, error);
        }
    }

    private static void force(String name, ModConfigSpec.ConfigValue<Boolean> option, boolean wanted) {
        try {
            if (option.get() == wanted) {
                return;
            }
            NexusCore.LOGGER.info("[NexusCore] vampiro: {} {} -> {}", name, option.get(), wanted);
            option.set(wanted);
        } catch (Throwable error) {
            NexusCore.LOGGER.warn("[NexusCore] vampiro: nao deu para ajustar {}", name, error);
        }
    }

    private VampirismSettings() {}
}
