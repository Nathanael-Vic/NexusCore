package com.nexussmp.nexuscore.backrooms.voice;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A porta de entrada da parte de voz — e a unica que o resto do mod enxerga.
 *
 * <p>Sem o Simple Voice Chat instalado, nada disso existe: por isso nenhum tipo do mod dele aparece
 * aqui na assinatura dos metodos. O corpo so e verificado pela JVM quando roda, e so roda depois do
 * teste de presenca. E assim que o skinwalker continua funcionando (mudo) num servidor sem voz.
 */
public final class BackroomsVoices {
    private static final String VOICECHAT = "voicechat";
    private static Boolean present;

    public static boolean available() {
        if (present == null) {
            present = ModList.get().isLoaded(VOICECHAT);
        }
        return present;
    }

    /** Tom da voz enquanto ele finge ser gente, e depois que para de fingir. */
    public static final double PITCH_HUMAN = BackroomsVoicechatPlugin.PITCH_HUMAN;
    public static final double PITCH_BEAST = BackroomsVoicechatPlugin.PITCH_BEAST;

    /** O ultimo motivo de falha ja avisado: o log leva um aviso por motivo, nao um por fala. */
    private static String lastComplaint = "";

    /** Solta um pedaco de fala de {@code speaker} saindo da entidade. */
    public static String speak(Entity source, @Nullable UUID speaker, double pitch) {
        String result = speakResult(source, speaker, pitch);
        // Falar e o tipo de coisa que falha calada: sem um aviso, "nao saiu som" vira adivinhacao.
        if (!"TOCANDO".equals(result) && !result.equals(lastComplaint)) {
            lastComplaint = result;
            NexusCore.LOGGER.warn("[NexusCore] o skinwalker nao conseguiu falar: {}", result);
        }
        return result;
    }

    private static String speakResult(Entity source, @Nullable UUID speaker, double pitch) {
        if (speaker == null) {
            return "SEM_DONO";
        }
        if (!available()) {
            return "SEM_MOD";
        }
        return BackroomsVoicechatPlugin.speak(source, speaker, pitch).name();
    }

    /** Quem tem voz gravada agora. Lista vazia sem o mod de voz. */
    public static List<UUID> voices() {
        return available() ? BackroomsVoicechatPlugin.voices() : List.of();
    }

    private BackroomsVoices() {}
}
