package com.nexussmp.nexuscore.hologram;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A gravacao em si: a cada tick, guarda o que o jogador esta fazendo.
 *
 * <p>Roda no servidor, que e quem sabe de verdade posicao, rotacao, pose e itens — os mesmos dados que
 * os outros jogadores recebem. Nao e animacao montada a mao: e exatamente o que aconteceu.
 */
public final class HologramRecorder {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static final class Session {
        final HologramRecording.Builder builder;
        boolean wasSwingingMain;
        boolean wasSwingingOff;

        Session(HologramRecording.Builder builder) {
            this.builder = builder;
        }
    }

    public static boolean isRecording(ServerPlayer player) {
        return SESSIONS.containsKey(player.getUUID());
    }

    public static void start(ServerPlayer player, String name) {
        SESSIONS.put(player.getUUID(), new Session(new HologramRecording.Builder(name, player)));
    }

    /** Termina e devolve a gravacao; {@code null} se nao havia nada gravado. */
    @Nullable
    public static HologramRecording stop(ServerPlayer player) {
        Session session = SESSIONS.remove(player.getUUID());
        if (session == null || session.builder.tickCount() == 0) {
            return null;
        }
        return session.builder.build();
    }

    public static boolean cancel(ServerPlayer player) {
        return SESSIONS.remove(player.getUUID()) != null;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        if (session.builder.tickCount() >= HologramSettings.current().maxRecordingTicks()) {
            HologramRecording recording = stop(player);
            if (recording != null && HologramStorage.save(player.server, recording)) {
                player.sendSystemMessage(Component.translatable("nexuscore.hologram.record.auto_stop",
                        recording.name(), String.format(Locale.ROOT, "%.1f", recording.seconds())));
            }
            return;
        }

        // O golpe de braco so vale no tick em que comeca, senao o holograma repetiria o gesto sem parar.
        boolean swingingMain = player.swinging && player.swingingArm == net.minecraft.world.InteractionHand.MAIN_HAND;
        boolean swingingOff = player.swinging && player.swingingArm == net.minecraft.world.InteractionHand.OFF_HAND;
        boolean startedMain = swingingMain && !session.wasSwingingMain;
        boolean startedOff = swingingOff && !session.wasSwingingOff;
        session.wasSwingingMain = swingingMain;
        session.wasSwingingOff = swingingOff;

        session.builder.capture(player, startedMain, startedOff);
    }

    /** Sair do servidor no meio da gravacao salva o que deu tempo, em vez de perder tudo. */
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HologramRecording recording = stop(player);
            if (recording != null) {
                HologramStorage.save(player.server, recording);
            }
        }
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private HologramRecorder() {}
}
