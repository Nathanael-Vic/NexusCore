package com.nexussmp.nexuscore.backrooms.client;

import com.nexussmp.nexuscore.backrooms.network.BackroomsTransitionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A travessia vista de dentro: o chao engolindo a pessoa.
 *
 * <p>Nao ha teleporte nenhum aqui — o servidor faz isso no meio da animacao. O truque e so a camera:
 * ela afunda ate ficar dentro do bloco, o que ja deixa a tela escura por si, e a escuridao por cima
 * cobre o instante em que o mundo troca. Do outro lado a camera sobe de volta.
 */
public final class BackroomsTransition {
    /** Ate onde a camera afunda, em blocos. Passou do chao, ja esta dentro da pedra. */
    private static final double DEPTH = 2.6;

    @Nullable
    private static BackroomsTransitionPayload.Phase phase;
    private static int duration;
    private static int ticksLeft;

    public static void start(BackroomsTransitionPayload payload) {
        phase = payload.phase();
        duration = Math.max(1, payload.durationTicks());
        ticksLeft = duration;
    }

    public static void clear() {
        phase = null;
        ticksLeft = 0;
    }

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (phase != null && event.getEntity() == Minecraft.getInstance().player && --ticksLeft <= 0) {
            clear();
        }
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    /** 0 no comeco da fase, 1 no fim. */
    private static float progress(float partialTick) {
        if (phase == null) {
            return 0.0F;
        }
        return Mth.clamp((duration - ticksLeft + partialTick) / duration, 0.0F, 1.0F);
    }

    /** Quanto a camera esta abaixo do lugar dela, em blocos. */
    public static double cameraDrop(float partialTick) {
        if (phase == null) {
            return 0.0;
        }
        float progress = progress(partialTick);
        // Afundar acelera (a pessoa e puxada); voltar desacelera (ela emerge e se assenta).
        return phase == BackroomsTransitionPayload.Phase.FALL
                ? DEPTH * progress * progress
                : DEPTH * (1.0F - progress) * (1.0F - progress);
    }

    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (phase == null) {
            return;
        }
        float progress = progress(event.getPartialTick().getGameTimeDeltaPartialTick(false));
        float darkness = phase == BackroomsTransitionPayload.Phase.FALL ? progress : 1.0F - progress;
        int alpha = (int) (Mth.clamp(darkness, 0.0F, 1.0F) * 255.0F);
        if (alpha <= 2) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
    }

    private BackroomsTransition() {}
}
