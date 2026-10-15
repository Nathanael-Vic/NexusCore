package com.nexussmp.nexuscore.radio;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import com.nexussmp.nexuscore.radio.voice.RadioVoiceState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Quem toca a radio no servidor: abre a transmissao, conta para todo mundo e guarda quem esta
 * dentro dela.
 *
 * <p>Uma de cada vez, de proposito: e uma radio, nao um sistema de salas. Duas transmissoes ao
 * mesmo tempo obrigariam a escolher entre elas na interface, e a ideia e justamente o contrario —
 * quem esta no ar esta no ar para o servidor inteiro.
 *
 * <p>Nada aqui toca audio ainda. Esta camada e a sessao e o estado; a voz vem depois.
 */
public final class RadioManager {
    /** Nivel de operador: quem nomeia locutor, encerra o ar dos outros e abre o proprio. */
    public static final int MODERATOR_LEVEL = 2;

    @Nullable
    private static BroadcastSession current;

    public static void clear() {
        current = null;
        RadioVoiceState.clear();
    }

    /**
     * Quem fala ou quem ouve mudou: a foto que o chat de voz le e tirada de novo.
     *
     * <p>Toda mudanca de membro passa por aqui logo depois de acontecer — entrar, sair, fechar o
     * microfone, abrir e fechar o ar. Esquecer uma delas deixaria a voz indo para quem ja saiu, ou
     * nao indo para quem acabou de entrar.
     */
    private static void voiceChanged() {
        RadioVoiceState.update(live());
    }

    /** A transmissao no ar agora, ou {@code null} se o ar esta vazio. */
    @Nullable
    public static BroadcastSession live() {
        return current != null && current.isLive() ? current : null;
    }

    // ---- quem pode abrir o ar ----

    /** Locutor nomeado pela moderacao, ou a propria moderacao. */
    public static boolean mayBroadcast(ServerPlayer player) {
        return player.hasPermissions(MODERATOR_LEVEL)
                || RadioHosts.get(player.server).isHost(player.getUUID());
    }

    /**
     * Avisa a pessoa do que ela pode fazer agora.
     *
     * <p>Chamado ao virar (ou deixar de ser) locutor: o botao da pausa aparece ou some na hora, sem
     * ninguem ter que entrar e sair do servidor para descobrir.
     */
    public static void refresh(ServerPlayer player) {
        sendState(player, live(), false);
        // A arvore de comandos tambem muda com a permissao: sem isto, o /nexus radio iniciar so
        // apareceria para a pessoa no proximo login.
        player.server.getCommands().sendCommands(player);
    }

    // ---- comecar e terminar ----

    /**
     * Abre a transmissao. {@code null} se ja existe uma no ar, ou se o titulo nao sobrou nada.
     *
     * <p>O nome no ar vem limpo daqui: sem codigo de cor, sem caractere invisivel e sem tamanho
     * exagerado. Vazio significa o nick de quem apresenta.
     */
    @Nullable
    public static BroadcastSession start(ServerPlayer presenter, String title, String presenterName) {
        if (live() != null) {
            return null;
        }
        String cleanTitle = sanitize(title, RadioPayloads.MAX_TITLE);
        if (cleanTitle.isEmpty()) {
            return null;
        }
        String cleanName = sanitize(presenterName, RadioPayloads.MAX_NAME);
        if (cleanName.isEmpty()) {
            cleanName = presenter.getGameProfile().getName();
        }

        BroadcastSession session = new BroadcastSession(UUID.randomUUID(), presenter, cleanName, cleanTitle,
                System.currentTimeMillis());
        current = session;
        // O apresentador ja entra ouvindo e no ar: ele esta na propria transmissao.
        session.setMembership(presenter.getUUID(),
                new Membership(true, true, true, false, Membership.FULL_VOLUME));
        voiceChanged();
        NexusCore.LOGGER.info("[NexusCore] radio no ar: '{}' por {} ({})", cleanTitle, cleanName,
                presenter.getGameProfile().getName());
        announce(presenter.server, session);
        return session;
    }

    /**
     * Texto digitado por gente, pronto para ir a tela de todo mundo.
     *
     * <p>Tira o que o jogo leria como formatacao (o {@code §}) e tudo o que nao e texto visivel; o
     * resto e cortado no tamanho. Sem isso, um titulo poderia pintar a tela alheia de outra cor ou
     * empurrar o resto dela para fora.
     */
    public static String sanitize(String raw, int maxLength) {
        StringBuilder clean = new StringBuilder(Math.min(raw.length(), maxLength));
        for (int i = 0; i < raw.length() && clean.length() < maxLength; i++) {
            char letter = raw.charAt(i);
            if (letter != ChatFormatting.PREFIX_CODE && letter >= ' ' && letter != 127) {
                clean.append(letter);
            }
        }
        return clean.toString().strip();
    }

