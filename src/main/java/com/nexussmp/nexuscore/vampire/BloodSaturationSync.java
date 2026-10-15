package com.nexussmp.nexuscore.vampire;

import com.nexussmp.nexuscore.vampire.network.VampirePayloads;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manda para cada vampiro a saturacao do sangue dele, quando ela muda.
 *
 * <p>E o que deixa o contorno amarelo das gotas ir diminuindo aos poucos, como o das carnes. Sem
 * isto, o cliente so ficaria sabendo da saturacao quando o nivel de sangue mudasse — ver
 * {@link VampirePayloads.BloodSaturation}.
 *
 * <p>Confere a cada poucos ticks, espalhado entre os jogadores para nao cair todo mundo no mesmo
 * tick, e so manda quando o numero mudou: a saturacao gasta devagar, entao na pratica sai um
 * pacote de vez em quando por vampiro.
 */
final class BloodSaturationSync {
    private static final int INTERVAL_TICKS = 5;

    /** O ultimo valor mandado para cada um. Sem entrada, a proxima conferida manda. */
    private static final Map<UUID, Float> SENT = new HashMap<>();

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || (player.tickCount + player.getId()) % INTERVAL_TICKS != 0
                || !VampirismBridge.isVampire(player)) {
            return;
        }
        float saturation = VampirismBridge.bloodSaturation(player);
        if (Float.isNaN(saturation)) {
            return;
        }
        Float sent = SENT.put(player.getUUID(), saturation);
        if (sent == null || sent != saturation) {
            PacketDistributor.sendToPlayer(player, new VampirePayloads.BloodSaturation(saturation));
        }
    }

    static void forget(UUID player) {
        SENT.remove(player);
    }

    static void clear() {
        SENT.clear();
    }

    private BloodSaturationSync() {}
}
