package com.nexussmp.nexuscore.monolith;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Onde estao os monolitos deste mundo. Nao e salvo em lugar nenhum: sai da semente, pelo mesmo
 * calculo que decide onde eles nascem ({@link LoneMonolithPlacement}), entao a geracao do mundo e o
 * resto do jogo sempre concordam.
 */
public final class MonolithSites {
    public static final String NAMESPACE = "ancient_remnants";

    /** Um monolito: qual, o meio do chunk onde nasce, e o raio da estrutura (o miolo calmo). */
    public record Site(ResourceLocation id, Holder<Structure> structure, BlockPos center, int coreRadius) {
        public double horizontalDistance(double x, double z) {
            double dx = x - center.getX();
            double dz = z - center.getZ();
            return Math.sqrt(dx * dx + dz * dz);
        }
    }

    private static final Map<ServerLevel, List<Site>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static List<Site> of(ServerLevel level) {
        List<Site> known = CACHE.get(level);
        if (known == null) {
            known = compute(level);
            CACHE.put(level, known);
        }
        return known;
    }

    private static List<Site> compute(ServerLevel level) {
        ChunkGeneratorStructureState state = level.getChunkSource().getGeneratorState();
        List<Site> sites = new ArrayList<>();
        for (Holder<StructureSet> set : state.possibleStructureSets()) {
            StructurePlacement placement = set.value().placement();
            if (!(placement instanceof LoneMonolithPlacement lone)) {
                continue;
            }
            ChunkPos chunk = lone.target(state).orElse(null);
            if (chunk == null) {
                continue;
            }
            for (StructureSet.StructureSelectionEntry entry : set.value().structures()) {
                ResourceLocation id = entry.structure().unwrapKey().map(key -> key.location()).orElse(null);
                if (id != null) {
                    sites.add(new Site(id, entry.structure(), chunk.getMiddleBlockPosition(64), lone.coreRadius()));
                }
            }
        }
        return List.copyOf(sites);
    }

    /** O monolito mais perto desse ponto, ou {@code null} se o mundo nao tem nenhum. */
    public static Site nearest(List<Site> sites, double x, double z) {
        Site best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Site site : sites) {
            double distance = site.horizontalDistance(x, z);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = site;
            }
        }
        return best;
    }

    /** Este ponto esta dentro da caixa de algum monolito ja gerado? */
    public static boolean insideStructure(ServerLevel level, BlockPos pos) {
        for (Site site : of(level)) {
            if (site.horizontalDistance(pos.getX(), pos.getZ()) > site.coreRadius() * 3.0) {
                continue;
            }
            if (level.structureManager().getStructureAt(pos, site.structure().value()).isValid()) {
                return true;
            }
        }
        return false;
    }

    /** Alguma estrutura do Ancient Remnants passa por este chunk? */
    public static boolean isMonolithStructure(ServerLevel level, Structure structure) {
        ResourceLocation key = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return key != null && NAMESPACE.equals(key.getNamespace());
    }

    private MonolithSites() {}
}
