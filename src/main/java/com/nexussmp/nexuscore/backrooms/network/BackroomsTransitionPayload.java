package com.nexussmp.nexuscore.backrooms.network;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: comeca a animacao de atravessar o chao.
 *
 * <p>So o cliente sabe desenhar isso direito, porque e camera e tela: o servidor manda a fase e
 * quanto ela dura, e o resto acontece por la.
 */
public record BackroomsTransitionPayload(Phase phase, int durationTicks) implements CustomPacketPayload {

    public enum Phase {
        /** Afundando: a camera desce para dentro do chao e a tela apaga. */
        FALL,
        /** Do outro lado: a camera volta ao lugar e a tela abre. */
        ARRIVE
    }

    public static final Type<BackroomsTransitionPayload> TYPE = new Type<>(NexusCore.id("backrooms_transition"));

    public static final StreamCodec<FriendlyByteBuf, BackroomsTransitionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeEnum(payload.phase());
                buf.writeVarInt(payload.durationTicks());
            },
            buf -> new BackroomsTransitionPayload(buf.readEnum(Phase.class), buf.readVarInt()));

    public static BackroomsTransitionPayload fall(int ticks) {
        return new BackroomsTransitionPayload(Phase.FALL, ticks);
    }

    public static BackroomsTransitionPayload arrive(int ticks) {
        return new BackroomsTransitionPayload(Phase.ARRIVE, ticks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
