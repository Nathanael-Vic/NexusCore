package com.nexussmp.nexuscore.hologram.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * O modo de desenho do holograma: transparente, sem corte de faces (da para ver o lado de dentro,
 * como numa projecao) e sem escrever profundidade, para as partes nao se apagarem entre si.
 *
 * <p>Herda de {@link RenderType} so para alcancar os estados protegidos do jogo — e o jeito normal
 * de um mod criar um modo de desenho proprio.
 */
public final class HologramRenderTypes extends RenderType {
    private static final Function<ResourceLocation, RenderType> HOLOGRAM = Util.memoize(texture -> create(
            "nexuscore_hologram",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            1536,
            true,
            true,
            CompositeState.builder()
                    .setShaderState(new ShaderStateShard(HologramShaders::hologram))
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)));

    public static RenderType hologram(ResourceLocation texture) {
        return HOLOGRAM.apply(texture);
    }

    private HologramRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                                boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
        throw new UnsupportedOperationException("classe utilitaria; existe so para alcancar os estados do RenderType");
    }
}
