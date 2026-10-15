package com.nexussmp.nexuscore.display.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nexussmp.nexuscore.display.block.PanelBlock;
import com.nexussmp.nexuscore.display.blockentity.PanelBlockEntity;
import com.nexussmp.nexuscore.display.client.DisplayClientSettings;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * Desenha a tela na face do painel, como o texto de uma placa: sem HUD e sem entidades.
 * So o controlador desenha, cobrindo a tela inteira.
 */
public final class PanelRenderer implements BlockEntityRenderer<PanelBlockEntity> {
    /**
     * Distancia em blocos a frente da face. O fundo e a moldura sao do modelo do bloco (geometria do
     * chunk); aqui so vai o conteudo, um pouco a frente para nao brigar com a moldura.
     */
    private static final float CONTENT_OFFSET = 0.02F;

    private final Font font;

    public PanelRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(PanelBlockEntity panel, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        Level level = panel.getLevel();
        BlockState state = panel.getBlockState();
        if (level == null || !panel.isController() || !DisplayClientSettings.enabled()
                || !(state.getBlock() instanceof PanelBlock)) {
            return;
        }
        Direction facing = state.getValue(PanelBlock.FACING);
        PanelClientCache cache = PanelClientCache.of(panel);
        PanelLayout.Page page = cache.resolve(panel, font);
        float units = cache.unitsPerBlock();

        pose.pushPose();
        // Origem no canto superior esquerdo da face da frente; x para a direita de quem olha, y para baixo.
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        pose.translate(-0.5F, panel.height() - 0.5F, 0.5F + CONTENT_OFFSET);
        pose.scale(1F / units, -1F / units, 1F / units);

        Matrix4f matrix = pose.last().pose();
        page.drawRects(buffers.getBuffer(RenderType.textBackground()), matrix);
        page.drawTexts(font, matrix, buffers);
        pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(PanelBlockEntity panel) {
        return panel.isController() && (panel.width() > 1 || panel.height() > 1);
    }

    @Override
    public int getViewDistance() {
        return DisplayClientSettings.renderDistance();
    }

    @Override
    public AABB getRenderBoundingBox(PanelBlockEntity panel) {
        BlockState state = panel.getBlockState();
        if (!panel.isController() || !(state.getBlock() instanceof PanelBlock)) {
            return BlockEntityRenderer.super.getRenderBoundingBox(panel);
        }
        return PanelClientCache.of(panel).renderBox(panel, state.getValue(PanelBlock.FACING));
    }
}
