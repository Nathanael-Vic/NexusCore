package com.nexussmp.nexuscore.backrooms.network;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor → cliente: o que a tela da vitima deve fazer.
 *
 * <p>O servidor manda so o momento; desenhar tremor, escuro e chiado e coisa de cliente.
 */
public record SkinwalkerPayload(Stage stage, int durationTicks) implements CustomPacketPayload {

    public enum Stage {
        /** Ele esta se revelando: a tela vai fechando e tremendo junto. */
        REVEAL,
        /** Virou bicho e vem vindo: pulso vermelho nas bordas. */
        HUNT,
        /** Pegou: corte seco para o preto, chiado de sinal perdido e o "voce foi pego". */
        CAUGHT,
        /** Acabou (saiu pela porta, foi trazido de volta): a tela volta ao normal. */
        CLEAR
    }

    public static final Type<SkinwalkerPayload> TYPE = new Type<>(NexusCore.id("skinwalker"));

    public static final StreamCodec<FriendlyByteBuf, SkinwalkerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeEnum(payload.stage());
                buf.writeVarInt(payload.durationTicks());
            },
            buf -> new SkinwalkerPayload(buf.readEnum(Stage.class), buf.readVarInt()));

    public static SkinwalkerPayload reveal(int ticks) {
        return new SkinwalkerPayload(Stage.REVEAL, ticks);
    }

    public static SkinwalkerPayload hunt() {
        return new SkinwalkerPayload(Stage.HUNT, 0);
    }

    public static SkinwalkerPayload caught() {
        // Um pouco mais que a espera do servidor: quem encerra a tela e a volta ao mundo, nunca o relogio.
        return new SkinwalkerPayload(Stage.CAUGHT, 90);
    }

    public static SkinwalkerPayload clear() {
        return new SkinwalkerPayload(Stage.CLEAR, 0);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
