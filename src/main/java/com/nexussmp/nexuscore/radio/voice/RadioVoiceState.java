package com.nexussmp.nexuscore.radio.voice;

import com.nexussmp.nexuscore.radio.BroadcastSession;
import com.nexussmp.nexuscore.radio.Membership;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Quem fala e quem ouve na radio, numa foto que o chat de voz pode ler.
 *
 * <p>O chat de voz trabalha na <b>thread dele</b>, nao na do jogo: cada pacote de microfone chega
 * por la, cinquenta por segundo por pessoa falando. A transmissao, por outro lado, muda na thread do
 * jogo — alguem entra, sai, fecha o microfone. Deixar a thread do chat ler a transmissao direto seria
 * ler um mapa no meio de uma escrita, que e o tipo de defeito que so aparece de vez em quando e nunca
 * na frente de quem esta procurando.
 *
 * <p>Entao a thread do jogo tira uma foto a cada mudanca — conjuntos imutaveis, trocados de uma vez
 * so — e a do chat so olha a foto. Ela nunca ve uma transmissao pela metade.
 *
 * <p>Esta classe nao conhece o Simple Voice Chat, de proposito: e a radio que a atualiza, e a radio
 * existe com ou sem o mod de voz instalado.
 */
public final class RadioVoiceState {
    /**
     * A foto.
     *
     * @param speaking  no ar e com o microfone aberto: a voz deles vai para a radio
     * @param listeners quem esta ouvindo a transmissao
     */
    public record Snapshot(Set<UUID> speaking, Set<UUID> listeners) {
        static final Snapshot EMPTY = new Snapshot(Set.of(), Set.of());
    }

    /**
     * O canal de cada voz e o id da pessoa embaralhado com uma constante.
     *
     * <p>Precisa ser <b>um canal por voz</b>: o chat de voz monta a fila de audio por canal, e duas
     * vozes num canal so virariam uma misturada picotada. E precisa ser um calculo, e nao uma tabela,
     * porque o cliente tem de chegar ao mesmo canal sozinho para saber de quem e cada pacote — e
     * desfazer o embaralhamento devolve a pessoa sem perguntar nada a ninguem.
     */
    private static final long SALT_HIGH = 0x6E65787573726164L;
    private static final long SALT_LOW = 0x696F766F7A5F7632L;

    private static volatile Snapshot current = Snapshot.EMPTY;

    public static Snapshot current() {
        return current;
    }

    /** O canal pelo qual a voz desta pessoa chega a radio. */
    public static UUID channelOf(UUID speaker) {
        return new UUID(speaker.getMostSignificantBits() ^ SALT_HIGH, speaker.getLeastSignificantBits() ^ SALT_LOW);
    }

    /** A pessoa por tras de um canal da radio — a mesma conta ao contrario. */
    public static UUID speakerOf(UUID channel) {
        return channelOf(channel);
    }

    /**
     * Tira a foto nova. Quem muda quem fala ou quem ouve chama isto logo depois.
     *
     * <p>Roda na thread do jogo, que e a unica que mexe na transmissao.
     */
    public static void update(@Nullable BroadcastSession session) {
        if (session == null || !session.isLive()) {
            current = Snapshot.EMPTY;
            return;
        }
        Set<UUID> speaking = new LinkedHashSet<>();
        for (Map.Entry<UUID, Membership> entry : session.onAir().entrySet()) {
            if (!entry.getValue().micMuted()) {
                speaking.add(entry.getKey());
            }
        }
        current = new Snapshot(Set.copyOf(speaking), Set.copyOf(session.listeners()));
    }

    public static void clear() {
        current = Snapshot.EMPTY;
    }

    private RadioVoiceState() {}
}
