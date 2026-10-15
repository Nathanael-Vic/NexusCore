package com.nexussmp.nexuscore.radio;

import net.minecraft.network.FriendlyByteBuf;

/**
 * O que uma pessoa e dentro de uma transmissao.
 *
 * <p>Sao <b>marcas separadas</b>, e essa separacao e o coracao do sistema:
 *
 * <ul>
 *   <li><b>ouvindo</b> — abriu o audio da transmissao. Nada alem disso;
 *   <li><b>convidado</b> — o apresentador autorizou esta pessoa a participar. Autorizacao nao liga
 *       microfone nenhum: e so a permissao, dada antes ou durante;
 *   <li><b>no ar</b> — a voz dela esta indo junto com a do apresentador, agora;
 *   <li><b>mudo</b> — esta no ar, mas com o microfone fechado. Continua sendo a pessoa do
 *       programa: apenas nada do que ela falar entra na transmissao neste momento;
 *   <li><b>volume da voz</b> — o quanto a voz dela entra na mistura, de 0 a 100. Quem apresenta
 *       mexe nisso para equilibrar convidado alto com convidado baixo, e vale para todo mundo que
 *       estiver ouvindo.
 * </ul>
 *
 * <p>O mudo existe separado do "no ar" por um motivo pratico: quem apresenta precisa poder
 * conversar com quem esta do lado dele no jogo sem que o servidor inteiro ouca. Quando o audio
 * entrar, e esta marca que o canal vai ler para decidir se manda a voz dela adiante.
 *
 * <p>Elas andam juntas sem se misturar: da para estar so ouvindo, estar convidado sem estar no ar,
 * e sair do ar continuando como ouvinte. Guardar isso como marcas (e nao como um estado so) e o que
 * deixa o sistema de convidados entrar depois sem refazer nada.
 *
 * <p>O {@link Role} existe apenas para <i>mostrar</i> uma palavra na tela; quem manda sao as marcas.
 */
public record Membership(boolean listening, boolean invited, boolean onAir, boolean micMuted,
                         int voiceVolume) {
    /** Volume cheio: e onde toda voz comeca. */
    public static final int FULL_VOLUME = 100;
    public static final Membership NONE = new Membership(false, false, false, false, FULL_VOLUME);

    /** Os estados que a interface sabe escrever, do mais forte para o mais fraco. */
    public enum Role {
        NOT_LISTENING,
        LISTENING,
        INVITED,
        ON_AIR
    }

    public Role role() {
        if (onAir) {
            return Role.ON_AIR;
        }
        if (invited) {
            return Role.INVITED;
        }
        return listening ? Role.LISTENING : Role.NOT_LISTENING;
    }

    public boolean isNothing() {
        return !listening && !invited && !onAir;
    }

    public Membership withListening(boolean value) {
        // Quem sai da transmissao sai do ar junto: nao da para estar no ar sem estar la.
        return new Membership(value, invited, value && onAir, micMuted, voiceVolume);
    }

    /** Abre ou fecha o microfone de quem esta no ar. */
    public Membership withMicMuted(boolean value) {
        return new Membership(listening, invited, onAir, value, voiceVolume);
    }

    /** O quanto a voz desta pessoa entra na mistura. */
    public Membership withVoiceVolume(int value) {
        return new Membership(listening, invited, onAir, micMuted, clampVolume(value));
    }

    public static int clampVolume(int value) {
        return Math.max(0, Math.min(FULL_VOLUME, value));
    }

    // ---- rede ----
    // As marcas cabem num byte, e o formato ja nasce pronto para o convidado: quando essa parte
    // chegar, nada muda aqui nem do outro lado.

    public void write(FriendlyByteBuf buf) {
        int flags = (listening ? 1 : 0) | (invited ? 2 : 0) | (onAir ? 4 : 0) | (micMuted ? 8 : 0);
        buf.writeByte(flags);
        buf.writeByte(clampVolume(voiceVolume));
    }

    public static Membership read(FriendlyByteBuf buf) {
        int flags = buf.readByte();
        return new Membership((flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, (flags & 8) != 0,
                clampVolume(buf.readByte()));
    }
}
