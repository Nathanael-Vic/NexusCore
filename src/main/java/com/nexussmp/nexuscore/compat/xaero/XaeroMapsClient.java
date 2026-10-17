package com.nexussmp.nexuscore.compat.xaero;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * O mesmo bloqueio de mapas, aplicado pelo cliente em si mesmo.
 *
 * <p>O Xaero e, quase sempre, um mod so de cliente: o servidor nao conhece os efeitos dele e por
 * isso nao tem como manda-los. Quem sabe onde esta, porem, e o proprio cliente: nas backrooms basta
 * olhar a dimensao, e fora do corpo pela mascara o servidor ja avisou. Entao ele aplica o bloqueio
 * em si, que e exatamente o que o Xaero confere antes de desenhar o mapa.
 *
 * <p>Cada parte do mod que quer os mapas fechados registra a sua condicao em {@link #blockWhen};
 * basta uma delas valer para o bloqueio ficar. Nada disto e uma questao de confianca: o mapa e uma
 * coisa do cliente de qualquer jeito. Quem roda o servidor com os mods do Xaero instalados tambem
 * ganha o bloqueio pelo servidor, e os dois caminhos convivem: o efeito ja aplicado simplesmente e
 * visto como aplicado.
 */
public final class XaeroMapsClient {
    private static final List<Predicate<LocalPlayer>> CONDITIONS = new ArrayList<>();
    private static boolean listening;
    private static boolean blocked;

    /** Fecha os mapas enquanto a condicao valer. Chamado no registro dos modulos de cliente. */
    public static void blockWhen(Predicate<LocalPlayer> condition) {
        CONDITIONS.add(condition);
        if (!listening) {
            listening = true;
            NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, XaeroMapsClient::onPlayerTick);
            NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> blocked = false);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (event.getEntity() != player || player == null) {
            return;
        }
        boolean now = false;
        for (Predicate<LocalPlayer> condition : CONDITIONS) {
            if (condition.test(player)) {
                now = true;
                break;
            }
        }
        if (now) {
            // Na hora em que fecha; depois, uma vez por segundo, porque o efeito dura bem mais que isso.
            if (!blocked || player.tickCount % 20 == 0) {
                XaeroMaps.block(player);
            }
        } else if (blocked) {
            XaeroMaps.release(player);
        }
        blocked = now;
    }

    private XaeroMapsClient() {}
}
