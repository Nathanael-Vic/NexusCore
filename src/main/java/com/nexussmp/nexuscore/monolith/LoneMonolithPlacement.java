package com.nexussmp.nexuscore.monolith;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nexussmp.nexuscore.mixin.monolith.ChunkGeneratorStructureStateAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Uma estrutura so no mundo inteiro.
 *
 * <p>O posicionamento do jogo espalha estruturas numa grade (uma por regiao), e nao tem como pedir
 * "exatamente uma". Este pede: a partir da semente, cada monolito ganha uma direcao (os seis dividem
 * a volta em fatias de 60 graus, cada um na sua) e uma distancia do centro do mundo entre
 * {@code min_distance} e {@code max_distance}. Dali procura o bioma certo em volta ({@link #locate}).
 * O mesmo mundo sempre da o mesmo lugar.
 *
 * <p>O {@code /locate} do jogo so conhece os posicionamentos dele, entao nao acha estes: o lugar so se
 * descobre andando. Para operador existe {@code /nexus monolito onde}.
 */
public final class LoneMonolithPlacement extends StructurePlacement {
    public static final MapCodec<LoneMonolithPlacement> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.fieldOf("salt").forGetter(LoneMonolithPlacement::salt),
            Codec.intRange(0, 5).fieldOf("slot").forGetter(LoneMonolithPlacement::slot),
            Codec.intRange(0, 30_000).fieldOf("min_distance").forGetter(LoneMonolithPlacement::minDistance),
            Codec.intRange(0, 30_000).fieldOf("max_distance").forGetter(LoneMonolithPlacement::maxDistance),
            Codec.intRange(8, 256).fieldOf("core_radius").forGetter(LoneMonolithPlacement::coreRadius),
            RegistryCodecs.homogeneousList(Registries.BIOME).fieldOf("preferred_biomes").forGetter(LoneMonolithPlacement::preferredBiomes)
    ).apply(instance, LoneMonolithPlacement::new));

    /** Quantos pontos tentar antes de desistir (sem bioma certo por perto, nao nasce). */
    private static final int ATTEMPTS = 36;
    /** Ate onde procurar o bioma em volta de cada ponto, em blocos. */
    private static final int BIOME_SEARCH_RADIUS = 640;

    private final int slot;
    private final int minDistance;
    private final int maxDistance;
    private final int coreRadius;
    private final HolderSet<Biome> preferredBiomes;
    /** Um resultado por mundo carregado: a busca de bioma e cara, e a pergunta vem de cada chunk. */
    private final Map<ChunkGeneratorStructureState, Optional<ChunkPos>> targets =
            Collections.synchronizedMap(new WeakHashMap<>());

    public LoneMonolithPlacement(int salt, int slot, int minDistance, int maxDistance, int coreRadius,
                                 HolderSet<Biome> preferredBiomes) {
        super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, salt, Optional.empty());
        this.slot = slot;
        this.minDistance = Math.min(minDistance, maxDistance);
        this.maxDistance = Math.max(minDistance, maxDistance);
        this.coreRadius = coreRadius;
        this.preferredBiomes = preferredBiomes;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return target(state).map(chunk -> chunk.x == x && chunk.z == z).orElse(false);
    }

    /** O chunk onde esta estrutura nasce neste mundo, se nasce. */
    public Optional<ChunkPos> target(ChunkGeneratorStructureState state) {
        Optional<ChunkPos> known = targets.get(state);
        if (known != null) {
            return known;
        }
        synchronized (this) {
            known = targets.get(state);
            if (known == null) {
                known = locate(state);
                targets.put(state, known);
            }
            return known;
        }
    }

    /**
     * Tres rodadas, cada uma mais solta: primeiro na fatia dele; depois em qualquer direcao, na mesma
     * faixa de distancia (bioma raro, como badlands ou oceano congelado, pode nao existir na fatia); por
     * ultimo um pouco mais longe. Se nem assim aparecer o bioma, o mundo fica sem esse monolito.
     */
    private Optional<ChunkPos> locate(ChunkGeneratorStructureState state) {
        // A direcao de partida e do mundo (igual para os seis); a fatia e de cada um.
        double base = RandomSource.create(state.getLevelSeed()).nextDouble() * Math.PI * 2.0;
        double sector = Math.PI / 3.0;
        RandomSource random = RandomSource.create(state.getLevelSeed() ^ ((long) salt() * 0x9E3779B97F4A7C15L));
        BiomeSource biomes = ((ChunkGeneratorStructureStateAccessor) state).nexus$biomeSource();
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            int round = attempt * 3 / ATTEMPTS;
            double angle = round == 0
                    ? base + slot * sector + (random.nextDouble() - 0.5) * sector * 0.7
                    : random.nextDouble() * Math.PI * 2.0;
            double farthest = round == 2 ? maxDistance * 1.6 : maxDistance;
            double distance = minDistance + random.nextDouble() * (farthest - minDistance);
            int x = (int) (Math.cos(angle) * distance);
            int z = (int) (Math.sin(angle) * distance);
            // Varre o quadrado de 16 em 16 blocos e sorteia um dos pontos certos: barato e espalhado.
            Pair<BlockPos, Holder<Biome>> found = biomes.findBiomeHorizontal(x, 80, z, BIOME_SEARCH_RADIUS, 4,
                    preferredBiomes::contains, random, false, state.randomState().sampler());
            if (found != null) {
                return Optional.of(new ChunkPos(found.getFirst()));
            }
        }
        return Optional.empty();
    }

    public int slot() {
        return slot;
    }

    public int minDistance() {
        return minDistance;
    }

    public int maxDistance() {
        return maxDistance;
    }

    /** O tamanho aproximado da propria estrutura: dali para dentro, nada de estranho acontece. */
    public int coreRadius() {
        return coreRadius;
    }

    public HolderSet<Biome> preferredBiomes() {
        return preferredBiomes;
    }

    @Override
    public StructurePlacementType<?> type() {
        return MonolithRegistries.LONE_MONOLITH.get();
    }
}
