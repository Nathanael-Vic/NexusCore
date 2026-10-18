package com.nexussmp.nexuscore.monolith;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.List;

/**
 * O "eco" de um monolito: o quanto o mundo esta errado num ponto, e o que de errado tem em cada chunk.
 *
 * <p>A intensidade vai de 0 a 1. Longe ({@value #REACH} blocos alem da estrutura) e zero; vai subindo
 * devagar, com quase nada nos primeiros cem blocos, e chega ao maximo logo antes da estrutura. Na
 * estrutura em si cai a zero de novo: o monolito e o unico lugar inteiro no meio do estrago.
 *
 * <ul>
 *   <li><b>Estranho</b> (ate 0,25): da para achar que foi impressao. Uma arvore com folha de outra
 *       cor, minerio no meio da grama, bicho parado encarando.</li>
 *   <li><b>Errado</b> (ate 0,6): chunks de outro bioma com corte reto, blocos flutuando, arvore de
 *       cabeca para baixo, placa com texto corrompido, bicho com nome de sistema.</li>
 *   <li><b>Quebrado</b> (acima): chunks vazios ate o fundo, blocos de textura faltando, bicho com a
 *       textura roxa e preta.</li>
 * </ul>
 *
 * <p>Tudo aqui e funcao da semente e da posicao: o mesmo mundo sempre sai igual.
 */
public final class Echo {
    /** Ate quantos blocos alem da borda da estrutura o eco alcanca. */
    public static final int REACH = 360;
    /** Os primeiros blocos fora da estrutura: o eco sobe de zero ao maximo. */
    private static final int RISE = 24;

    public static final float STRANGE = 0.25F;
    public static final float WRONG = 0.6F;

    /** A intensidade num ponto: a do monolito mais forte ali. */
    public static float intensity(List<MonolithSites.Site> sites, double x, double z) {
        float strongest = 0.0F;
        for (MonolithSites.Site site : sites) {
            strongest = Math.max(strongest, intensity(site, x, z));
        }
        return strongest;
    }

    public static float intensity(MonolithSites.Site site, double x, double z) {
        double edge = site.horizontalDistance(x, z) - site.coreRadius();
        if (edge <= 0.0 || edge >= REACH) {
            return 0.0F;
        }
        if (edge < RISE) {
            return (float) (edge / RISE);
        }
        double far = (edge - RISE) / (REACH - RISE);
        return (float) ((1.0 - far) * (1.0 - far));
    }

    // ---- o que tem em cada chunk ----

    /** As peles de bioma que um chunk pode vestir. */
    public enum Skin { SNOW, DESERT, BADLANDS, MUSHROOM, CHERRY }

    /**
     * O plano de um chunk. No maximo uma coisa grande (vazio ou outro bioma) e algumas pequenas.
     *
     * @param skin    o bioma estranho que o chunk veste, ou {@code null}
     * @param shift   quanto o chao desse bioma sobe (ou desce), em blocos
     * @param floating quantos blocos soltos no ar
     */
    public record Plan(float intensity, boolean hollow, Skin skin, int shift, boolean wrongLeaves, boolean ore,
                       int floating, boolean invertedTree, boolean sign, boolean nullPatch, long seed) {
        public static final Plan NOTHING = new Plan(0.0F, false, null, 0, false, false, 0, false, false, false, 0L);

        public boolean empty() {
            return !hollow && skin == null && !wrongLeaves && !ore && floating == 0 && !invertedTree && !sign && !nullPatch;
        }
    }

    public static Plan plan(long worldSeed, ChunkPos chunk, List<MonolithSites.Site> sites) {
        float f = intensity(sites, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
        if (f <= 0.0F) {
            return Plan.NOTHING;
        }
        long seed = worldSeed ^ (chunk.toLong() * 0x9E3779B97F4A7C15L) ^ 0x6D6F6E6F6C697468L;
        RandomSource random = new XoroshiroRandomSource(seed);

        boolean hollow = false;
        Skin skin = null;
        int shift = 0;
        if (f > WRONG && random.nextFloat() < Mth.lerp((f - WRONG) / (1.0F - WRONG), 0.05F, 0.14F)) {
            hollow = true;
        } else if (f > STRANGE && random.nextFloat() < Mth.lerp((f - STRANGE) / (1.0F - STRANGE), 0.06F, 0.24F)) {
            Skin[] skins = Skin.values();
            skin = skins[random.nextInt(skins.length)];
            // Quase sempre perto da altura de antes: um degrau, nao uma montanha.
            shift = random.nextInt(9) - 4;
        }

        boolean wrongLeaves = random.nextFloat() < 0.12F + 0.12F * f;
        boolean ore = random.nextFloat() < 0.06F + 0.08F * f;
        int floating = f > STRANGE && random.nextFloat() < 0.12F + 0.2F * f ? 1 + random.nextInt(f > WRONG ? 4 : 2) : 0;
        boolean invertedTree = f > STRANGE && random.nextFloat() < 0.05F + 0.06F * f;
        boolean sign = f > STRANGE && random.nextFloat() < 0.04F + 0.05F * f;
        boolean nullPatch = f > WRONG && random.nextFloat() < 0.18F + 0.2F * f;
        if (hollow) {
            // No vazio nao sobra chao para arvore, placa ou minerio.
            return new Plan(f, true, null, 0, false, false, 0, false, false, false, seed);
        }
        return new Plan(f, false, skin, shift, wrongLeaves, ore, floating, invertedTree, sign, nullPatch, seed);
    }

    private Echo() {}
}
