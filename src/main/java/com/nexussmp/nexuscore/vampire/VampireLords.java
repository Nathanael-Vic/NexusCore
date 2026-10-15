package com.nexussmp.nexuscore.vampire;

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
 * Quem pode ser vampiro de verdade: a lista de lordes.
 *
 * <p>Ser mordido faz qualquer um virar vampiro — mas vampiro comum aqui e um beco sem saida: ele
 * queima no sol, precisa de sangue e nada mais. Quem <b>progride</b> — sobe de nivel, ganha ponto de
 * habilidade, aprende poder — e so quem esta nesta lista, e quem poe alguem nela e a moderacao.
 *
 * <p>E permissao, nao estado: estar na lista nao torna ninguem vampiro (isso e
 * {@code /nexus vampiro tornar}), e sair dela nao cura ninguem — so devolve a pessoa ao nivel 1.
 *
 * <p>Fica no arquivo do mundo: lorde continua lorde depois do restart. O nome ao lado do id existe
 * so para a listagem ter o que mostrar de quem esta offline; quem manda e o id.
 */
public final class VampireLords extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_vampire_lords";
    private static final Factory<VampireLords> FACTORY = new Factory<>(VampireLords::new, VampireLords::load);

    /** lorde -> ultimo nome conhecido. */
    private final Map<UUID, String> lords = new LinkedHashMap<>();

    public static VampireLords get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public boolean isLord(UUID player) {
        return lords.containsKey(player);
    }

    /** {@code false} se ja era lorde. */
    public boolean add(ServerPlayer player) {
        if (lords.put(player.getUUID(), player.getGameProfile().getName()) != null) {
            return false;
        }
        setDirty();
        return true;
    }

    /** {@code false} se nao era lorde. */
    public boolean remove(UUID player) {
        if (lords.remove(player) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public Collection<String> names() {
        return lords.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        lords.forEach((id, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            entry.putString("name", name);
            list.add(entry);
        });
        tag.put("lords", list);
        return tag;
    }

    private static VampireLords load(CompoundTag tag, HolderLookup.Provider registries) {
        VampireLords data = new VampireLords();
        for (Tag entry : tag.getList("lords", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            if (compound.hasUUID("id")) {
                data.lords.put(compound.getUUID("id"), compound.getString("name"));
            }
        }
        return data;
    }
}
