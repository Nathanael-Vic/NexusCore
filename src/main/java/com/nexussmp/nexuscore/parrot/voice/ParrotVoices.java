package com.nexussmp.nexuscore.parrot.voice;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * A porta da voz dos papagaios — a unica classe daqui que o resto do mod chama antes de saber se o
 * Simple Voice Chat esta instalado.
 *
 * <p>Nenhum tipo do mod de voz aparece nas assinaturas, e o {@link ParrotVoicechatPlugin} so e tocado
 * depois do teste de presenca. Sem o mod, o papagaio continua no ombro, piando como qualquer
 * papagaio — so nao repete ninguem.
 */
public final class ParrotVoices {
    private static final String VOICECHAT = "voicechat";
    private static Boolean present;
    /** O ultimo motivo de falha avisado: o log leva um aviso por motivo, nao um por fala. */
    private static String lastComplaint = "";

    public static boolean available() {
        if (present == null) {
            present = ModList.get().isLoaded(VOICECHAT);
        }
        return present;
    }

    /**
     * Toca uma frase saindo do papagaio, {@code pitch} vezes mais fina e {@code speed} vezes mais
     * rapida (as duas coisas separadas, ver {@link ParrotVoice}).
     *
     * @return quantos ticks a fala dura, ou 0 se nao saiu som
     */
    static int say(Entity parrot, List<byte[]> frames, double pitch, double speed) {
        if (!available()) {
            return 0;
        }
        int ticks = ParrotVoicechatPlugin.play(parrot, frames, pitch, speed);
        if (ticks <= 0) {
            complain(ticks == 0 ? "SEM_API" : "SEM_CANAL");
        }
        return Math.max(ticks, 0);
    }

    private static void complain(String reason) {
        if (!reason.equals(lastComplaint)) {
            lastComplaint = reason;
            NexusCore.LOGGER.warn("[NexusCore] o papagaio nao conseguiu falar: {}", reason);
        }
    }

    private ParrotVoices() {}
}
