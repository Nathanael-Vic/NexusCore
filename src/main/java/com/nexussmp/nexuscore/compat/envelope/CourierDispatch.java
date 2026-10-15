package com.nexussmp.nexuscore.compat.envelope;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.MailService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Chama um pombo carteiro para cada caixa de correio com carta esperando.
 *
 * <p>No Envelope, so busca carta um pombo que ja esteja a ate 20 blocos da caixa. Aqui, se a carta
 * espera alguns segundos e nenhum pombo por perto se mexe para buscar, um pombo carteiro (o de
 * servico do proprio Envelope, de chapeuzinho) surge longe, alto e fora da vista de todo mundo, e
 * vem voando buscar. Dali em diante e uma entrega do Envelope como qualquer outra; o pombo de
 * servico nao tem casa, entao nao volta depois de entregar.
 *
 * <p><b>O lorde.</b> Carta da caixa de um lorde vampiro vai de morcego ({@link BatCouriers}), e so
 * a noite: o morcego so e chamado de noite, e nenhum pombo comum pode levar a carta de um lorde —
 * nem o que estiver na porta. De dia, a carta do lorde espera a noite chegar.
 *
 * <p>O resto do horario e o do Envelope: pombo comum so sai de dia e sem chuva.
 *
 * <p>So caixas perto de alguem sao olhadas: e onde o pombo precisa estar para se mexer.
 */
final class CourierDispatch {
    private static final int SCAN_INTERVAL = 40;
    private static final int SCAN_RADIUS = 48;
    /** De onde o pombo chamado vem: tantos blocos em volta da caixa, e tantos acima. */
    private static final double ARRIVAL_RADIUS = 44.0;
    private static final double ARRIVAL_LIFT = 20.0;
    /** Perto disso, um pombo que ja esteja indo para a caixa conta como "alguem ja vem". */
    private static final double NATURAL_RADIUS = 24.0;
    /** Teto de pombos chamados ao mesmo tempo no servidor inteiro. */
    private static final int MAX_COURIERS = 24;

    /** Caixa com carta esperando: desde qual tick. */
    private static final Map<GlobalPos, Long> WAITING = new HashMap<>();
    /** Caixa com pombo chamado a caminho: qual pombo. */
    private static final Map<GlobalPos, UUID> ASSIGNED = new HashMap<>();

