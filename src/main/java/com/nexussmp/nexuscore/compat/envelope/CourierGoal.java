package com.nexussmp.nexuscore.compat.envelope;

import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.entity.ai.PigeonNavigation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * O que um pombo carteiro do NexusCore faz fora da entrega: vir ate a caixa, e depois ir embora.
 *
 * <p>Tem a prioridade mais alta do pombo e segura o movimento, entao nada do que ele faria a toa —
 * procurar comida, pousar, fugir de gato, se mudar para o pombal de alguem — atrapalha o caminho.
 * Durante a entrega em si, este objetivo fica parado e quem conduz e o proprio Envelope.
 *
 * <p><b>Vir.</b> Voa ate a frente da caixa e, chegando, pega a carta pelo mesmo caminho que um pombo
 * do Envelope pega. Se a carta sumir antes (alguem tirou, outro pombo levou) ou ele nao conseguir
 * chegar em um minuto e meio, desiste e vai embora.
 *
 * <p><b>Ir embora.</b> Voa para um ponto longe e alto que ninguem esteja olhando, e some assim que
 * ninguem estiver olhando para ele — pode ser antes de chegar la. Nunca some na frente de alguem.
 */
final class CourierGoal extends Goal {
    private static final int APPROACH_TIMEOUT = 20 * 90;
    private static final int LEAVE_TIMEOUT = 20 * 40;
    private static final double EXIT_RADIUS = 56.0;
    private static final double EXIT_LIFT = 24.0;
    /** Ninguem a menos disso quando ele some, mesmo de costas. */
    private static final double UNNOTICED_DISTANCE = 20.0;

    private final Pigeon pigeon;
    private int ticks;
    @Nullable
    private Vec3 exit;

    CourierGoal(Pigeon pigeon) {
        this.pigeon = pigeon;
        setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return !pigeon.isDelivering() && (Couriers.isLeaving(pigeon) || Couriers.mailboxOf(pigeon) != null);
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        ticks = 0;
        exit = null;
    }

    @Override
    public void tick() {
        if (!(pigeon.level() instanceof ServerLevel level)) {
            return;
        }
        ticks++;
        if (Couriers.isLeaving(pigeon)) {
            leave(level);
        } else {
            approach(level);
        }
    }

    private void approach(ServerLevel level) {
        BlockPos mailbox = Couriers.mailboxOf(pigeon);
        if (mailbox == null) {
            return;
        }
        if (ticks > APPROACH_TIMEOUT || !(level.getBlockEntity(mailbox) instanceof MailboxBlockEntity box)
                || !box.isAvailableForPickup()) {
            CourierDispatch.leave(pigeon);
            return;
        }
        BlockPos target = PigeonNavigation.getMailboxApproachTarget(level, mailbox);
        if (pigeon.hasReachedTarget(target)) {
            Couriers.clearMailbox(pigeon);
            if (!box.tryStartDelivery(pigeon)) {
                CourierDispatch.leave(pigeon);
            }
            return;
        }
        if (ticks % 10 == 1 || pigeon.getNavigation().isDone()) {
            pigeon.pathfindDirectlyTowards(target);
        }
    }

    private void leave(ServerLevel level) {
        if (ticks % 10 == 0 && CourierSky.unnoticed(level, pigeon.position(), UNNOTICED_DISTANCE)) {
            pigeon.discard();
            return;
        }
        if (exit == null) {
            Vec3 hidden = CourierSky.hiddenPoint(level, pigeon.position(), EXIT_RADIUS, EXIT_LIFT, pigeon.getRandom());
            exit = hidden != null ? hidden : pigeon.position().add(0.0, 40.0, 0.0);
        }
        // Chegou ao ponto escondido, ou nao chegou nunca: some de qualquer jeito. Um pombo carteiro
        // encalhado para sempre seria pior do que um que some com alguem olhando.
        if (ticks > LEAVE_TIMEOUT || pigeon.position().distanceToSqr(exit) < 9.0) {
            pigeon.discard();
            return;
        }
        if (ticks % 10 == 1 || pigeon.getNavigation().isDone()) {
            pigeon.pathfindDirectlyTowards(BlockPos.containing(exit));
        }
    }
}
