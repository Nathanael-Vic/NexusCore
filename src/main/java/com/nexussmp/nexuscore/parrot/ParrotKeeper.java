package com.nexussmp.nexuscore.parrot;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.parrot.voice.ParrotBrain;
import com.nexussmp.nexuscore.parrot.voice.ParrotVoices;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Quem garante que cada papagaio esta no ombro certo.
 *
 * <p>A conta e simples e roda a cada quarto de segundo: quem tem papagaio no arquivo, esta online,
 * vivo e fora do modo espectador, tem de ter um no ombro; quem nao, nao. O resto — entrar no
 * servidor, morrer, renascer, atravessar um portal, o restart — cai nessa mesma regra, sem um caso
 * especial para cada um.
 *
 * <p>Morreu, ele sai voando; renasceu, ele volta. Tudo no servidor, na thread do jogo.
 */
public final class ParrotKeeper {
    private static final int CHECK_TICKS = 5;
    private static final int THINK_TICKS = 20;

    /** O papagaio de cada pessoa agora — o que esta chegando ou parado, nunca o que esta indo embora. */
    private static final Map<UUID, ShoulderParrot> LIVE = new HashMap<>();

    static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (now % CHECK_TICKS != 0) {
            return;
        }
        boolean enabled = CommonConfig.isModuleEnabled(ModuleIds.PARROT);
        ParrotOwners owners = ParrotOwners.get(server);

        Iterator<Map.Entry<UUID, ShoulderParrot>> live = LIVE.entrySet().iterator();
        while (live.hasNext()) {
            Map.Entry<UUID, ShoulderParrot> entry = live.next();
            ShoulderParrot parrot = entry.getValue();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (parrot.isRemoved() || owner == null || parrot.getVehicle() != owner || parrot.level() != owner.level()) {
                parrot.discard();
                live.remove();
            } else if (!enabled || owners.get(owner.getUUID()) == null || !canCarry(owner)) {
                parrot.leave();
                live.remove();
            }
        }

        if (!enabled) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ParrotOwners.Perch perch = owners.get(player.getUUID());
            if (perch != null && !LIVE.containsKey(player.getUUID()) && canCarry(player)) {
                place(player, perch, ShoulderParrot.ARRIVAL_QUIET);
            }
        }
        if (now % THINK_TICKS == 0 && ParrotVoices.available()) {
            ParrotBrain.think(server, Collections.unmodifiableMap(LIVE));
        }
    }

    /** Da (ou troca) o papagaio de alguem. O antigo, se havia, encolhe enquanto o novo chega. */
    public static void give(ServerPlayer player, ParrotColor color, String name) {
        ParrotOwners.Perch perch = new ParrotOwners.Perch(color, name, player.getGameProfile().getName());
        ParrotOwners.get(player.server).set(player.getUUID(), perch);
        ShoulderParrot old = LIVE.remove(player.getUUID());
        if (old != null) {
            old.leave();
        }
        if (canCarry(player)) {
            place(player, perch, ShoulderParrot.ARRIVAL_GRAND);
        }
    }

    /** Tira o papagaio de alguem, e com ele tudo o que ele lembrava. {@code false} se nao tinha. */
    public static boolean take(MinecraftServer server, UUID owner) {
        boolean had = ParrotOwners.get(server).remove(owner);
        ShoulderParrot parrot = LIVE.remove(owner);
        if (parrot != null) {
            parrot.leave();
        }
        ParrotBrain.forget(owner);
        return had;
    }

    private static void place(ServerPlayer player, ParrotOwners.Perch perch, byte arrival) {
        ShoulderParrot parrot = ShoulderParrot.perchOn(player, perch, arrival);
        if (parrot != null) {
            LIVE.put(player.getUUID(), parrot);
        }
    }

    private static boolean canCarry(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isRemoved();
    }

    /**
     * Ninguem mexe no papagaio dos outros: nem coleira, nem biscoito, nem semente, nem etiqueta de
     * nome. Cancelado dos dois lados, para o cliente nem mostrar o braco interagindo.
     */
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof ShoulderParrot) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    static void onInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getTarget() instanceof ShoulderParrot) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }

    /**
     * Golpe no papagaio vai para o dono, como se o papagaio nem estivesse ali: numa briga, quem mira
     * no ombro de alguem quer acertar a pessoa. O papagaio mesmo nunca e atingido — nem copiado pelo
     * anel da metamorfose, que escuta este mesmo evento depois.
     */
    static void onAttack(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof ShoulderParrot parrot)) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer attacker && parrot.getVehicle() instanceof ServerPlayer owner
                && owner != attacker) {
            attacker.attack(owner);
        }
    }

    static void clear() {
        LIVE.clear();
    }

    private ParrotKeeper() {}
}
