package com.nexussmp.nexuscore.core.util;

/**
 * Faixas testadas de cada mod integrado. Fonte unica: usada pelo plugin de mixin (antes dos mods
 * carregarem) e pelo {@code ModuleManager} (depois). Fora da faixa, o modulo desliga com aviso em vez
 * de crashar.
 */
public final class CompatRequirements {

    /** Linha 0.8 do Sodium (Config API backportada para a 1.21.1 na 0.8.12). */
    public static final ModRequirement SODIUM = new ModRequirement("sodium", "[0.8.12,0.9)");

    /**
     * Classes internas do WaterFrames 2.1.x (conferidas no bytecode da 2.1.23).
     * A 3.x troca o CreativeCore pelo WaterUI, e este modulo precisara ser refeito.
     */
    public static final ModRequirement WATERFRAMES = new ModRequirement("waterframes", "[2.1.23,2.2)");

    /** Faixa que o proprio WaterFrames 2.1.23 exige. */
    public static final ModRequirement CREATIVECORE = new ModRequirement("creativecore", "[2.13.13,2.14)");

    public static final ModRequirement LAMBDYNAMICLIGHTS = ModRequirement.any("lambdynlights_runtime");
    public static final ModRequirement FANCYMENU = ModRequirement.any("fancymenu");
    /** Os mixins do correio mexem em classes internas do Envelope: a faixa e a testada. */
    public static final ModRequirement ENVELOPE = new ModRequirement("envelope", "[0.7.5,0.8)");
    /**
     * O mixin da magia mexe em metodos de {@code AbstractSpell} (conferidos na 1.21.1-3.16.1): a faixa
     * e a linha 3.16 da 1.21.1.
     */
    public static final ModRequirement IRONS_SPELLBOOKS = new ModRequirement("irons_spellbooks", "[1.21.1-3.16,1.21.1-3.17)");

    private CompatRequirements() {}
}
