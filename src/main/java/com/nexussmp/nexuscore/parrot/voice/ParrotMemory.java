package com.nexussmp.nexuscore.parrot.voice;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * O que um papagaio ouviu, e o que ficou na cabeca dele.
 *
 * <p>Sao dois tempos. <b>Ouvir</b> acontece na thread do chat de voz, pacote por pacote, e so junta
 * os pedacos de fala de cada pessoa ate ela fazer uma pausa. <b>Guardar</b> acontece na thread do
 * jogo, uma vez por segundo: cada frase fechada tem uma chance de ficar — papagaio nao decora tudo o
 * que escuta — e o que fica dura algumas horas, mesmo depois de quem falou ir embora.
 *
 * <p>Como nas backrooms, o que se guarda sao os pacotes Opus do jeito que chegaram (uns 60 bytes a
 * cada 20 ms): doze frases de tres segundos cabem em menos de 150 KB. Nada vai para o disco — o
 * servidor reiniciou, ele esquece.
 *
 * <p>Os trechos aqui sao mais curtos que os do skinwalker de proposito. Ele precisa de frases
 * inteiras para enganar; o papagaio fica mais engracado repetindo o pedaco: "...que isso?!".
 */
final class ParrotMemory {
    /** Quantas frases ele lembra. Entrou uma nova, sai a mais velha. */
    private static final int MAX_PHRASES = 12;
    /** Quanto tempo uma frase fica na cabeca dele. */
    private static final long LIFETIME_MS = 3L * 60 * 60 * 1000;
    /** Pausa que fecha uma frase: mais que uma respirada, menos que uma troca de assunto. */
    private static final long GAP_MS = 600;
    /** Abaixo de meio segundo e pigarro, nao frase. */
    private static final int MIN_FRAMES = 25;
    /** Teto de tres segundos: fala comprida vira pedacos, e o papagaio pega um deles. */
    private static final int MAX_FRAMES = 150;
    /** A chance de uma frase ouvida ficar na memoria. */
    private static final float LEARN_CHANCE = 0.45F;

    /** Uma frase: os pacotes, na ordem, e quando ele ouviu. */
    record Phrase(List<byte[]> frames, long heardAt) {
        /** Duracao em ticks: cada pacote sao 20 ms, um tick sao 50. */
        int ticks() {
            return (frames.size() * 20 + 49) / 50;
        }
    }

    /** A frase que alguem esta falando agora, ainda aberta. */
    private static final class Listening {
        private List<byte[]> frames = new ArrayList<>();
        private long lastPacket;
    }

    private final Map<UUID, Listening> listening = new HashMap<>();
    /** Frases ja fechadas, esperando a thread do jogo decidir se ficam. */
    private final List<List<byte[]>> heard = new ArrayList<>();
    private final Deque<Phrase> phrases = new ArrayDeque<>();
    @Nullable
    private Phrase lastSaid;

    /** Mais um pacote de fala de alguem por perto. Thread do chat de voz. */
    synchronized void hear(UUID speaker, byte[] frame, long now) {
        Listening current = listening.computeIfAbsent(speaker, ignored -> new Listening());
        if (now - current.lastPacket > GAP_MS) {
            close(current);
        }
        // Pacote vazio e o chat de voz avisando que a pessoa soltou o botao (ou parou de falar).
        if (frame.length == 0) {
            close(current);
            return;
        }
        current.frames.add(frame);
        current.lastPacket = now;
        if (current.frames.size() >= MAX_FRAMES) {
            close(current);
        }
    }

    /**
     * Fecha as frases paradas, decide quais ficam e esquece as velhas. Thread do jogo.
     *
     * @return se alguma frase nova ficou na memoria agora
     */
    synchronized boolean settle(long now, RandomSource random) {
        Iterator<Listening> open = listening.values().iterator();
        while (open.hasNext()) {
            Listening current = open.next();
            if (now - current.lastPacket > GAP_MS) {
                close(current);
                open.remove();
            }
        }
        boolean learned = false;
        for (List<byte[]> frames : heard) {
            if (random.nextFloat() < LEARN_CHANCE) {
                phrases.addLast(new Phrase(frames, now));
                learned = true;
            }
        }
        heard.clear();
        while (phrases.size() > MAX_PHRASES) {
            phrases.removeFirst();
        }
        phrases.removeIf(phrase -> now - phrase.heardAt() > LIFETIME_MS);
        return learned;
    }

    /** A ultima frase que ficou: e a que ele solta quando repete na hora. */
    @Nullable
    synchronized Phrase newest() {
        return remember(phrases.peekLast());
    }

    /**
     * Uma frase qualquer da memoria.
     *
     * <p>Com gente por perto, puxa para as mais recentes — repetir o que acabou de ser dito e a graca.
     * Sozinho, qualquer uma serve: e ele lembrando de alguem que ja foi embora. Nunca a mesma duas
     * vezes seguidas, se houver outra.
     */
    @Nullable
    synchronized Phrase pick(RandomSource random, boolean recentFirst) {
        if (phrases.isEmpty()) {
            return null;
        }
        List<Phrase> options = new ArrayList<>(phrases);
        if (options.size() > 1 && lastSaid != null) {
            options.remove(lastSaid);
        }
        int index;
        if (recentFirst) {
            // Quadrado de um sorteio uniforme: cai perto de zero bem mais vezes — perto do fim da fila.
            float roll = random.nextFloat();
            index = options.size() - 1 - (int) (roll * roll * options.size());
        } else {
            index = random.nextInt(options.size());
        }
        return remember(options.get(Math.max(0, index)));
    }

    synchronized boolean isEmpty() {
        return phrases.isEmpty() && heard.isEmpty() && listening.isEmpty();
    }

    @Nullable
    private Phrase remember(@Nullable Phrase phrase) {
        if (phrase != null) {
            lastSaid = phrase;
        }
        return phrase;
    }

    private void close(Listening current) {
        if (current.frames.size() >= MIN_FRAMES) {
            heard.add(List.copyOf(current.frames));
        }
        if (!current.frames.isEmpty()) {
            current.frames = new ArrayList<>();
        }
    }
}
