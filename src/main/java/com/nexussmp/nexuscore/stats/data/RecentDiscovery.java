package com.nexussmp.nexuscore.stats.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Entrada do feed "descobertas recentes": so primeiras vezes no servidor. */
public record RecentDiscovery(Kind kind, ResourceLocation id, String playerName, long epochMillis) {

    public enum Kind {
        /** Pioneiro de um bioma. */
        BIOME,
        /** Pioneiro de um tipo de estrutura (a primeira instancia dele). */
        STRUCTURE_TYPE,
        /** Mais uma instancia de um tipo ja conhecido. */
        STRUCTURE;

        private static final Kind[] VALUES = values();

        public static Kind byOrdinal(int ordinal) {
            return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : STRUCTURE;
        }
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putByte("kind", (byte) kind.ordinal());
        tag.putString("id", id.toString());
        tag.putString("name", playerName);
        tag.putLong("time", epochMillis);
        return tag;
    }

    @Nullable
    static RecentDiscovery load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("id"));
        if (id == null) {
            return null;
        }
        return new RecentDiscovery(Kind.byOrdinal(tag.getByte("kind")), id, tag.getString("name"), tag.getLong("time"));
    }
}
