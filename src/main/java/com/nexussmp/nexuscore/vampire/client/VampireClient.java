package com.nexussmp.nexuscore.vampire.client;

import com.nexussmp.nexuscore.vampire.network.VampirePayloads;

/**
 * O que este cliente sabe sobre si: se e lorde, e quanta saturacao de sangue tem.
 *
 * <p>Os dois vem do servidor. O cliente nunca decide nada com eles — as regras estao todas do
 * outro lado —, so desenha: o botao das habilidades, e o contorno amarelo das gotas de sangue.
 */
public final class VampireClient {
    private static boolean lord;
    /** {@code NaN} enquanto o servidor nao disse nada. */
    private static float bloodSaturation = Float.NaN;

    public static void apply(VampirePayloads.Lord payload) {
        lord = payload.lord();
    }

    public static void apply(VampirePayloads.BloodSaturation payload) {
        bloodSaturation = payload.saturation();
    }

    public static boolean isLord() {
        return lord;
    }

    public static float bloodSaturation() {
        return bloodSaturation;
    }

    /** Saiu do servidor: a proxima entrada diz de novo. */
    public static void clear() {
        lord = false;
        bloodSaturation = Float.NaN;
    }

    private VampireClient() {}
}
