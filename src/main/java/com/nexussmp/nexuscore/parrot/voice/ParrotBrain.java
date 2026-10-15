package com.nexussmp.nexuscore.parrot.voice;

import com.nexussmp.nexuscore.parrot.ShoulderParrot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quando o papagaio aprende e quando ele abre o bico.
 *
 * <p><b>Ouvir.</b> So entra na memoria dele a voz de <i>outra</i> pessoa falando perto — nunca a do
 * dono, nunca cochicho, nunca conversa de grupo (quem esta num grupo do chat de voz nao esta sendo
 * ouvido por quem esta em volta, entao o papagaio tambem nao ouve). Quem esta perto de quem e
 * calculado aqui, na thread do jogo, uma vez por segundo, e publicado como uma lista pronta: a
 * thread do chat de voz so consulta, nunca le posicao de entidade.
 *
 * <p><b>Falar.</b> Uma vez por segundo, cada papagaio rola um dado:
 * <ul>
 *   <li>com alguem por perto, 1 em {@value #ODDS_WITH_COMPANY} — da uma fala a cada dois minutos e
 *       meio, em media, puxando para o que acabou de ouvir;</li>
 *   <li>sozinho com o dono, 1 em {@value #ODDS_ALONE} — uns seis minutos, lembrando de qualquer
 *       coisa que ainda esteja na cabeca dele.</li>
 * </ul>
 * Alem disso, quando uma frase nova acaba de ficar na memoria, as vezes ele a repete na hora, um ou
 * dois segundos depois — o "eco" de papagaio, que e o momento mais engracado. Entre uma fala e
 * outra ele descansa pelo menos {@value #REST_MS} ms, aconteca o que acontecer: e o que garante que
 * nunca vira barulho constante.
 *
 * <p><b>A voz.</b> Mais fina que a da pessoa, quase na mesma velocidade, e cada papagaio com o seu
 * tom (sai do id do dono), com uma variacao pequena a cada fala.
 */
public final class ParrotBrain {
    /** Fala ate esta distancia do papagaio entra no ouvido dele. */
    private static final double HEARING_RANGE = 12.0;
    /** Alguem ate esta distancia conta como companhia. */
    private static final double COMPANY_RANGE = 24.0;
    private static final int ODDS_WITH_COMPANY = 150;
    private static final int ODDS_ALONE = 360;
    /** A chance de repetir na hora uma frase que acabou de aprender. */
    private static final float ECHO_CHANCE = 0.3F;
    private static final long ECHO_MIN_DELAY_MS = 1200;
    private static final long ECHO_MAX_DELAY_MS = 4000;
    /** Descanso minimo entre duas falas. */
    private static final long REST_MS = 45_000;
    /**
     * O tom dos papagaios: de 1.22 a 1.38 vezes a voz original. Da para subir mais do que quando a
     * fala era so acelerada, porque agora a velocidade nao vai junto ({@link ParrotVoice}).
     */
    private static final double PITCH_BASE = 1.22;
    private static final int PITCH_SPREAD = 17;
    private static final double PITCH_JITTER = 0.03;
    /** Quanto mais rapido ele fala que a pessoa: um tiquinho, papagaio fala ligeiro. */
    private static final double SPEED = 1.06;

    /** Quem fala -> os donos dos papagaios que ouvem essa pessoa agora. Lido pela thread do chat de voz. */
    private static volatile Map<UUID, List<UUID>> ears = Map.of();
    /** Memoria de cada papagaio, pelo dono. Sobrevive ao papagaio sair e voltar (morte, relog). */
    private static final Map<UUID, ParrotMemory> MEMORIES = new ConcurrentHashMap<>();
    /** So a thread do jogo mexe aqui. */
    private static final Map<UUID, Mood> MOODS = new HashMap<>();

    private static final class Mood {
        private long restUntil;
        private long echoAt = -1;
    }

    /** Um pacote de fala de alguem. Thread do chat de voz. */
    static void hear(UUID speaker, byte[] frame) {
        List<UUID> listeners = ears.get(speaker);
        if (listeners == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (UUID owner : listeners) {
            MEMORIES.computeIfAbsent(owner, ignored -> new ParrotMemory()).hear(speaker, frame, now);
        }
    }

    /** Uma vez por segundo, na thread do jogo: quem ouve quem, o que fica na memoria, quem fala. */
    public static void think(MinecraftServer server, Map<UUID, ShoulderParrot> parrots) {
        long now = System.currentTimeMillis();
        RandomSource random = server.overworld().getRandom();
        ears = listen(server, parrots);

        MEMORIES.forEach((owner, memory) -> {
            boolean learned = memory.settle(now, random);
            Mood mood = MOODS.computeIfAbsent(owner, ignored -> new Mood());
            if (learned && mood.echoAt < 0 && now >= mood.restUntil && random.nextFloat() < ECHO_CHANCE) {
                mood.echoAt = now + ECHO_MIN_DELAY_MS + random.nextInt((int) (ECHO_MAX_DELAY_MS - ECHO_MIN_DELAY_MS));
            }
        });
        // Memoria vazia de quem nem papagaio tem agora nao precisa ficar ocupando lugar.
        MEMORIES.entrySet().removeIf(entry -> {
            boolean gone = entry.getValue().isEmpty() && !parrots.containsKey(entry.getKey());
            if (gone) {
                MOODS.remove(entry.getKey());
            }
            return gone;
        });

        parrots.forEach((owner, parrot) -> {
            ParrotMemory memory = MEMORIES.get(owner);
            if (memory == null || parrot.isTalking() || parrot.isLeaving()
                    || parrot.arrival() != ShoulderParrot.ARRIVAL_NONE) {
                return;
            }
            Mood mood = MOODS.computeIfAbsent(owner, ignored -> new Mood());
            if (now < mood.restUntil) {
                mood.echoAt = -1;
                return;
            }
            ParrotMemory.Phrase phrase;
            if (mood.echoAt >= 0 && now >= mood.echoAt) {
                mood.echoAt = -1;
                phrase = memory.newest();
            } else {
                boolean company = hasCompany(parrot);
                if (random.nextInt(company ? ODDS_WITH_COMPANY : ODDS_ALONE) != 0) {
                    return;
                }
                phrase = memory.pick(random, company);
            }
            if (phrase == null) {
                return;
            }
            int ticks = ParrotVoices.say(parrot, phrase.frames(), pitchOf(owner, random), SPEED);
            if (ticks > 0) {
                parrot.startTalking(ticks);
                mood.restUntil = now + REST_MS + ticks * 50L;
            }
        });
    }

    /** Para cada pessoa no servidor, quais papagaios estao perto o bastante para ouvir. */
    private static Map<UUID, List<UUID>> listen(MinecraftServer server, Map<UUID, ShoulderParrot> parrots) {
        if (parrots.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<UUID>> result = new HashMap<>();
        double range = HEARING_RANGE * HEARING_RANGE;
        for (ServerPlayer speaker : server.getPlayerList().getPlayers()) {
            if (speaker.isSpectator()) {
                continue;
            }
            List<UUID> listeners = null;
            for (Map.Entry<UUID, ShoulderParrot> entry : parrots.entrySet()) {
                ShoulderParrot parrot = entry.getValue();
                if (entry.getKey().equals(speaker.getUUID()) || parrot.level() != speaker.level()
                        || parrot.distanceToSqr(speaker) > range) {
                    continue;
                }
                if (listeners == null) {
                    listeners = new ArrayList<>(2);
                }
                listeners.add(entry.getKey());
            }
            if (listeners != null) {
                result.put(speaker.getUUID(), List.copyOf(listeners));
            }
        }
        return Map.copyOf(result);
    }

    /** Tem mais alguem por perto, alem do dono? */
    private static boolean hasCompany(ShoulderParrot parrot) {
        double range = COMPANY_RANGE * COMPANY_RANGE;
        for (Player other : parrot.level().players()) {
            if (other != parrot.getVehicle() && !other.isSpectator() && parrot.distanceToSqr(other) <= range) {
                return true;
            }
        }
        return false;
    }

    private static double pitchOf(UUID owner, RandomSource random) {
        double own = PITCH_BASE + Math.floorMod(owner.hashCode(), PITCH_SPREAD) / 100.0;
        return own + (random.nextDouble() * 2.0 - 1.0) * PITCH_JITTER;
    }

    /** O papagaio foi tirado: a memoria vai junto. */
    public static void forget(UUID owner) {
        MEMORIES.remove(owner);
        MOODS.remove(owner);
    }

    public static void clear() {
        ears = Map.of();
        MEMORIES.clear();
        MOODS.clear();
    }

    private ParrotBrain() {}
}
