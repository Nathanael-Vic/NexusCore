package com.nexussmp.nexuscore.stats.data;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tudo que foi descoberto, salvo junto com o mundo em {@code data/nexuscore_stats.dat}.
 * So o servidor mexe nisto, sempre na thread principal.
 */
public final class ExplorationData extends SavedData {
    public static final String FILE_ID = "nexuscore_stats";
    /** Sobe quando o formato do arquivo mudar de forma incompativel. */
    private static final int DATA_VERSION = 1;

    private static final Factory<ExplorationData> FACTORY = new Factory<>(ExplorationData::new, ExplorationData::load);

    private final Map<ResourceLocation, DimensionExploration> dimensions = new HashMap<>();
    private final Map<UUID, PlayerExploration> players = new HashMap<>();

    public static ExplorationData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public Map<ResourceLocation, DimensionExploration> dimensions() {
        return Collections.unmodifiableMap(dimensions);
    }

    @Nullable
    public DimensionExploration dimension(ResourceLocation id) {
        return dimensions.get(id);
    }

    @Nullable
    public PlayerExploration player(UUID uuid) {
        return players.get(uuid);
    }

    /** Resultado de uma descoberta de bioma; {@code null} quando o jogador ja conhecia o bioma. */
    public record BiomeResult(boolean firstInServer) {
        private static final BiomeResult FIRST = new BiomeResult(true);
        private static final BiomeResult PERSONAL = new BiomeResult(false);
    }

    /** Resultado de uma descoberta de estrutura; {@code null} quando nada mudou para o jogador. */
    public record StructureResult(boolean firstTypeInServer, boolean firstInstanceInServer, boolean firstInstanceForPlayer) {}

    @Nullable
    public BiomeResult recordBiome(ServerPlayer player, ResourceLocation dimensionId, ResourceLocation biome) {
        PlayerExploration.Dimension personal = playerData(player).dimension(dimensionId);
        if (!personal.biomes.add(biome)) {
            return null;
        }
        Discovery discovery = Discovery.of(player);
        DimensionExploration dimension = dimensions.computeIfAbsent(dimensionId, id -> new DimensionExploration());
        boolean first = dimension.addBiome(biome, discovery);
        if (first) {
            dimension.addRecent(new RecentDiscovery(RecentDiscovery.Kind.BIOME, biome,
                    discovery.playerName(), discovery.epochMillis()));
        }
        setDirty();
        return first ? BiomeResult.FIRST : BiomeResult.PERSONAL;
    }

    @Nullable
    public StructureResult recordStructure(ServerPlayer player, ResourceLocation dimensionId, StructureInstanceId structure) {
        PlayerExploration.Dimension personal = playerData(player).dimension(dimensionId);
        boolean newInstanceForPlayer = personal.structures.add(structure);
        boolean newTypeForPlayer = personal.structureTypes.add(structure.type());
        if (!newInstanceForPlayer && !newTypeForPlayer) {
            return null;
        }
        Discovery discovery = Discovery.of(player);
        DimensionExploration dimension = dimensions.computeIfAbsent(dimensionId, id -> new DimensionExploration());
        boolean firstType = dimension.addStructureType(structure.type(), discovery);
        boolean firstInstance = dimension.addStructure(structure, discovery);
        if (firstType || firstInstance) {
            RecentDiscovery.Kind kind = firstType ? RecentDiscovery.Kind.STRUCTURE_TYPE : RecentDiscovery.Kind.STRUCTURE;
            dimension.addRecent(new RecentDiscovery(kind, structure.type(), discovery.playerName(), discovery.epochMillis()));
        }
        setDirty();
        return new StructureResult(firstType, firstInstance, newInstanceForPlayer);
    }

    /** Apaga uma dimensao inteira (global e de todos os jogadores). */
    public boolean resetDimension(ResourceLocation dimensionId) {
        boolean changed = dimensions.remove(dimensionId) != null;
        for (PlayerExploration player : players.values()) {
            changed |= player.removeDimension(dimensionId);
        }
        if (changed) {
            setDirty();
        }
        return changed;
    }

    private PlayerExploration playerData(ServerPlayer player) {
        PlayerExploration data = players.computeIfAbsent(player.getUUID(), uuid -> new PlayerExploration());
        String name = player.getGameProfile().getName();
        if (!name.equals(data.lastKnownName())) {
            data.setLastKnownName(name);
            setDirty();
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("version", DATA_VERSION);

        ListTag dimensionList = new ListTag();
        dimensions.forEach((id, dimension) -> {
            CompoundTag entry = dimension.save();
            entry.putString("id", id.toString());
            dimensionList.add(entry);
        });
        tag.put("dimensions", dimensionList);

        ListTag playerList = new ListTag();
        players.forEach((uuid, player) -> {
            CompoundTag entry = player.save();
            entry.putUUID("uuid", uuid);
            playerList.add(entry);
        });
        tag.put("players", playerList);
        return tag;
    }

    private static ExplorationData load(CompoundTag tag, HolderLookup.Provider registries) {
        ExplorationData data = new ExplorationData();
        int version = tag.getInt("version");
        if (version > DATA_VERSION) {
            NexusCore.LOGGER.warn("[NexusCore] {}.dat e da versao {} (esta versao do mod le ate {}); lendo o que der.",
                    FILE_ID, version, DATA_VERSION);
        }

        ListTag dimensionList = tag.getList("dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensionList.size(); i++) {
            CompoundTag entry = dimensionList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id != null) {
                data.dimensions.put(id, DimensionExploration.load(entry));
            }
        }

        ListTag playerList = tag.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag entry = playerList.getCompound(i);
            if (entry.hasUUID("uuid")) {
                data.players.put(entry.getUUID("uuid"), PlayerExploration.load(entry));
            }
        }
        return data;
    }
}
