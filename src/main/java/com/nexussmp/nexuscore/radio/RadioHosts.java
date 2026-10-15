package com.nexussmp.nexuscore.radio;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quem pode abrir o ar: a lista de locutores.
 *
 * <p>Radio nao e coisa de todo mundo — a graca e justamente ter gente com programa. A moderacao
 * escolhe quem apresenta; o resto do servidor ouve.
 *
 * <p>Fica no arquivo do mundo, junto do resto: locutor continua locutor depois de um restart. O
 * nome e guardado ao lado do id so para o comando de listagem ter o que mostrar de quem esta
 * offline — quem manda e o id.
 */
public final class RadioHosts extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_radio_hosts";
    private static final Factory<RadioHosts> FACTORY = new Factory<>(RadioHosts::new, RadioHosts::load);

    /** locutor → ultimo nome conhecido. */
    private final Map<UUID, String> hosts = new LinkedHashMap<>();

    public static RadioHosts get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public boolean isHost(UUID player) {
        return hosts.containsKey(player);
    }

    /** {@code false} se ja era locutor. */
    public boolean add(ServerPlayer player) {
        if (hosts.put(player.getUUID(), player.getGameProfile().getName()) != null) {
            return false;
        }
        setDirty();
        return true;
    }

    /** {@code false} se nao era locutor. */
    public boolean remove(UUID player) {
        if (hosts.remove(player) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public Collection<String> names() {
        return hosts.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        hosts.forEach((id, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            entry.putString("name", name);
            list.add(entry);
        });
        tag.put("hosts", list);
        return tag;
    }

    private static RadioHosts load(CompoundTag tag, HolderLookup.Provider registries) {
        RadioHosts data = new RadioHosts();
        for (Tag entry : tag.getList("hosts", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            if (compound.hasUUID("id")) {
                data.hosts.put(compound.getUUID("id"), compound.getString("name"));
            }
        }
        return data;
    }
}
