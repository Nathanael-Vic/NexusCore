package com.nexussmp.nexuscore.core.vault;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda-volumes: o inventario de alguem fica aqui inteiro e volta exatamente como estava.
 *
 * <p>Nao e so das backrooms. A ideia e a mesma em qualquer lugar onde a pessoa "sai daqui sem nada e
 * volta como estava": cada guardada tem uma <b>chave</b> ({@code backrooms}, {@code space}...), entao
 * dois sistemas diferentes nunca disputam o mesmo inventario.
 *
 * <p>Alem das 36 vagas de sempre, cada {@link VaultCompartment} registrado guarda as suas junto — e
 * assim os acessorios do Curios entram e voltam com o resto, sem este arquivo saber o que e um
 * acessorio.
 *
 * <p>O conteudo vive no arquivo do mundo, nao em memoria: cair o servidor com alguem la dentro nao
 * come as coisas dela.
 */
public final class PlayerVault extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_vault";
    private static final Factory<PlayerVault> FACTORY = new Factory<>(PlayerVault::new, PlayerVault::load);

    /**
     * Os compartimentos extras, na ordem em que foram registrados.
     *
     * <p>O registro acontece uma vez, no setup do mod, antes de qualquer mundo abrir; dali em diante
     * a lista e so lida, sempre na thread do servidor.
     */
    private static final List<VaultCompartment> COMPARTMENTS = new ArrayList<>();

    /** jogador → (chave → o que foi guardado). */
    private final Map<UUID, Map<String, Stash>> vaults = new HashMap<>();

    /** O inventario principal e o que veio dos compartimentos extras, guardados juntos. */
    private record Stash(ListTag items, CompoundTag extras) {}

    /** Registra um compartimento extra. Repetir o mesmo id nao faz nada. */
    public static void register(VaultCompartment compartment) {
        for (VaultCompartment existing : COMPARTMENTS) {
            if (existing.id().equals(compartment.id())) {
                return;
            }
        }
        COMPARTMENTS.add(compartment);
    }

    public static PlayerVault get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    public boolean has(UUID player, String key) {
        Map<String, Stash> byKey = vaults.get(player);
        return byKey != null && byKey.containsKey(key);
    }

    /**
     * Guarda tudo o que a pessoa esta carregando e a deixa de maos vazias.
     *
     * <p>Nao sobrescreve o que ja estiver guardado naquela chave: se por algum caminho torto ela
     * entrar duas vezes, o primeiro inventario — o de verdade — continua intacto.
     */
    public boolean stash(ServerPlayer player, String key) {
        if (has(player.getUUID(), key)) {
            return false;
        }
        ListTag items = player.getInventory().save(new ListTag());
        CompoundTag extras = new CompoundTag();
        for (VaultCompartment compartment : COMPARTMENTS) {
            Tag saved = compartment.stash(player);
            if (saved != null) {
                extras.put(compartment.id(), saved);
            }
        }
        vaults.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>())
                .put(key, new Stash(items, extras));
        player.getInventory().clearContent();
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        setDirty();
        return true;
    }

    /** Devolve o que estava guardado. {@code false} se nao havia nada naquela chave. */
    public boolean restore(ServerPlayer player, String key) {
        Map<String, Stash> byKey = vaults.get(player.getUUID());
        Stash stash = byKey == null ? null : byKey.remove(key);
        if (stash == null) {
            return false;
        }
        if (byKey.isEmpty()) {
            vaults.remove(player.getUUID());
        }
        // O que a pessoa juntou do outro lado nao some: o inventario guardado volta para o lugar de
        // cada coisa, e o que ela trazia na mao e posto de volta por cima — o que nao couber cai no
        // chao, como acontece com qualquer item que sobra. No caminho normal ela volta de maos
        // vazias e isto nao faz nada; e o que segura quem saiu por fora (um /tp, um /home de outro
        // mod) e caiu de novo antes de devolverem as coisas dela.
        List<ItemStack> carried = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) {
                carried.add(stack.copy());
            }
        }
        player.getInventory().clearContent();
        player.getInventory().load(stash.items());
        for (ItemStack stack : carried) {
            player.getInventory().placeItemBackInInventory(stack);
        }
        restoreExtras(player, stash.extras());
        player.containerMenu.broadcastChanges();
        player.inventoryMenu.broadcastChanges();
        setDirty();
        return true;
    }

    /**
     * Devolve as vagas extras.
     *
     * <p>Se sobrar algo que ninguem reclamou — o mod que criava aquelas vagas foi removido entre a
     * ida e a volta —, isso vai para o log em vez de sumir em silencio: a pessoa precisa saber que o
     * colar dela ficou para tras porque o mod saiu, e nao por causa de um bug.
     */
    private static void restoreExtras(ServerPlayer player, CompoundTag extras) {
        for (VaultCompartment compartment : COMPARTMENTS) {
            Tag saved = extras.get(compartment.id());
            if (saved != null) {
                compartment.restore(player, saved);
                extras.remove(compartment.id());
            }
        }
        for (String orphan : extras.getAllKeys()) {
            NexusCore.LOGGER.warn("[NexusCore] guarda-volumes de {}: nada sabe devolver '{}' (o mod saiu?)",
                    player.getGameProfile().getName(), orphan);
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        vaults.forEach((player, byKey) -> byKey.forEach((key, stash) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            entry.putString("key", key);
            entry.put("items", stash.items());
            if (!stash.extras().isEmpty()) {
                entry.put("extras", stash.extras());
            }
            list.add(entry);
        }));
        tag.put("vaults", list);
        return tag;
    }

    private static PlayerVault load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerVault data = new PlayerVault();
        for (Tag entry : tag.getList("vaults", Tag.TAG_COMPOUND)) {
            CompoundTag compound = (CompoundTag) entry;
            // Guardadas antigas nao tinham "extras"; um composto vazio le igual.
            Stash stash = new Stash(compound.getList("items", Tag.TAG_COMPOUND), compound.getCompound("extras"));
            data.vaults.computeIfAbsent(compound.getUUID("player"), ignored -> new HashMap<>())
                    .put(compound.getString("key"), stash);
        }
        return data;
    }
}
