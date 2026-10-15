package com.nexussmp.nexuscore.backrooms.voice;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * As falas guardadas de cada pessoa.
 *
 * <p>O truque daqui e nao guardar som, e sim <b>os pacotes do jeito que chegaram</b>: o chat de voz
 * transmite Opus comprimido, uns 60 bytes por 20 ms. Uma frase de quatro segundos ocupa cerca de
 * 12 KB assim, contra 380 KB se fosse guardada como onda. Oito frases por pessoa dao menos de
 * 100 KB — cabe sem pesar, e por isso da para ter variedade em vez de repetir a mesma frase.
 *
 * <p>O tamanho dos trechos e o que decide se o susto funciona. Pedaco curto demais sai picado e
 * ninguem entende o que foi dito; e o que corta uma frase no meio e a pausa que a gente faz para
 * respirar. Por isso a fala so fecha depois de um silencio <b>longo</b> (mais que uma respirada),
 * o teto e generoso o bastante para uma frase inteira caber, e qualquer coisa menor que um segundo
 * e jogada fora antes de virar trecho — pigarro nao assusta ninguem.
 *
 * <p>A conta cara (decodificar) so acontece na hora em que o skinwalker resolve falar, que e raro.
 *
 * <p>Nada disso vai para o disco: fala guardada e coisa de sessao. Servidor reiniciou, ele emudece
 * ate alguem falar de novo.
 */
final class VoiceVault {
    /** Quantos trechos por pessoa. Oito frases longas dao variedade sem virar arquivo de audio. */
    private static final int MAX_CLIPS = 8;
    /** Trecho mais velho que isso sai de cena — meia hora e tempo de sobra para assustar. */
    private static final long LIFETIME_MS = 30 * 60 * 1000;
    /**
     * Silencio que fecha uma fala.
     *
     * <p>Setecentos milissegundos e mais que a respirada no meio de uma frase e menos que a pausa
     * entre um assunto e outro: e o que separa "ei, vem ca" de dois pedacos soltos.
     */
    private static final long GAP_MS = 700;
    /** Teto de uma fala: 20 ms por pacote, entao 200 pacotes sao quatro segundos. */
    private static final int MAX_FRAMES = 200;
    /** Abaixo de um segundo nao da para entender o que foi dito: nao vira trecho. */
    private static final int MIN_FRAMES = 50;
    /** Quantos trechos entram no sorteio para ficar o mais longo. */
    private static final int CANDIDATES = 3;

    /** Uma fala: os pacotes crus, na ordem, e quando foi dita. */
    record Clip(List<byte[]> frames, long recordedAt) {}

    /** O que esta sendo falado agora por uma pessoa, e o que ela ja disse. */
    private static final class Speech {
        private final Deque<Clip> clips = new ArrayDeque<>();
        private List<byte[]> current = new ArrayList<>();
        private long lastPacket;
    }

    private final Map<UUID, Speech> speeches = new ConcurrentHashMap<>();

    /** Guarda mais um pacote de voz. */
    void accept(UUID speaker, byte[] frame, long now) {
        Speech speech = speeches.computeIfAbsent(speaker, ignored -> new Speech());
        synchronized (speech) {
            expire(speech, now);
            if (frame.length == 0 || now - speech.lastPacket > GAP_MS) {
                close(speech, now);
            }
            if (frame.length == 0) {
                return;
            }
            speech.lastPacket = now;
            speech.current.add(frame);
            if (speech.current.size() >= MAX_FRAMES) {
                close(speech, now);
            }
        }
    }

    /**
     * Uma fala daquela pessoa.
     *
     * <p>Sorteia tres e fica com a mais longa. E um meio-termo de proposito: sortear puro traz
     * pedaco curto demais com frequencia, e pegar sempre a maior faria o skinwalker repetir a mesma
     * frase a noite inteira. Assim quase sempre sai coisa que da para entender, e ainda muda.
     *
     * <p>Fecha antes o que estiver pendurado: quem falou uma frase e calou tem essa frase pronta
     * para ser usada, sem precisar falar de novo.
     */
    @Nullable
    Clip random(UUID speaker, RandomSource random, long now) {
        Speech speech = speeches.get(speaker);
        if (speech == null) {
            return null;
        }
        synchronized (speech) {
            expire(speech, now);
            close(speech, now);
            if (speech.clips.isEmpty()) {
                return null;
            }
            List<Clip> options = new ArrayList<>(speech.clips);
            Clip best = options.get(random.nextInt(options.size()));
            for (int i = 1; i < CANDIDATES && options.size() > 1; i++) {
                Clip other = options.get(random.nextInt(options.size()));
                if (other.frames().size() > best.frames().size()) {
                    best = other;
                }
            }
            return best;
        }
    }

    /** Quem tem alguma fala guardada. */
    List<UUID> speakers(long now) {
        List<UUID> speakers = new ArrayList<>();
        speeches.forEach((speaker, speech) -> {
            synchronized (speech) {
                expire(speech, now);
                close(speech, now);
                if (!speech.clips.isEmpty()) {
                    speakers.add(speaker);
                }
            }
        });
        return speakers;
    }

    void forget(UUID speaker) {
        speeches.remove(speaker);
    }

    void clear() {
        speeches.clear();
    }

    /** Fecha a fala em andamento, se ela ja tiver tamanho de frase. */
    private void close(Speech speech, long now) {
        if (speech.current.size() >= MIN_FRAMES) {
            speech.clips.addLast(new Clip(List.copyOf(speech.current), now));
            // Fila circular: entra fala nova, sai a mais velha. E o que mantem o repertorio vivo.
            while (speech.clips.size() > MAX_CLIPS) {
                speech.clips.removeFirst();
            }
        }
        if (!speech.current.isEmpty()) {
            speech.current = new ArrayList<>();
        }
    }

    private void expire(Speech speech, long now) {
        speech.clips.removeIf(clip -> now - clip.recordedAt() > LIFETIME_MS);
    }

    /** Descomprime uma fala para onda, que e o que o tocador do chat de voz quer. */
    static short[] decode(Clip clip, OpusDecoder decoder) {
        List<short[]> pieces = new ArrayList<>(clip.frames().size());
        int total = 0;
        for (byte[] frame : clip.frames()) {
            short[] decoded = decoder.decode(frame);
            if (decoded != null && decoded.length > 0) {
                pieces.add(decoded);
                total += decoded.length;
            }
        }
        short[] samples = new short[total];
        int offset = 0;
        for (short[] piece : pieces) {
            System.arraycopy(piece, 0, samples, offset, piece.length);
            offset += piece.length;
        }
        return samples;
    }
}
