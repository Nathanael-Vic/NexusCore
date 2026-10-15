package com.nexussmp.nexuscore.magic;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.magic.network.MagicPayloads;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Quem sabe magia: a lista de magos.
 *
 * <p>Magia aqui e coisa de poucos. O Iron's Spells continua inteiro no mundo — as estruturas, os
 * magos inimigos, os livros caidos nos baus —, mas so quem esta nesta lista le uma magia, lanca uma
 * magia ou mexe nas mesas de magia. Para o resto, os nomes das magias sao rabiscos que nao param
 * quietos e os objetos magicos nao respondem.
 *
 * <p>Quem poe alguem na lista e a moderacao ({@code /nexus mago}). Fica no arquivo do mundo: mago
 * continua mago depois do restart. O nome ao lado do id existe so para a listagem ter o que mostrar
 * de quem esta offline; quem manda e o id.
 *
 * <p>Esta classe nao encosta em nada do Iron's Spells, de proposito: o comando existe mesmo sem o
 * mod instalado, e so ele passa por aqui.
 */
public final class Mages extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_mages";
    private static final Factory<Mages> FACTORY = new Factory<>(Mages::new, Mages::load);
    /** Nivel de operador para nomear e tirar mago. */
    public static final int MODERATOR_LEVEL = 2;

    /** O que o servidor contou a este cliente sobre a propria pessoa. */
    private static volatile boolean clientSelf;

    /** mago -> ultimo nome conhecido. */
    private final Map<UUID, String> mages = new LinkedHashMap<>();

    public static Mages get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    /**
     * Se esta pessoa sabe magia. Vale dos dois lados: no servidor pela lista; no cliente pelo que o
     * servidor contou a ela.
     *
     * <p>No cliente so a propria pessoa importa — e para ela que os nomes aparecem e os botoes
     * respondem. Qualquer outro jogador conta como mago ali, para nada ficar escondido por engano.
     *
     * <p>Com o modulo desligado, todo mundo e mago: o Iron's Spells volta a ser o de sempre.
     */
    public static boolean isMage(Player player) {
        if (!CommonConfig.isModuleEnabled(ModuleIds.MAGIC)) {
            return true;
        }
        if (player.level().isClientSide()) {
            return !player.isLocalPlayer() || clientSelf;
        }
        MinecraftServer server = player.getServer();
        return server == null || get(server).contains(player.getUUID());
    }

    /** Conta ao cliente daquela pessoa se ela sabe magia. */
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new MagicPayloads.Mage(isMage(player)));
    }

    public static void setClientSelf(boolean mage) {
        clientSelf = mage;
    }

    public boolean contains(UUID player) {
        return mages.containsKey(player);
    }

    /** {@code false} se ja era mago. */
    public boolean add(UUID player, String name) {
        if (mages.put(player, name) != null) {
            return false;
        }
        setDirty();
        return true;
    }

    /** {@code false} se nao era mago. */
    public boolean remove(UUID player) {
        if (mages.remove(player) == null) {
            return false;
        }
        setDirty();
        return true;
    }

    public Collection<String> names() {
        return mages.values();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        mages.forEach((id, name) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            entry.putString("name", name);
            list.add(entry);
        });
        tag.put("mages", list);
        return tag;
    }

    private static Mages load(CompoundTag tag, HolderLookup.Provider registries) {
        Mages data = new Mages();
        for (Tag entry : tag.getList("mages", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            if (compound.hasUUID("id")) {
                data.mages.put(compound.getUUID("id"), compound.getString("name"));
            }
        }
        return data;
    }
}
