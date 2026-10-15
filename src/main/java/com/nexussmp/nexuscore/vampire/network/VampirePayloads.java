package com.nexussmp.nexuscore.vampire.network;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Os pacotes do modulo: se esta pessoa e lorde, e quanta saturacao de sangue ela tem. */
public final class VampirePayloads {

    /**
     * Servidor -> cliente: "voce progride" ou "voce nao progride".
     *
     * <p>E so isso que o cliente precisa saber. Ele nao decide nada com esse valor — o servidor ja
     * tranca tudo do lado de la — mas e o que faz o botao das habilidades aparecer ou nao no
     * inventario, e mostrar um botao que nao funciona seria pior do que nao mostrar.
     */
    public record Lord(boolean lord) implements CustomPacketPayload {
        public static final Type<Lord> TYPE = new Type<>(NexusCore.id("vampire_lord"));

        public static final StreamCodec<FriendlyByteBuf, Lord> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeBoolean(payload.lord()),
                buf -> new Lord(buf.readBoolean()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Servidor -> cliente: a saturacao do sangue do vampiro.
     *
     * <p>O Vampirism so manda esse numero quando o nivel de sangue muda. Enquanto a saturacao vai
     * gastando, o nivel fica parado — e o cliente continuaria achando que ela esta cheia ate o fim,
     * e ai caindo de uma vez. E o mesmo problema da saturacao comum no jogo, e o mesmo remedio que o
     * AppleSkin usa para ela: um pacote proprio, mandado quando o valor muda.
     */
    public record BloodSaturation(float saturation) implements CustomPacketPayload {
        public static final Type<BloodSaturation> TYPE = new Type<>(NexusCore.id("vampire_blood_saturation"));

        public static final StreamCodec<FriendlyByteBuf, BloodSaturation> STREAM_CODEC = StreamCodec.of(
                (buf, payload) -> buf.writeFloat(payload.saturation()),
                buf -> new BloodSaturation(buf.readFloat()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private VampirePayloads() {}
}
