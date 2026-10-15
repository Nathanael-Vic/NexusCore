package com.nexussmp.nexuscore.stats.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** O que um jogador ja descobriu, por dimensao. Base para leaderboard e recompensas futuras. */
public final class PlayerExploration {
    private String lastKnownName = "";
    private final Map<ResourceLocation, Dimension> dimensions = new HashMap<>();

    public String lastKnownName() {
        return lastKnownName;
    }

    void setLastKnownName(String name) {
        lastKnownName = name;
    }

    public Map<ResourceLocation, Dimension> dimensions() {
        return Collections.unmodifiableMap(dimensions);
    }

    Dimension dimension(ResourceLocation id) {
        return dimensions.computeIfAbsent(id, key -> new Dimension());
    }

    boolean removeDimension(ResourceLocation id) {
        return dimensions.remove(id) != null;
    }

    public static final class Dimension {
        final Set<ResourceLocation> biomes = new HashSet<>();
        final Set<ResourceLocation> structureTypes = new HashSet<>();
        final Set<StructureInstanceId> structures = new HashSet<>();

        public int biomeCount() {
            return biomes.size();
        }

        public int structureTypeCount() {
            return structureTypes.size();
        }

        public int structureCount() {
            return structures.size();
        }
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", lastKnownName);
        ListTag dimensionList = new ListTag();
        dimensions.forEach((id, dimension) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("id", id.toString());
            entry.put("biomes", saveIds(dimension.biomes));
            entry.put("structureTypes", saveIds(dimension.structureTypes));
            ListTag structures = new ListTag();
            dimension.structures.forEach(structure -> structures.add(structure.save()));
            entry.put("structures", structures);
            dimensionList.add(entry);
        });
        tag.put("dimensions", dimensionList);
        return tag;
    }

    static PlayerExploration load(CompoundTag tag) {
        PlayerExploration player = new PlayerExploration();
        player.lastKnownName = tag.getString("name");
        ListTag dimensionList = tag.getList("dimensions", Tag.TAG_COMPOUND);
        for (int i = 0; i < dimensionList.size(); i++) {
            CompoundTag entry = dimensionList.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("id"));
            if (id == null) {
                continue;
            }
            Dimension dimension = player.dimension(id);
            loadIds(entry.getList("biomes", Tag.TAG_STRING), dimension.biomes);
            loadIds(entry.getList("structureTypes", Tag.TAG_STRING), dimension.structureTypes);
            ListTag structures = entry.getList("structures", Tag.TAG_COMPOUND);
            for (int j = 0; j < structures.size(); j++) {
                StructureInstanceId structure = StructureInstanceId.load(structures.getCompound(j));
                if (structure != null) {
                    dimension.structures.add(structure);
                }
            }
        }
        return player;
    }

    private static ListTag saveIds(Set<ResourceLocation> ids) {
        ListTag list = new ListTag();
        ids.forEach(id -> list.add(StringTag.valueOf(id.toString())));
        return list;
    }

    private static void loadIds(ListTag list, Set<ResourceLocation> target) {
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                target.add(id);
            }
        }
    }
}
