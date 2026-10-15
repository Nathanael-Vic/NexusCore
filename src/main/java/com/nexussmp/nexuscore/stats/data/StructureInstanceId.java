package com.nexussmp.nexuscore.stats.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/** Uma estrutura especifica no mapa: tipo + chunk onde ela comeca. */
public record StructureInstanceId(ResourceLocation type, long startChunk) {

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.toString());
        tag.putLong("chunk", startChunk);
        return tag;
    }

    @Nullable
    static StructureInstanceId load(CompoundTag tag) {
        ResourceLocation type = ResourceLocation.tryParse(tag.getString("type"));
        return type == null ? null : new StructureInstanceId(type, tag.getLong("chunk"));
    }
}
