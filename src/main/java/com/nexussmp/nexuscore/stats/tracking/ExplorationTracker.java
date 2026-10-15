package com.nexussmp.nexuscore.stats.tracking;

import com.nexussmp.nexuscore.stats.StatsSettings;
import com.nexussmp.nexuscore.stats.StatsSync;
import com.nexussmp.nexuscore.stats.data.ExplorationData;
import com.nexussmp.nexuscore.stats.data.StructureInstanceId;
import com.nexussmp.nexuscore.stats.event.BiomeDiscoveredEvent;
import com.nexussmp.nexuscore.stats.event.StructureDiscoveredEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Descobre biomas e estruturas conforme os jogadores andam.
 *
 * <p>Nada e escaneado: so olha onde cada jogador esta. Custo por jogador, a cada {@code checkIntervalTicks}:
 * uma leitura de bioma (e so registra se mudou) e, ao trocar de chunk, a lista de estruturas que tocam aquele
 * chunk. A maioria dos chunks nao tem estrutura, entao o passo caro quase nunca roda.
 */
public final class ExplorationTracker {
    private static final Structure[] NO_STRUCTURES = new Structure[0];
    private static final Map<UUID, PlayerState> STATES = new HashMap<>();

    /** Cache em memoria por jogador, para nao repetir trabalho entre verificacoes. */
    private static final class PlayerState {
        ServerLevel level;
        Holder<Biome> lastBiome;
        long lastChunk = Long.MIN_VALUE;
        Structure[] nearbyStructures = NO_STRUCTURES;

        void resetFor(ServerLevel newLevel) {
            level = newLevel;
            lastBiome = null;
            lastChunk = Long.MIN_VALUE;
            nearbyStructures = NO_STRUCTURES;
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player instanceof FakePlayer) {
            return;
        }
        StatsSettings settings = StatsSettings.current();
        if (!settings.enabled() || (player.tickCount + player.getId()) % settings.checkIntervalTicks() != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!settings.tracks(level.dimension()) || !settings.counts(player)) {
            return;
        }
        BlockPos pos = player.blockPosition();
        if (!settings.insideArea(pos.getX(), pos.getZ())) {
            return;
        }

        PlayerState state = STATES.computeIfAbsent(player.getUUID(), uuid -> new PlayerState());
        if (state.level != level) {
            state.resetFor(level);
        }
        checkBiome(player, level, pos, state);
        checkStructures(player, level, pos, state);
    }

    private static void checkBiome(ServerPlayer player, ServerLevel level, BlockPos pos, PlayerState state) {
        Holder<Biome> biome = level.getBiome(pos);
        if (biome == state.lastBiome) {
            return;
        }
        state.lastBiome = biome;
        biome.unwrapKey().ifPresent(key -> {
            ExplorationData.BiomeResult result = ExplorationData.get(level.getServer())
                    .recordBiome(player, level.dimension().location(), key.location());
            if (result == null) {
                return;
            }
            if (result.firstInServer()) {
                StatsSync.markDirty();
            }
            NeoForge.EVENT_BUS.post(new BiomeDiscoveredEvent(player, level.dimension(), key.location(), result.firstInServer()));
        });
    }

    private static void checkStructures(ServerPlayer player, ServerLevel level, BlockPos pos, PlayerState state) {
        StructureManager structures = level.structureManager();
        long chunk = ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
        if (chunk != state.lastChunk) {
            state.lastChunk = chunk;
            Map<Structure, ?> references = structures.getAllStructuresAt(pos);
            state.nearbyStructures = references.isEmpty() ? NO_STRUCTURES : references.keySet().toArray(NO_STRUCTURES);
        }
        if (state.nearbyStructures.length == 0) {
            return;
        }

        Registry<Structure> registry = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Structure structure : state.nearbyStructures) {
            // Mesmo criterio das conquistas: precisa estar dentro de uma peca, nao so perto.
            StructureStart start = structures.getStructureWithPieceAt(pos, structure);
            if (!start.isValid()) {
                continue;
            }
            ResourceLocation type = registry.getKey(structure);
            if (type == null) {
                continue;
            }
            StructureInstanceId instance = new StructureInstanceId(type, start.getChunkPos().toLong());
            ExplorationData.StructureResult result = ExplorationData.get(level.getServer())
                    .recordStructure(player, level.dimension().location(), instance);
            if (result == null) {
                continue;
            }
            if (result.firstTypeInServer() || result.firstInstanceInServer()) {
                StatsSync.markDirty();
            }
            NeoForge.EVENT_BUS.post(new StructureDiscoveredEvent(player, level.dimension(), instance,
                    result.firstTypeInServer(), result.firstInstanceInServer(), result.firstInstanceForPlayer()));
        }
    }

    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }

    public static void clear() {
        STATES.clear();
    }

    private ExplorationTracker() {}
}
