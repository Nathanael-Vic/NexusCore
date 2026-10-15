package com.nexussmp.nexuscore.compat.envelope;

import io.github.mortuusars.envelope.world.block.mailbox.MailboxBlockEntity;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import io.github.mortuusars.envelope.world.mail.delivery.background.BackgroundCourier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A porta de entrada dos mixins do Envelope ({@code mixin/envelope}) para o resto do correio.
 *
 * <p>O resto do pacote fica fechado; so o que os mixins precisam chamar passa por aqui.
 */
public final class EnvelopeHooks {

    /** O som que o pombo toca no lugar deste: o mesmo, o de morcego, ou {@code null} para nenhum. */
    @Nullable
    public static SoundEvent courierSound(Pigeon pigeon, SoundEvent sound) {
        return BatCouriers.looksLikeBat(pigeon) ? BatCouriers.batSound(sound) : sound;
    }

    /**
     * O pombo carteiro terminou a entrega e o Envelope ia faze-lo virar fumaca ali mesmo. {@code true}
     * se ele vai embora voando, e o Envelope nao deve fazer nada.
     */
    public static boolean flyAway(Pigeon pigeon) {
        return CourierDispatch.leave(pigeon);
    }

    /** Se este pombo pode levar a carta desta caixa. */
    public static boolean mayPickUp(MailboxBlockEntity box, Pigeon pigeon) {
        return CourierDispatch.mayPickUp(box, pigeon);
    }

    /** Das caixas que um pombo achou por perto, tira as que ele nao pode levar. */
    public static List<BlockPos> reachableMailboxes(Pigeon pigeon, List<BlockPos> found) {
        if (found.isEmpty()) {
            return found;
        }
        return found.stream()
                .filter(pos -> !(pigeon.level().getBlockEntity(pos) instanceof MailboxBlockEntity box)
                        || CourierDispatch.mayPickUp(box, pigeon))
                .toList();
    }

    /** Quantos blocos o pombo sobe e se afasta antes de sumir no ar. */
    public static int ascendDistance(int distance) {
        return CourierSky.ascendDistance(distance);
    }

    /**
     * Carta de lorde viajando de dia: o morcego nao aparece. Segue a viagem invisivel, do jeito que o
     * Envelope ja faz com o pombo longe de todo mundo, e a carta chega do mesmo jeito.
     */
    public static boolean hideBackgroundCourier(BackgroundCourier courier) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !CourierDispatch.enabled()) {
            return false;
        }
        boolean lord = Couriers.isLord(server, courier.getDelivery().getOwner().orElse(null));
        return lord && !server.overworld().isNight();
    }

    private EnvelopeHooks() {}
}
