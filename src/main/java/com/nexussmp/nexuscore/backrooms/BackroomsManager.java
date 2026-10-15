package com.nexussmp.nexuscore.backrooms;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.backrooms.network.BackroomsTransitionPayload;
import com.nexussmp.nexuscore.backrooms.network.SkinwalkerPayload;
import com.nexussmp.nexuscore.compat.playerrevive.PlayerReviveCompat;
import com.nexussmp.nexuscore.compat.xaero.XaeroMaps;
import com.nexussmp.nexuscore.core.registry.NexusSounds;
import com.nexussmp.nexuscore.core.vault.PlayerVault;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Quem toca as backrooms no servidor: constroi os andares aos poucos, engole quem cai e devolve
 * quem acha uma porta.
 *
 * <p>Estado de execucao mora aqui (obras em andamento, quedas em curso); o que precisa sobreviver a
 * um restart mora no {@link BackroomsData}.
 */
public final class BackroomsManager {
    /** Quanto tempo dura a queda antes do teleporte, em ticks. */
    public static final int FALL_TICKS = 70;
    /** Quanto tempo dura a chegada do outro lado. */
    public static final int ARRIVE_TICKS = 40;

    /** Chave do guarda-volumes: separa este inventario de qualquer outro sistema. */
    public static final String VAULT_KEY = "backrooms";
    /**
     * Tempo entre ser pego e acordar no mundo: o corte para o preto, o chiado e o "voce foi pego"
     * precisam caber na tela. Combina com {@code SkinwalkerPayload.caught()}, que dura um pouco mais.
     */
    private static final int CATCH_TICKS = 75;

    private static final List<Job> JOBS = new ArrayList<>();
    private static final Map<UUID, Integer> FALLING = new HashMap<>();
    private static final Map<UUID, Integer> CAUGHT = new HashMap<>();

    /** Uma obra em andamento e quem pediu por ela, para avisar do progresso. */
    private record Job(BackroomsBuilder builder, @Nullable UUID requester, int[] lastReport) {}

    public static void clear() {
        JOBS.clear();
        FALLING.clear();
        CAUGHT.clear();
        SkinwalkerDirector.clear();
    }

    public static boolean busy() {
        return !JOBS.isEmpty();
    }

    // ---- geracao ----

    /**
     * Comeca um andar. A construcao continua sozinha nos ticks seguintes.
     *
     * <p>Com {@code replaced}, o andar novo nasce no lugar do antigo em vez de num canto livre — e o
     * que acontece quando o comando e dado de dentro das backrooms. A obra cobre os dois retangulos,
     * entao um andar menor nao deixa pedaco do anterior boiando em volta.
     */
    public static BackroomsArea generate(ServerLevel level, int size, int cellSize, int doors,
                                         boolean ceiling, long seed, @Nullable ServerPlayer requester,
                                         @Nullable BackroomsArea replaced) {
        BackroomsData data = BackroomsData.get(level.getServer());
        int originX = replaced == null ? data.nextOriginX() : replaced.originX();
        int originZ = replaced == null ? 0 : replaced.originZ();
        BackroomsArea area = new BackroomsArea(originX, originZ, size, cellSize, seed, ceiling,
                BackroomsSettings.lampSpacing(), BackroomsSettings.lampChance());
        BackroomsBuilder builder = BackroomsBuilder.create(level, area, doors, replaced);
        if (replaced != null) {
            data.removeArea(replaced);
            // A obra passa por cima das cabecas deste andar: essas saem da lista. As dos outros
            // andares continuam no chao, e na lista, para o comando de limpar ainda achar.
            data.forgetHeadsIn(replaced);
            data.forgetHeadsIn(area);
        }
        data.addArea(area);
        JOBS.add(new Job(builder, requester == null ? null : requester.getUUID(), new int[] {-1}));
        return area;
    }

