package com.nexussmp.nexuscore.radio;

import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Uma transmissao: quem apresenta, sobre o que, desde quando, e quem esta dentro dela.
 *
 * <p>Vive so em memoria, de proposito. Transmissao e uma coisa que acontece <i>agora</i>; se o
 * servidor cair no meio, ela acabou — nao faz sentido o mundo voltar com uma radio no ar sem
 * ninguem falando nela.
 *
 * <p>Por enquanto a voz nao anda por aqui: esta camada e a sessao, o estado e quem participa. O
 * audio (Simple Voice Chat) entra depois e vai ler exatamente estas marcas para saber quem escuta
 * e quem fala.
 */
public final class BroadcastSession {
    public enum State {
        /** No ar agora. */
        LIVE,
        /** Terminou. A sessao ainda existe por um instante, so para avisar quem estava ouvindo. */
        ENDED
    }

    private final UUID id;
    private final UUID presenterId;
    /**
     * O nome que vai ao ar.
     *
     * <p>Pode ser o nick de quem apresenta ou um nome de programa escolhido na hora de abrir. A
     * <b>cara</b> continua vindo do {@link #presenterId}: quem ouve ve a skin de quem esta falando,
     * mesmo quando o nome no ar e outro.
     */
    private final String presenterName;
    private final String title;
    /** Relogio do servidor no comeco e no fim: e daqui que sai "no ar ha tantos minutos". */
    private final long startedAt;
    private long endedAt;
    private State state = State.LIVE;

    /**
     * Quem esta dentro, na ordem em que entrou — e a ordem que a mesa de som mostra. Quem nunca
     * entrou simplesmente nao esta no mapa.
     */
    private final Map<UUID, Membership> people = new LinkedHashMap<>();

    /**
     * A musica do programa.
     *
     * <p>O servidor nao carrega audio nenhum: ele guarda <b>o endereco e o relogio</b>, e cada
     * cliente que esta ouvindo toca por conta propria a partir dali. E o mesmo jeito que os teloes
     * do WaterFrames funcionam — e o unico que faz sentido, porque musica em canal de voz vira lata.
     *
     * <p>A posicao nasce de {@code musicStartedAt}: o instante em que aquele endereco estaria no
     * segundo zero. Pausar guarda a posicao; retomar recalcula o instante. Assim quem chega no meio
     * entra no meio, sem ninguem mandar "pule para tal segundo" a cada instante.
     */
    private String musicUrl = "";
    private boolean musicPaused;
    private long musicStartedAt;
    private long musicPausedPosition;
    private int musicVolume = Membership.FULL_VOLUME;

    BroadcastSession(UUID id, ServerPlayer presenter, String presenterName, String title, long startedAt) {
        this.id = id;
        this.presenterId = presenter.getUUID();
        this.presenterName = presenterName;
        this.title = title;
        this.startedAt = startedAt;
    }

    public UUID id() {
        return id;
    }

    public UUID presenterId() {
        return presenterId;
    }

    public String presenterName() {
        return presenterName;
    }

    public String title() {
        return title;
    }

    public State state() {
        return state;
    }

    public boolean isLive() {
        return state == State.LIVE;
    }

    /** Ha quantos segundos esta no ar (ou quanto durou, se ja acabou). */
    public int elapsedSeconds(long now) {
        long end = state == State.LIVE ? now : endedAt;
        return (int) Math.max(0L, (end - startedAt) / 1000L);
    }

    void end(long now) {
        state = State.ENDED;
        endedAt = now;
    }

    // ---- quem esta dentro ----

    public Membership membership(UUID player) {
        return people.getOrDefault(player, Membership.NONE);
    }

    /** Guarda a marca nova. Quem volta a ser nada some do mapa, em vez de virar entrada vazia. */
    void setMembership(UUID player, Membership membership) {
        if (membership.isNothing()) {
            people.remove(player);
        } else {
            people.put(player, membership);
        }
    }

    public void forget(UUID player) {
        people.remove(player);
    }

    // ---- a musica ----

    public String musicUrl() {
        return musicUrl;
    }

    public boolean musicPaused() {
        return musicPaused;
    }

    public int musicVolume() {
        return musicVolume;
    }

    public boolean hasMusic() {
        return !musicUrl.isEmpty();
    }

    /** Em que ponto da musica o programa esta agora. */
    public long musicPositionMillis(long now) {
        if (!hasMusic()) {
            return 0L;
        }
        return musicPaused ? musicPausedPosition : Math.max(0L, now - musicStartedAt);
    }

    void playMusic(String url, long now) {
        musicUrl = url;
        musicPaused = false;
        musicStartedAt = now;
        musicPausedPosition = 0L;
    }

    void pauseMusic(long now) {
        if (hasMusic() && !musicPaused) {
            musicPausedPosition = musicPositionMillis(now);
            musicPaused = true;
        }
    }

    void resumeMusic(long now) {
        if (hasMusic() && musicPaused) {
            // O inicio volta para tras na mesma medida do que ja tocou: a conta da posicao continua
            // valendo sozinha daqui em diante.
            musicStartedAt = now - musicPausedPosition;
            musicPaused = false;
        }
    }

    void stopMusic() {
        musicUrl = "";
        musicPaused = false;
        musicStartedAt = 0L;
        musicPausedPosition = 0L;
    }

    void setMusicVolume(int volume) {
        musicVolume = Membership.clampVolume(volume);
    }

    /** Quem esta no ar agora, na ordem em que entrou — a lista que a mesa de som mostra. */
    public Map<UUID, Membership> onAir() {
        Map<UUID, Membership> result = new LinkedHashMap<>();
        people.forEach((id, membership) -> {
            if (membership.onAir()) {
                result.put(id, membership);
            }
        });
        return result;
    }

    /** Quem esta ouvindo agora — inclusive quem esta no ar, que ouve a propria transmissao. */
    public Set<UUID> listeners() {
        Set<UUID> result = new LinkedHashSet<>();
        people.forEach((id, membership) -> {
            if (membership.listening()) {
                result.add(id);
            }
        });
        return result;
    }

    public int listenerCount() {
        return (int) people.values().stream().filter(Membership::listening).count();
    }
}
