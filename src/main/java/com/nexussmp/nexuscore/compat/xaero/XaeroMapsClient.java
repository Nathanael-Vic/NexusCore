package com.nexussmp.nexuscore.compat.xaero;

import com.nexussmp.nexuscore.backrooms.BackroomsDimensions;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * O mesmo bloqueio de mapas, aplicado pelo cliente em si mesmo.
 *
 * <p>O Xaero e, quase sempre, um mod so de cliente: o servidor nao conhece os efeitos dele e por
 * isso nao tem como manda-los. Quem sabe que esta nas backrooms, porem, e o proprio cliente — basta
 * olhar em que dimensao ele esta. Entao ele aplica o bloqueio em si, que e exatamente o que o Xaero
 * confere antes de desenhar o mapa.
 *
 * <p>Nada disto e uma questao de confianca: o mapa e uma coisa do cliente de qualquer jeito. Quem
 * roda o servidor com os mods do Xaero instalados tambem ganha o bloqueio pelo servidor, e os dois
 * caminhos convivem — o efeito ja aplicado simplesmente e visto como aplicado.
 */
public final class XaeroMapsClient {
    private static boolean inside;

    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getEntity() != minecraft.player || minecraft.player == null) {
            return;
        }
        boolean now = minecraft.player.level().dimension().equals(BackroomsDimensions.BACKROOMS);
        if (now) {
            // Na chegada, na hora; depois, uma vez por segundo — o efeito dura bem mais que isso.
            if (!inside || minecraft.player.tickCount % 20 == 0) {
                XaeroMaps.block(minecraft.player);
            }
        } else if (inside) {
            XaeroMaps.release(minecraft.player);
        }
        inside = now;
    }

    /** Sair do servidor zera a memoria: o proximo mundo comeca do lado de fora. */
    public static void reset() {
        inside = false;
    }

    private XaeroMapsClient() {}
}