    /** Refaz so a cobertura de um andar que ja existe. */
    public static void rebuildCeiling(ServerLevel level, BackroomsArea area, @Nullable ServerPlayer requester) {
        BackroomsData.get(level.getServer()).setDirty();
        JOBS.add(new Job(BackroomsBuilder.ceilingOnly(level, area),
                requester == null ? null : requester.getUUID(), new int[] {-1}));
    }

    public static void tick(MinecraftServer server) {
        tickJobs(server);
        tickFalling(server);
        tickCaught(server);
        SkinwalkerDirector.tick(server);
    }

    /** Foi pego: a tela ja esta preta, agora so falta acordar do outro lado. */
    public static void startCatch(ServerPlayer player) {
        CAUGHT.putIfAbsent(player.getUUID(), CATCH_TICKS);
    }

    private static void tickCaught(MinecraftServer server) {
        if (CAUGHT.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> iterator = CAUGHT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            int left = entry.getValue() - 1;
            if (left > 0) {
                entry.setValue(left);
                continue;
            }
            iterator.remove();
            player.level().playSound(null, player.blockPosition(), NexusSounds.SKINWALKER_RELEASE.get(),
                    SoundSource.HOSTILE, 1.0F, 1.0F);
            bringBack(player);
        }
    }

    private static void tickJobs(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        Iterator<Job> iterator = JOBS.iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            boolean done = job.builder().step(BackroomsSettings.chunksPerTick());
            report(server, job, done);
            if (done) {
                iterator.remove();
            }
        }
    }

    /** Avisa de dez em dez por cento: obra grande demora, e silencio parece travamento. */
    private static void report(MinecraftServer server, Job job, boolean done) {
        if (job.requester() == null) {
            return;
        }
        int percent = done ? 100 : job.builder().progressPercent() / 10 * 10;
        if (percent == job.lastReport()[0]) {
            return;
        }
        job.lastReport()[0] = percent;
        ServerPlayer player = server.getPlayerList().getPlayer(job.requester());
        if (player == null) {
            return;
        }
        player.displayClientMessage(done
                ? Component.translatable("nexuscore.backrooms.generate.done",
                        job.builder().area().size(), job.builder().area().doors().size())
                : Component.translatable("nexuscore.backrooms.generate.progress", percent), true);
    }

    // ---- entrar e sair ----

    public static boolean isInBackrooms(ServerPlayer player) {
        return player.level().dimension().equals(BackroomsDimensions.BACKROOMS);
    }

    /**
     * Se ha o que devolver a esta pessoa: ela esta la dentro, esta caindo, foi pega, ou deixou
     * ponto de volta ou coisas guardadas. Quem nunca foi nao tem de onde voltar — sem isto, o
     * comando de trazer de volta mandava para o spawn do mundo quem so estava perto do alvo.
     */
    public static boolean involved(ServerPlayer player) {
        UUID id = player.getUUID();
        return isInBackrooms(player) || FALLING.containsKey(id) || CAUGHT.containsKey(id)
                || BackroomsData.get(player.server).peekReturn(id) != null
                || PlayerVault.get(player.server).has(id, VAULT_KEY);
    }

    /**
     * Comeca a queda: a pessoa trava no lugar e afunda no chao. O teleporte em si so acontece no fim
     * da animacao, para a tela nao piscar de um mundo para o outro.
     */
    public static boolean startFall(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel backrooms = server.getLevel(BackroomsDimensions.BACKROOMS);
        BackroomsData data = BackroomsData.get(server);
        // Quem esta caido pelo PlayerRevive nao vai: la dentro ninguem levanta a pessoa, e caida
        // ela nao anda ate uma porta. Fica onde esta, para ser salva por quem estiver por perto.
        if (backrooms == null || data.newest() == null || isInBackrooms(player)
                || FALLING.containsKey(player.getUUID()) || PlayerReviveCompat.isDowned(player)) {
            return false;
        }
        data.setReturn(player.getUUID(), new BackroomsData.ReturnPoint(player.level().dimension(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));

        // Travar e cegar faz parte do efeito: a pessoa nao anda para fora da animacao, e a tela ja
        // escurece antes de o mundo trocar.
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FALL_TICKS + 20, 250, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FALL_TICKS + ARRIVE_TICKS, 0, false, false, false));
        player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRIGGER, SoundSource.AMBIENT, 0.7F, 0.4F);
        PacketDistributor.sendToPlayer(player, BackroomsTransitionPayload.fall(FALL_TICKS));
        FALLING.put(player.getUUID(), FALL_TICKS);
        return true;
    }

    private static void tickFalling(MinecraftServer server) {
        if (FALLING.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> iterator = FALLING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            int left = entry.getValue() - 1;
            if (left > 0) {
                entry.setValue(left);
                continue;
            }
            iterator.remove();
            land(player);
        }
    }

    private static void land(ServerPlayer player) {
        MinecraftServer server = player.server;
        ServerLevel backrooms = server.getLevel(BackroomsDimensions.BACKROOMS);
        BackroomsArea area = BackroomsData.get(server).newest();
        if (backrooms == null || area == null) {
            return;
        }
        BlockPos spawn = BackroomsBuilder.findSpawn(area, RandomSource.create());
        // Ninguem leva nada para dentro: inventario e acessorios ficam guardados ate a volta.
        PlayerVault.get(server).stash(player, VAULT_KEY);
        player.teleportTo(backrooms, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), 0.0F);
        // Mapa nenhum funciona la dentro — e o labirinto so assusta enquanto for labirinto.
        XaeroMaps.block(player);
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.fallDistance = 0.0F;
        // O zumbido do andar e um loop no cliente (BackroomsAudio), que comeca sozinho ao entrar.
        PacketDistributor.sendToPlayer(player, BackroomsTransitionPayload.arrive(ARRIVE_TICKS));
        NexusCore.LOGGER.info("[NexusCore] {} caiu nas backrooms em {}", player.getGameProfile().getName(), spawn);
    }

    /** Sai pela porta (ou pelo comando): volta exatamente de onde caiu. */
    public static boolean bringBack(ServerPlayer player) {
        MinecraftServer server = player.server;
        CAUGHT.remove(player.getUUID());
        // Trazido de volta no meio da queda: a queda nao termina depois, do outro lado.
        FALLING.remove(player.getUUID());
        SkinwalkerDirector.forget(player.getUUID());
        if (player.level() instanceof ServerLevel level) {
            SkinwalkerDirector.removeFor(level, player);
        }
        BackroomsData.ReturnPoint point = BackroomsData.get(server).takeReturn(player.getUUID());
        ServerLevel target = point == null ? server.overworld() : server.getLevel(point.dimension());
        if (target == null) {
            target = server.overworld();
        }
        if (point == null) {
            BlockPos spawn = target.getSharedSpawnPos();
            player.teleportTo(target, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(target, point.x(), point.y(), point.z(), point.yaw(), point.pitch());
        }
        player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        player.fallDistance = 0.0F;
        player.removeEffect(MobEffects.BLINDNESS);
        XaeroMaps.release(player);
        // Tudo de volta, exatamente como estava: e como se nunca tivesse saido daqui.
        PlayerVault.get(server).restore(player, VAULT_KEY);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.AMBIENT, 0.4F, 1.2F);
        PacketDistributor.sendToPlayer(player, BackroomsTransitionPayload.arrive(ARRIVE_TICKS));
        // Fim do susto: a tela da pessoa volta ao normal mesmo que ela tenha saido no meio da corrida.
        PacketDistributor.sendToPlayer(player, SkinwalkerPayload.clear());
        return true;
    }

    /** Toda porta de madeira das backrooms e saida: abrir e sair. */
    public static boolean isExitDoor(Level level, BlockPos pos) {
        return level.dimension().equals(BackroomsDimensions.BACKROOMS)
                && level.getBlockState(pos).is(Blocks.OAK_DOOR);
    }

    private BackroomsManager() {}
}
