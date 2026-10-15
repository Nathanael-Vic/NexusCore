package com.nexussmp.nexuscore.relic;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Reliquia nao cai na morte: fica com quem a carregava, e volta no renascer.
 *
 * <p><b>Do inventario.</b> Quando alguem morre, tudo o que ia ficar no chao passa por uma lista so
 * (os "drops") antes de qualquer mod pegar — e e dali que o Corpse monta o corpo. Escutando essa
 * lista na frente de todo mundo, as reliquias saem dela antes de o corpo existir: nao vao para o
 * corpo, nao podem ser saqueadas, e voltam para o inventario quando a pessoa renasce.
 *
 * <p><b>Das vagas do Curios.</b> La a reliquia simplesmente nao sai: ela diz ao Curios que fica
 * ({@code ALWAYS_KEEP}), e o Curios a leva junto para o corpo novo, na mesma vaga.
 *
 * <p>O que fica guardado entre a morte e o renascer vai para o arquivo do mundo: se o servidor cair
 * nesse meio-tempo, a reliquia volta quando a pessoa entrar de novo.
 */
public final class RelicKeeper extends SavedData {
    private static final String FILE_ID = NexusCore.MOD_ID + "_relic_keeper";
    private static final Factory<RelicKeeper> FACTORY = new Factory<>(RelicKeeper::new, RelicKeeper::load);

    /** Reliquias esperando o dono renascer. */
    private final Map<UUID, List<ItemStack>> waiting = new HashMap<>();

    private static RelicKeeper get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE_ID);
    }

    /** Morreu: as reliquias saem do que vai para o chao (e para o corpo) e ficam guardadas. */
    static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        List<ItemStack> kept = new ArrayList<>();
        Iterator<ItemEntity> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next().getItem();
            if (Relics.isRelic(stack)) {
                kept.add(stack.copy());
                drops.remove();
            }
        }
        if (!kept.isEmpty()) {
            RelicKeeper keeper = get(player.server);
            keeper.waiting.computeIfAbsent(player.getUUID(), ignored -> new ArrayList<>()).addAll(kept);
            keeper.setDirty();
        }
    }

    /** Renasceu (ou entrou depois de o servidor cair no meio): as reliquias voltam. */
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            giveBack(player);
        }
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.isAlive()) {
                giveBack(player);
            }
            Relics.syncCooldowns(player);
        }
    }

    private static void giveBack(ServerPlayer player) {
        RelicKeeper keeper = get(player.server);
        List<ItemStack> stacks = keeper.waiting.remove(player.getUUID());
        if (stacks == null) {
            return;
        }
        keeper.setDirty();
        for (ItemStack stack : stacks) {
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
        Relics.syncCooldowns(player);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag players = new ListTag();
        waiting.forEach((player, stacks) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("player", player);
            ListTag items = new ListTag();
            stacks.forEach(stack -> items.add(stack.save(registries)));
            entry.put("items", items);
            players.add(entry);
        });
        tag.put("waiting", players);
        return tag;
    }

    private static RelicKeeper load(CompoundTag tag, HolderLookup.Provider registries) {
        RelicKeeper keeper = new RelicKeeper();
        for (Tag raw : tag.getList("waiting", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            if (!entry.hasUUID("player")) {
                continue;
            }
            List<ItemStack> stacks = new ArrayList<>();
            for (Tag item : entry.getList("items", Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, item).ifPresent(stacks::add);
            }
            if (!stacks.isEmpty()) {
                keeper.waiting.put(entry.getUUID("player"), stacks);
            }
        }
        return keeper;
    }
}
