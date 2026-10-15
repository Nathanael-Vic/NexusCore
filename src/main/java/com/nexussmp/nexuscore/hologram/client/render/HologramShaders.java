package com.nexussmp.nexuscore.hologram.client.render;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.hologram.client.HologramClientSettings;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * O shader que da o efeito de projecao: cor unica, linhas de varredura, oscilacao e brilho na silhueta.
 *
 * <p>Os valores da config viram uniformes uma vez por frame, antes de as entidades serem desenhadas.
 * A altura da camera tambem entra: sem ela, as linhas escorregariam na tela quando o jogador pula.
 */
public final class HologramShaders {
    @Nullable
    private static ShaderInstance hologram;

    public static void onRegisterShaders(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(), NexusCore.id("hologram"), DefaultVertexFormat.NEW_ENTITY),
                    shader -> hologram = shader);
        } catch (IOException e) {
            NexusCore.LOGGER.error("[NexusCore] shader do holograma nao carregou: {}", e.toString());
        }
    }

    @Nullable
    public static ShaderInstance hologram() {
        return hologram;
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (hologram == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Uniform color = hologram.getUniform("NexusHoloColor");
        if (color != null) {
            color.set(HologramClientSettings.red(), HologramClientSettings.green(), HologramClientSettings.blue(),
                    HologramClientSettings.rim());
        }
        Uniform params = hologram.getUniform("NexusHoloParams");
        if (params != null) {
            params.set(HologramClientSettings.alpha(), HologramClientSettings.scanlines(),
                    HologramClientSettings.flicker(), (float) event.getCamera().getPosition().y);
        }
    }

    private HologramShaders() {}
}