    static boolean enabled() {
        return CommonConfig.isModuleEnabled(ModuleIds.COMPAT_ENVELOPE);
    }

    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SCAN_INTERVAL != 0) {
            return;
        }
        if (!enabled() || !ServerConfig.get(ServerConfig.ENVELOPE_SUMMON_COURIERS)) {
            WAITING.clear();
            return;
        }
        ASSIGNED.entrySet().removeIf(entry -> !stillComing(server, entry.getKey(), entry.getValue()));
        Set<GlobalPos> near = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();
            if (!MailService.operatesIn(level)) {
                continue;
            }
            List<BlockPos> mailboxes = level.getPoiManager()
                    .getInRange(holder -> holder.is(Envelope.PoiTypes.MAILBOX), player.blockPosition(),
                            SCAN_RADIUS, PoiManager.Occupancy.ANY)
                    .map(PoiRecord::getPos)
                    .toList();
            for (BlockPos pos : mailboxes) {
                GlobalPos key = GlobalPos.of(level.dimension(), pos);
                if (near.add(key)) {
                    consider(level, key, server.getTickCount());
                }
            }
        }
        // Caixa que ficou longe de todo mundo esquece a espera: quando alguem voltar, conta de novo.
        WAITING.keySet().retainAll(near);
    }

    private static void consider(ServerLevel level, GlobalPos key, long now) {
        if (ASSIGNED.containsKey(key)) {
            return;
        }
        BlockPos pos = key.pos();
        if (!(level.getBlockEntity(pos) instanceof MailboxBlockEntity box) || !box.isAvailableForPickup()) {
            WAITING.remove(key);
            return;
        }
        long since = WAITING.computeIfAbsent(key, ignored -> now);
        if (now - since < ServerConfig.get(ServerConfig.ENVELOPE_COURIER_WAIT_SECONDS) * 20L) {
            return;
        }
        boolean lord = Couriers.isLord(level.getServer(), box.getOwner());
        if (!courierHours(level, lord) || ASSIGNED.size() >= MAX_COURIERS) {
            return;
        }
        // Um pombo do Envelope que ja esteja indo para la chega primeiro. Carta de lorde nao espera
        // por isso: pombo comum nao leva carta de lorde.
        if (!lord && naturalPigeonComing(level, pos)) {
            return;
        }
        Pigeon courier = summon(level, pos, box.getOwner());
        if (courier != null) {
            ASSIGNED.put(key, courier.getUUID());
            WAITING.remove(key);
        }
    }

    /** Morcego de noite (menos em tempestade); pombo de dia e sem chuva, como o Envelope ja faz. */
    private static boolean courierHours(ServerLevel level, boolean lord) {
        return lord
                ? level.isNight() && !level.isThundering()
                : !level.isNight() && !level.isRaining();
    }

    private static boolean naturalPigeonComing(ServerLevel level, BlockPos mailbox) {
        return !level.getEntitiesOfClass(Pigeon.class, new AABB(mailbox).inflate(NATURAL_RADIUS),
                pigeon -> !Couriers.isSummoned(pigeon) && mailbox.equals(pigeon.getMailboxHandler().getTargetPos()))
                .isEmpty();
    }

    @Nullable
    private static Pigeon summon(ServerLevel level, BlockPos mailbox, @Nullable UUID owner) {
        Vec3 from = CourierSky.hiddenPoint(level, Vec3.atCenterOf(mailbox), ARRIVAL_RADIUS, ARRIVAL_LIFT, level.getRandom());
        if (from == null) {
            return null;
        }
        Pigeon courier = Pigeon.createService(level);
        float yaw = (float) (Mth.atan2(mailbox.getZ() + 0.5 - from.z, mailbox.getX() + 0.5 - from.x) * Mth.RAD_TO_DEG) - 90.0F;
        courier.moveTo(from.x, from.y, from.z, yaw, 0.0F);
        courier.setPersistenceRequired();
        Couriers.summoned(courier, mailbox, owner);
        // Surge sem a fumacinha do Envelope: ninguem esta olhando para este ponto.
        return level.addFreshEntity(courier) ? courier : null;
    }

    private static boolean stillComing(MinecraftServer server, GlobalPos key, UUID courier) {
        ServerLevel level = server.getLevel(key.dimension());
        return level != null && level.getEntity(courier) instanceof Pigeon pigeon && pigeon.isAlive()
                && !pigeon.isDelivering() && !Couriers.isLeaving(pigeon);
    }

    /**
     * Manda um pombo embora voando, em vez de sumir onde esta. {@code false} com o modulo desligado:
     * ai o Envelope faz o que faria.
     */
    static boolean leave(Pigeon pigeon) {
        if (!enabled()) {
            return false;
        }
        Couriers.clearMailbox(pigeon);
        pigeon.addTag(Couriers.LEAVING);
        adopt(pigeon);
        return true;
    }

    /**
     * Carta de lorde so sai com o morcego chamado para ela. Chamado pelo mixin, antes de o Envelope
     * entregar a carta a um pombo.
     */
    static boolean mayPickUp(MailboxBlockEntity box, Pigeon pigeon) {
        if (!enabled() || Couriers.isSummoned(pigeon) || !(box.getLevel() instanceof ServerLevel level)) {
            return true;
        }
        return !Couriers.isLord(level.getServer(), box.getOwner());
    }

    /** Todo pombo que entra no mundo: se e um dos nossos, ganha de volta o objetivo e a cara certa. */
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Pigeon pigeon) {
            adopt(pigeon);
            BatCouriers.refresh(pigeon);
        }
    }

    /** Os objetivos nao sao salvos com a entidade: cada vez que um pombo carteiro volta ao mundo, ganha o seu. */
    private static void adopt(Pigeon pigeon) {
        if (!Couriers.isSummoned(pigeon) && !Couriers.isLeaving(pigeon)) {
            return;
        }
        boolean has = pigeon.goalSelector.getAvailableGoals().stream()
                .anyMatch(wrapped -> wrapped.getGoal() instanceof CourierGoal);
        if (!has) {
            pigeon.goalSelector.addGoal(0, new CourierGoal(pigeon));
        }
    }

    static void clear() {
        WAITING.clear();
        ASSIGNED.clear();
    }

    private CourierDispatch() {}
}
