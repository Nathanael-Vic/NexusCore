package com.nexussmp.nexuscore.stats.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/** O que o servidor inteiro ja descobriu numa dimensao, com o pioneiro de cada coisa. */
public final class DimensionExploration {
    public static final int RECENT_LIMIT = 50;

    private final Map<ResourceLocation, Discovery> biomes = new HashMap<>();
    private final Map<ResourceLocation, Discovery> structureTypes = new HashMap<>();
    private final Map<StructureInstanceId, Discovery> structures = new HashMap<>();
    /** Mais nova primeiro. */
    private final Deque<RecentDiscovery> recent = new ArrayDeque<>();

    public Map<ResourceLocation, Discovery> biomes() {
        return Collections.unmodifiableMap(biomes);
    }

    public Map<ResourceLocation, Discovery> structureTypes() {
        return Collections.unmodifiableMap(structureTypes);
    }

    public Map<StructureInstanceId, Discovery> structures() {
        return Collections.unmodifiableMap(structures);
    }

    public Iterable<RecentDiscovery> recent() {
        return Collections.unmodifiableCollection(recent);
    }

    /** @return {@code true} se foi o primeiro do servidor */
    boolean addBiome(ResourceLocation biome, Discovery discovery) {
        return biomes.putIfAbsent(biome, discovery) == null;
    }

    boolean addStructureType(ResourceLocation type, Discovery discovery) {
        return structureTypes.putIfAbsent(type, discovery) == null;
    }

    boolean addStructure(StructureInstanceId id, Discovery discovery) {
        return structures.putIfAbsent(id, discovery) == null;
    }

    void addRecent(RecentDiscovery entry) {
        recent.addFirst(entry);
        while (recent.size() > RECENT_LIMIT) {
            recent.removeLast();
        }
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("biomes", saveDiscoveries(biomes));
        tag.put("structureTypes", saveDiscoveries(structureTypes));

        ListTag structureList = new ListTag();
        structures.forEach((id, discovery) -> {
            CompoundTag entry = id.save();
            entry.put("discovery", discovery.save());
            structureList.add(entry);
        });
        tag.put("structures", structureList);

        ListTag recentList = new ListTag();
        recent.forEach(entry -> recentList.add(entry.save()));
        tag.put("recent", recentList);
        return tag;
    }

    static DimensionExploration load(CompoundTag tag) {
        DimensionExploration dimension = new DimensionExploration();
        loadDiscoveries(tag.getList("biomes", Tag.TAG_COMPOUND), dimension.biomes);
        loadDiscoveries(tag.getList("structureTypes", Tag.TAG_COMPOUND), dimension.structureTypes);

        ListTag structureList = tag.getList("structures", Tag.TAG_COMPOUND);
        for (int i = 0; i < structureList.size(); i++) {
            CompoundTag entry = structureList.getCompound(i);
            StructureInstanceId id = StructureInstanceId.load(entry);
            if (id != null) {
                dimension.structures.put(id, Discovery.load(entry.getCompound("discovery")));
            }
        }

        ListTag recentList = tag.getList("recent", Tag.TAG_COMPOUND);
        for (int i = 0; i < recentList.size() && i < RECENT_LIMIT; i++) {
            RecentDiscovery entry = RecentDiscovery.load(recentList.getCompound(i));
            if (entry != null) {
                dimension.recent.addLast(entry);
            }
        }
        return dimension;
    }

    private static ListTag saveDiscoveries(Map<ResourceLocation, Discovery> discoveries) {
        ListTag list = new ListTag();
        discoveries.forEach((id, discovery) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            entry.put("discovery", discovery.save());
            list.add(entry);
        });
        return list;
    }

    /** IDs de mods removidos continuam no arquivo (historico); so IDs malformados sao descartados. */
    private static void loadDiscoveries(ListTag list, Map<ResourceLocation, Discovery> target) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id != null) {
                target.put(id, Discovery.load(entry.getCompound("discovery")));
            }
        }
    }
}
