package com.nexussmp.nexuscore.compat.fog;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.FogRenderer;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Conserta a nevoa dos biomas que a encurtam (a floresta do Vampirism, por exemplo) quando o Cirrus
 * esta instalado.
 *
 * <p>O Cirrus, no fim do calculo da nevoa, recoloca o comeco dela a 70% da distancia de visao, para
 * a transicao ficar suave. So que ele faz a conta com a distancia de visao inteira, sem olhar o fim
 * que outro mod pediu: se o bioma fecha a nevoa em 30 blocos, o comeco vai parar la nos 150, depois
 * do fim. O Sodium nao desenha nevoa nenhuma antes do comeco, e a nevoa do bioma simplesmente some.
 *
 * <p>Aqui guardamos o que os mods pediram no evento do NeoForge (ouvindo por ultimo, inclusive os
 * cancelados) e, depois de todo mundo mexer, devolvemos o comeco quando ele passou do fim. No caso
 * normal, sem nenhum mod encurtando a nevoa, nada muda.
 */
public final class FogGuard {
    private static FogRenderer.FogMode requestedMode;
    private static float requestedStart;
    private static boolean requested;

    /** Por ultimo no evento: o que ficou valendo e o que algum mod pediu. */
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        requested = event.isCanceled();
        requestedMode = event.getMode();
        requestedStart = event.getNearPlaneDistance();
    }

    /** Chamado no fim de {@code FogRenderer.setupFog}, depois do Cirrus e do Sodium Extra. */
    public static void repair(FogRenderer.FogMode mode) {
        if (!requested || mode != requestedMode) {
            return;
        }
        requested = false;
        float end = RenderSystem.getShaderFogEnd();
        if (RenderSystem.getShaderFogStart() >= end) {
            RenderSystem.setShaderFogStart(Math.min(requestedStart, end * 0.9F));
        }
    }

    private FogGuard() {}
}
