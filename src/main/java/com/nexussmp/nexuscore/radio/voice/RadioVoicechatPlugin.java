package com.nexussmp.nexuscore.radio.voice;

import com.nexussmp.nexuscore.NexusCore;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.UUID;

/**
 * A voz da radio: o microfone de quem esta no ar chegando a quem esta ouvindo, onde quer que esteja.
 *
 * <p>O pacote de microfone de quem apresenta e copiado e mandado a cada ouvinte como som <b>sem
 * posicao</b> — o som de um radio, que vem de todo lado e nao enfraquece com a distancia. Quem esta
 * no Nether ouve igual a quem esta ao lado.
 *
 * <h2>Quem ja ouve de perto nao recebe a copia</h2>
 *
 * <p>Quem esta perto de quem apresenta ja escuta essa voz pelo proprio chat de voz, por proximidade.
 * Mandar a copia da radio tambem faria a pessoa ouvir tudo duas vezes, com um eco de fracao de
 * segundo. Entao, para esse ouvinte, a radio cala a voz e deixa a proximidade trabalhar — a musica
 * continua, porque ela e tocada por cada cliente e nao passa por aqui.
 *
 * <p>Cada duvida vai para o lado de <b>mandar</b>, porque ouvir duas vezes e chato e nao ouvir e
 * pior. Por isso a copia so e cortada quando da para ter certeza de que a pessoa esta ouvindo:
 * <ul>
 *   <li>no mesmo grupo do chat de voz — grupo se ouve em qualquer distancia;</li>
 *   <li>fora de grupo, no mesmo mundo e bem dentro do alcance — ate {@link #CLEARLY_AUDIBLE} dele,
 *       onde a proximidade ainda chega com volume de gente conversando.</li>
 * </ul>
 * Grupo diferente, cochicho, beirada do alcance, outro mundo: a radio leva.
 *
 * <h2>O mudo cala a transmissao, nao a pessoa</h2>
 *
 * <p>Com o microfone fechado na mesa, a voz para de ir para a radio — e so isso. Quem esta do lado
 * de quem apresenta continua ouvindo pela proximidade, e e para isso que o mudo existe: conversar
 * com quem esta ali no jogo sem o servidor inteiro ouvir. A radio nunca mexe no chat de voz de
 * ninguem; quem quiser silencio total usa o mudo do proprio Simple Voice Chat.
 */
@ForgeVoicechatPlugin
public final class RadioVoicechatPlugin implements VoicechatPlugin {
    /**
     * Ate que fracao do alcance a voz por proximidade conta como "ja ouvindo".
     *
     * <p>O chat de voz vai baixando o volume com a distancia ate zerar no limite. Na beirada a voz e
     * um sussurro — cortar a radio ali trocaria uma voz clara por uma quase inaudivel. Ate 60% do
     * alcance ela ainda chega com uns 40% do volume, que e ouvir de verdade; dali para fora a radio
     * assume, mesmo que isso signifique ouvir um pouco dos dois.
     */
    private static final double CLEARLY_AUDIBLE = 0.6;

    private static volatile VoicechatServerApi api;

    @Override
    public String getPluginId() {
        // Diferente do plugin das backrooms: cada plugin do chat de voz precisa do seu nome.
        return NexusCore.MOD_ID + "_radio";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> api = event.getVoicechat());
        registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
            api = null;
            RadioVoiceState.clear();
        });
        registration.registerEvent(MicrophonePacketEvent.class, RadioVoicechatPlugin::onMicrophone);
        // O ajuste de volume acontece em quem ouve. So o cliente registra, e so ali a classe que le
        // o estado da radio no cliente chega a ser carregada.
        if (FMLEnvironment.dist.isClient()) {
            registration.registerEvent(ClientReceiveSoundEvent.StaticSound.class, RadioVoiceClient::onStaticSound);
        }
    }

    /** Roda na thread do chat de voz, cinquenta vezes por segundo por pessoa falando. So le a foto. */
    private static void onMicrophone(MicrophonePacketEvent event) {
        VoicechatServerApi server = api;
        VoicechatConnection sender = event.getSenderConnection();
        if (server == null || sender == null) {
            return;
        }
        UUID speaker = sender.getPlayer().getUuid();
        RadioVoiceState.Snapshot snapshot = RadioVoiceState.current();
        // Fora do ar ou com o microfone fechado na mesa: a radio nao leva nada. A proximidade segue
        // por conta do chat de voz, sem a radio encostar nela.
        if (!snapshot.speaking().contains(speaker)) {
            return;
        }

        boolean whispering = event.getPacket().isWhispering();
        double range = server.getVoiceChatDistance() * CLEARLY_AUDIBLE;
        StaticSoundPacket copy = event.getPacket().staticSoundPacketBuilder()
                .channelId(RadioVoiceState.channelOf(speaker))
                .build();

        for (UUID id : snapshot.listeners()) {
            if (id.equals(speaker)) {
                continue;
            }
            VoicechatConnection listener = server.getConnectionOf(id);
            if (listener == null || !listener.isConnected() || listener.isDisabled()) {
                continue;
            }
            if (alreadyHears(sender, listener, range, whispering)) {
                continue;
            }
            server.sendStaticSoundPacketTo(listener, copy);
        }
    }

    /** {@code true} so quando da para ter certeza de que o ouvinte ja escuta essa voz sem a radio. */
    private static boolean alreadyHears(VoicechatConnection speaker, VoicechatConnection listener,
                                        double range, boolean whispering) {
        Group speakerGroup = speaker.isInGroup() ? speaker.getGroup() : null;
        Group listenerGroup = listener.isInGroup() ? listener.getGroup() : null;
        if (speakerGroup != null && listenerGroup != null) {
            return speakerGroup.getId().equals(listenerGroup.getId());
        }
        // Um dos dois em grupo, o outro nao: se isso deixa ouvir por proximidade depende do tipo do
        // grupo. Na duvida, a radio leva.
        if (speakerGroup != null || listenerGroup != null) {
            return false;
        }
        // Cochichando, o alcance encolhe e a API nao conta quanto. Na duvida, a radio leva.
        if (whispering) {
            return false;
        }
        ServerPlayer a = speaker.getPlayer();
        ServerPlayer b = listener.getPlayer();
        if (a.getServerLevel().getServerLevel() != b.getServerLevel().getServerLevel()) {
            return false;
        }
        Position p = a.getPosition();
        Position q = b.getPosition();
        double dx = p.getX() - q.getX();
        double dy = p.getY() - q.getY();
        double dz = p.getZ() - q.getZ();
        return dx * dx + dy * dy + dz * dz <= range * range;
    }
}
