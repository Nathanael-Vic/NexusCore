package com.nexussmp.nexuscore.parrot;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quem tem papagaio no ombro, de que cor e com que nome.
 *
 * <p>So isso vai para o arquivo do mundo. O papagaio em si nunca e salvo: ele nasce de novo no
 * ombro da pessoa sempre que ela entra, renasce, troca de mundo ou o servidor reinicia. Assim nao
 * ha papagaio perdido no chao de um chunk, nem dois no mesmo ombro.
 *
 * <p>O nome da pessoa ao lado do id e so para a listagem ter o que mostrar de quem esta offline.
 */
public final class ParrotOwners extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_parrots";
    private static final Factory<ParrotOwners> FACTORY = new Factory<>(ParrotOwners::new, ParrotOwners::load);

    /** O papagaio de alguem: a pena, o nome dele e o ultimo nome conhecido do dono. */
    public record Perch(ParrotColor color, String name, String ownerName) {}

    private final Map<UUID, Perch> perches = new LinkedHashMap<>();

    public static ParrotOwners get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    @Nullable
    public Perch get(UUID owner) {
        return perches.get(owner);
    }

    public void set(UUID owner, Perch perch) {
        perches.put(owner, perch);
        setDirty();
    }

    /** {@code false} se a pessoa nao tinha papagaio. */
    public boolean remove(UUID owner) {
        if (perches.remove(owner) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public Collection<Perch> all() {
        return perches.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        perches.forEach((owner, perch) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("owner", owner);
            entry.putString("ownerName", perch.ownerName());
            entry.putString("color", perch.color().id());
            entry.putString("name", perch.name());
            list.add(entry);
        });
        tag.put("parrots", list);
        return tag;
    }

    private static ParrotOwners load(CompoundTag tag, HolderLookup.Provider registries) {
        ParrotOwners data = new ParrotOwners();
        for (Tag entry : tag.getList("parrots", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            ParrotColor color = ParrotColor.byId(compound.getString("color"));
            if (compound.hasUUID("owner") && color != null) {
                data.perches.put(compound.getUUID("owner"),
                        new Perch(color, compound.getString("name"), compound.getString("ownerName")));
            }
        }
        return data;
    }
}
