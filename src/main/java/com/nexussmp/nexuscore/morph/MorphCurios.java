package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * O registro do anel no Curios, numa classe so dele. Fica fora do {@link MorphModule} de proposito:
 * o Java confere os tipos de uma classe inteira quando ela carrega, e o modulo carrega sempre (ate
 * para descobrir que falta o Curios). Com a chamada la dentro, a falta do Curios derrubava o mod.
 * Esta classe so e tocada depois que o modulo confirmou que o Curios esta instalado.
 */
final class MorphCurios {

    /** Clique direito com o anel na mao veste o anel, como armadura (o Curios faz a troca). */
    static void register() {
        CuriosApi.registerCurio(NexusRegistries.MORPH_RING.get(), new MorphRingCurio());
    }

    private MorphCurios() {}
}
