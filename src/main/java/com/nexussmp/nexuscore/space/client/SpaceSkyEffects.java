package com.nexussmp.nexuscore.space.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.nexussmp.nexuscore.mixin.space.LevelRendererStarsAccessor;
import com.nexussmp.nexuscore.space.SpaceDimensions;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * O ceu do Space: uma cor so em todas as direcoes, mais as estrelas do overworld.
 *
 * <p>Nada de sol, lua, nuvens, chuva, laranja de pordo-sol nem o "degrade" do horizonte. O fundo nao e
 * desenhado: a tela ja e limpa com a cor da nevoa, e a nevoa esta presa na cor do espaco
 * ({@link SpaceSkyEffects#getBrightnessDependentFogColor}, reforcada por {@link SpaceSkyRenderer}).
 * Assim nao existe faixa mais clara perto do horizonte nem metade de baixo mais escura.
 */
public final class SpaceSkyEffects extends DimensionSpecialEffects {
    /** Brilho fixo: nao depende da hora nem da chuva do Nexus. */
    private static final float STAR_BRIGHTNESS = 0.8F;
    private static final Vec3 FOG_COLOR = Vec3.fromRGB24(SpaceDimensions.SKY_COLOR);

    public SpaceSkyEffects() {
        // cloudLevel NaN = sem nuvens (mesmo mecanismo do Nether e do End); SkyType.NONE = o jogo nao
        // desenha ceu por conta propria.
        super(Float.NaN, false, SkyType.NONE, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return FOG_COLOR;
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    @Nullable
    public float[] getSunriseColor(float timeOfDay, float partialTick) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix,
                             Camera camera, Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        // Devolvendo true, o jogo nao desenha mais nada de ceu: nem sol, nem lua, nem disco de horizonte.
        // Como ele tambem deixa de restaurar a nevoa, isso e por nossa conta.
        if (!SpaceClientSettings.enabled() || !renderStars(modelViewMatrix, projectionMatrix, setupFog)) {
            setupFog.run();
        }
        return true;
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
                                double camX, double camY, double camZ, Matrix4f frustumMatrix, Matrix4f projectionMatrix) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float partialTick, LightTexture lightTexture,
                                     double camX, double camY, double camZ) {
        return true;
    }

    /** Mesma sequencia do jogo para as estrelas, com brilho constante e sem rotacao (a hora e fixa). */
    private static boolean renderStars(Matrix4f modelViewMatrix, Matrix4f projectionMatrix, Runnable setupFog) {
        VertexBuffer stars = starBuffer();
        if (stars == null) {
            return false;
        }
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(STAR_BRIGHTNESS, STAR_BRIGHTNESS, STAR_BRIGHTNESS, STAR_BRIGHTNESS);
        FogRenderer.setupNoFog();
        stars.bind();
        stars.drawWithShader(modelViewMatrix, projectionMatrix, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        setupFog.run();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        return true;
    }

    @Nullable
    private static VertexBuffer starBuffer() {
        return Minecraft.getInstance().levelRenderer instanceof LevelRendererStarsAccessor accessor
                ? accessor.nexus$starBuffer()
                : null;
    }
}