    /** Fecha a transmissao no ar. {@code false} se nao havia nenhuma. */
    public static boolean stop(MinecraftServer server) {
        BroadcastSession session = live();
        if (session == null) {
            return false;
        }
        session.end(System.currentTimeMillis());
        NexusCore.LOGGER.info("[NexusCore] radio fora do ar: '{}' ({} s)", session.title(),
                session.elapsedSeconds(System.currentTimeMillis()));
        current = null;
        voiceChanged();
        // Um pacote por pessoa: o estado carrega tambem se aquela pessoa pode abrir o proprio ar.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendState(player, null, false);
        }
        return true;
    }

    /**
     * Conta a novidade para o servidor inteiro: o aviso na tela e uma linha no chat com o botao.
     *
     * <p>O chat entra porque o aviso da tela some sozinho, como todo aviso do jogo. Quem estava
     * lendo outra coisa ainda encontra o convite ali, e clicar nele e um caminho tao vanilla quanto
     * clicar em qualquer link que um servidor manda.
     */
    private static void announce(MinecraftServer server, BroadcastSession session) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // Quem abriu o ar nao precisa ser avisado de que abriu o ar: o comando ja respondeu.
            boolean presenter = player.getUUID().equals(session.presenterId());
            sendState(player, session, !presenter);
            if (!presenter) {
                player.sendSystemMessage(invitation(session));
            }
        }
    }

    private static Component invitation(BroadcastSession session) {
        Component listen = Component.translatable("nexuscore.radio.chat.listen")
                .withStyle(style -> style
                        .withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nexus radio ouvir"))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("nexuscore.radio.chat.listen.hover"))));
        return Component.translatable("nexuscore.radio.chat.live",
                        Component.literal(session.title()).withStyle(ChatFormatting.WHITE),
                        Component.literal(session.presenterName()).withStyle(ChatFormatting.WHITE),
                        listen)
                .withStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
    }

    // ---- entrar e sair ----

    /** Entra como ouvinte. {@code false} se a sessao pedida nao e a que esta no ar. */
    public static boolean listen(ServerPlayer player, UUID sessionId) {
        BroadcastSession session = live();
        if (session == null || !session.id().equals(sessionId)) {
            sendState(player, session, false);
            return false;
        }
        session.setMembership(player.getUUID(), session.membership(player.getUUID()).withListening(true));
        voiceChanged();
        sendState(player, session, false);
        return true;
    }

    /**
     * Abre ou fecha o microfone de quem esta no ar.
     *
     * <p>So quem esta no ar tem microfone para fechar — para o resto, o pedido nao significa nada e
     * o estado volta como estava.
     */
    public static boolean setMicMuted(ServerPlayer player, boolean muted) {
        BroadcastSession session = live();
        Membership membership = session == null ? Membership.NONE : session.membership(player.getUUID());
        if (session == null || !membership.onAir()) {
            sendState(player, session, false);
            return false;
        }
        session.setMembership(player.getUUID(), membership.withMicMuted(muted));
        voiceChanged();
        // Todo mundo precisa saber: a mesa mostra quem esta mudo, e o volume da voz de cada ouvinte
        // depende de quem esta falando.
        broadcastState(player.server, session);
        return true;
    }

    /** Manda o aviso da transmissao no ar de novo, para quem for pedido. */
    public static boolean reannounce(ServerPlayer player) {
        BroadcastSession session = live();
        if (session == null) {
            return false;
        }
        sendState(player, session, true);
        player.sendSystemMessage(invitation(session));
        return true;
    }

    // ---- a mesa de som ----

    /**
     * So quem apresenta mexe na mesa.
     *
     * <p>A moderacao tambem, para poder baixar uma musica alta ou parar o que nao devia estar
     * tocando sem precisar encerrar o programa de alguem.
     */
    private static boolean mayMix(ServerPlayer player, BroadcastSession session) {
        return session.presenterId().equals(player.getUUID()) || player.hasPermissions(MODERATOR_LEVEL);
    }

    /**
     * Poe uma musica no ar.
     *
     * <p>O endereco vai para a tela de todo mundo que esta ouvindo, e cada cliente busca por conta
     * propria — entao ele e conferido aqui: tem de ser http ou https e caber no tamanho. Nada de
     * endereco de arquivo local atravessando o servidor ate a maquina dos outros.
     */
    public static boolean playMusic(ServerPlayer player, String url) {
        BroadcastSession session = live();
        if (session == null || !mayMix(player, session)) {
            sendState(player, session, false);
            return false;
        }
        String clean = musicUrl(url);
        if (clean.isEmpty()) {
            return false;
        }
        session.playMusic(clean, System.currentTimeMillis());
        NexusCore.LOGGER.info("[NexusCore] radio tocando: {}", clean);
        broadcastState(player.server, session);
        return true;
    }

    /** O endereco aceito, ou vazio se nao serve. */
    public static String musicUrl(String raw) {
        String clean = sanitize(raw, RadioPayloads.MAX_URL);
        if (!clean.startsWith("http://") && !clean.startsWith("https://")) {
            return "";
        }
        try {
            URI.create(clean);
        } catch (IllegalArgumentException malformed) {
            return "";
        }
        return clean;
    }

    public static boolean pauseMusic(ServerPlayer player, boolean paused) {
        BroadcastSession session = live();
        if (session == null || !mayMix(player, session) || !session.hasMusic()) {
            sendState(player, session, false);
            return false;
        }
        if (paused) {
            session.pauseMusic(System.currentTimeMillis());
        } else {
            session.resumeMusic(System.currentTimeMillis());
        }
        broadcastState(player.server, session);
        return true;
    }

    public static boolean stopMusic(ServerPlayer player) {
        BroadcastSession session = live();
        if (session == null || !mayMix(player, session)) {
            sendState(player, session, false);
            return false;
        }
        session.stopMusic();
        broadcastState(player.server, session);
        return true;
    }

    public static boolean setMusicVolume(ServerPlayer player, int volume) {
        BroadcastSession session = live();
        if (session == null || !mayMix(player, session)) {
            sendState(player, session, false);
            return false;
        }
        session.setMusicVolume(volume);
        broadcastState(player.server, session);
        return true;
    }

    /** O volume com que a voz de alguem entra na mistura, para todo mundo. */
    public static boolean setVoiceVolume(ServerPlayer player, UUID voice, int volume) {
        BroadcastSession session = live();
        if (session == null || !mayMix(player, session)) {
            sendState(player, session, false);
            return false;
        }
        Membership membership = session.membership(voice);
        if (!membership.onAir()) {
            return false;
        }
        session.setMembership(voice, membership.withVoiceVolume(volume));
        broadcastState(player.server, session);
        return true;
    }

    /** Mudanca que vale para todo mundo: o estado novo vai para o servidor inteiro. */
    private static void broadcastState(MinecraftServer server, BroadcastSession session) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendState(player, session, false);
        }
    }

    /** Sai da transmissao, continuando convidado se ja era. */
    public static boolean leave(ServerPlayer player) {
        BroadcastSession session = live();
        if (session == null) {
            sendState(player, null, false);
            return false;
        }
        session.setMembership(player.getUUID(), session.membership(player.getUUID()).withListening(false));
        voiceChanged();
        sendState(player, session, false);
        return true;
    }

    // ---- avisos ----

    /** Quem chega fica sabendo do ar — e o locutor recebe o seu botao mesmo com o ar vazio. */
    public static void onLoggedIn(ServerPlayer player) {
        BroadcastSession session = live();
        sendState(player, session, session != null);
        if (session != null) {
            player.sendSystemMessage(invitation(session));
        }
    }

    /**
     * Quem sai do servidor sai da transmissao. Se for o apresentador, a transmissao acaba: radio
     * sem locutor e so silencio no ar.
     */
    public static void onLoggedOut(ServerPlayer player) {
        BroadcastSession session = live();
        if (session == null) {
            return;
        }
        if (session.presenterId().equals(player.getUUID())) {
            stop(player.server);
            return;
        }
        session.forget(player.getUUID());
        voiceChanged();
    }

    public static void sendState(ServerPlayer player, @Nullable BroadcastSession session, boolean announce) {
        boolean mayBroadcast = mayBroadcast(player);
        if (session == null || !session.isLive()) {
            PacketDistributor.sendToPlayer(player, RadioPayloads.State.off(mayBroadcast));
            return;
        }
        long now = System.currentTimeMillis();
        RadioPayloads.Music music = session.hasMusic()
                ? new RadioPayloads.Music(session.musicUrl(), session.musicPaused(),
                        session.musicPositionMillis(now), session.musicVolume())
                : RadioPayloads.Music.NONE;
        List<RadioPayloads.Voice> voices = new ArrayList<>();
        session.onAir().forEach((id, membership) -> voices.add(new RadioPayloads.Voice(id,
                voiceName(player.server, id, session), membership.voiceVolume(), membership.micMuted())));

        RadioPayloads.Live live = new RadioPayloads.Live(session.id(), session.presenterId(),
                session.presenterName(), session.title(), session.elapsedSeconds(now),
                session.membership(player.getUUID()), music, List.copyOf(voices));
        PacketDistributor.sendToPlayer(player, RadioPayloads.State.of(live, announce, mayBroadcast));
    }

    /** O nome que a mesa de som mostra: o do ar para quem apresenta, o nick para os convidados. */
    private static String voiceName(MinecraftServer server, UUID id, BroadcastSession session) {
        if (id.equals(session.presenterId())) {
            return session.presenterName();
        }
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        return player != null ? player.getGameProfile().getName() : "?";
    }

    private RadioManager() {}
}
