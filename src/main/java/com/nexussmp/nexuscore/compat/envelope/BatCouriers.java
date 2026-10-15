package com.nexussmp.nexuscore.compat.envelope;

import io.github.mortuusars.envelope.Envelope;
import io.github.mortuusars.envelope.world.entity.Pigeon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * O pombo que leva carta de lorde aparece e soa como morcego.
 *
 * <p>So a cara e a voz mudam: por baixo continua sendo o pombo do Envelope, que voa, pega a carta,
 * viaja e entrega do jeito dele. O desenho de morcego e do cliente ({@code compat.envelope.client});
 * aqui fica quem decide e quem troca o som.
 *
 * <p><b>Quem decide</b> e o dono da carta — o dono da caixa de onde ela saiu — ser lorde agora. A
 * conta e refeita a cada segundo, entao quem deixa de ser lorde tem o proximo morcego de volta a
 * pombo sozinho, inclusive um que ja esteja no meio do caminho.
 *
 * <p><b>O som</b> e trocado na saida: todo som do pombo passa por um metodo so dele, e o mixin
 * troca ali. Arrulho vira o chiado do morcego, e o som de dano e o de morte tambem; passo, bater de
 * asa e bicar comida ficam em silencio, porque o morcego nao faz nenhum desses.
 */
final class BatCouriers {
    private static final int REFRESH_TICKS = 20;

    /** Todo tick de toda entidade passa aqui; so o pombo, e so a cada segundo, faz a conta. */
    static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof Pigeon pigeon && !pigeon.level().isClientSide()
                && pigeon.tickCount % REFRESH_TICKS == 0) {
            refresh(pigeon);
        }
    }

    /** Refaz a conta; se mudou, marca o pombo e conta para quem esta vendo. */
    static void refresh(Pigeon pigeon) {
        boolean bat = CourierDispatch.enabled()
                && Couriers.isLord(pigeon.level().getServer(), Couriers.ownerOf(pigeon));
        if (bat == looksLikeBat(pigeon)) {
            return;
        }
        if (bat) {
            pigeon.addTag(Couriers.BAT);
        } else {
            pigeon.removeTag(Couriers.BAT);
        }
        PacketDistributor.sendToPlayersTrackingEntity(pigeon, new EnvelopePayloads.BatCourier(pigeon.getId(), bat));
    }

    /** Quem passa a ver um morcego fica sabendo. Pombo comum nao precisa de aviso: e o padrao. */
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Pigeon pigeon && looksLikeBat(pigeon)
                && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, new EnvelopePayloads.BatCourier(pigeon.getId(), true));
        }
    }

    static boolean looksLikeBat(Pigeon pigeon) {
        return pigeon.getTags().contains(Couriers.BAT);
    }

    /** O som de morcego no lugar deste som de pombo; {@code null} para ficar em silencio. */
    @Nullable
    static SoundEvent batSound(SoundEvent sound) {
        if (sound == Envelope.SoundEvents.PIGEON_AMBIENT.get()) {
            return SoundEvents.BAT_AMBIENT;
        }
        if (sound == Envelope.SoundEvents.PIGEON_HURT.get()) {
            return SoundEvents.BAT_HURT;
        }
        if (sound == Envelope.SoundEvents.PIGEON_DEATH.get()) {
            return SoundEvents.BAT_DEATH;
        }
        if (sound == Envelope.SoundEvents.PIGEON_STEP.get() || sound == Envelope.SoundEvents.PIGEON_FLY.get()
                || sound == Envelope.SoundEvents.PIGEON_EAT.get()) {
            return null;
        }
        return sound;
    }

    private BatCouriers() {}
}
