package com.nexussmp.nexuscore.radio.voice;

import com.nexussmp.nexuscore.radio.Membership;
import com.nexussmp.nexuscore.radio.client.RadioClient;
import com.nexussmp.nexuscore.radio.network.RadioPayloads;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;

import java.util.UUID;

/**
 * O volume da voz da radio, aplicado em quem ouve.
 *
 * <p>Dois botoes mandam nesse volume, e os dois so existem na radio. O da <b>mesa de som</b>, que
 * quem apresenta regula para todo mundo; e o <b>volume pessoal</b>, que cada ouvinte regula para si.
 * O que chega aos ouvidos e um vezes o outro — a mesma conta que ja valia para a musica.
 *
 * <p>A conta e feita aqui, com o audio ja aberto pelo chat de voz, e nao no servidor: la ela
 * obrigaria a abrir e fechar o audio de novo para cada pessoa, cinquenta vezes por segundo. Aqui
 * custa uma multiplicacao por amostra, na maquina de quem ouve.
 *
 * <p>E nada disso encosta nas configuracoes do Simple Voice Chat. Baixar a radio nao baixa a voz de
 * ninguem no chat de voz — era a preocupacao desde o comeco, e continua valendo.
 */
final class RadioVoiceClient {

    /** Roda na thread de audio do chat de voz: so le o estado que o cliente ja tem. */
    static void onStaticSound(ClientReceiveSoundEvent.StaticSound event) {
        RadioPayloads.Live live = RadioClient.live();
        if (live == null) {
            return;
        }
        UUID speaker = RadioVoiceState.speakerOf(event.getId());
        for (RadioPayloads.Voice voice : live.voices()) {
            if (!voice.id().equals(speaker)) {
                continue;
            }
            int volume = voice.volume() * RadioClient.personalVolume() / Membership.FULL_VOLUME;
            if (volume >= Membership.FULL_VOLUME) {
                return;
            }
            short[] audio = event.getRawAudio();
            if (audio == null || audio.length == 0) {
                return;
            }
            float gain = Math.max(0, volume) / (float) Membership.FULL_VOLUME;
            short[] scaled = new short[audio.length];
            for (int i = 0; i < audio.length; i++) {
                scaled[i] = (short) (audio[i] * gain);
            }
            event.setRawAudio(scaled);
            return;
        }
    }

    private RadioVoiceClient() {}
}
