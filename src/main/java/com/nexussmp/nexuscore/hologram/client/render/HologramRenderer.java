package com.nexussmp.nexuscore.hologram.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexussmp.nexuscore.hologram.client.HologramClientSettings;
import com.nexussmp.nexuscore.hologram.entity.HologramEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * Desenha o holograma com o modelo de jogador e a skin de quem gravou, passando tudo pelo shader
 * de projecao. Sem sombra no chao e sem nome em cima: e uma projecao, nao um jogador.
 */
public final class HologramRenderer extends LivingEntityRenderer<HologramEntity, PlayerModel<HologramEntity>> {
    private final PlayerModel<HologramEntity> wideModel;
    private final PlayerModel<HologramEntity> slimModel;

    public HologramRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.0F);
        this.wideModel = getModel();
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HologramItemLayer(this, context));
    }

    @Override
    public void render(HologramEntity hologram, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight) {
        if (!HologramClientSettings.enabled()) {
            return;
        }
        // A skin define se o modelo e de braco fino ou grosso.
        this.model = HologramSkins.skin(hologram).model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;
        // O shader ignora o mapa de luz: a projecao brilha sozinha, de dia ou de noite.
        super.render(hologram, yaw, partialTick, pose, buffers, LightTexture.FULL_BRIGHT);
    }

    @Override
    public ResourceLocation getTextureLocation(HologramEntity hologram) {
        return HologramSkins.skin(hologram).texture();
    }

    /**
     * Se o shader nao carregar (driver antigo, erro de compilacao), o holograma aparece translucido e
     * brilhante pelo modo de desenho do proprio jogo, sem os efeitos — melhor que sumir ou crashar.
     */
    @Override
    protected RenderType getRenderType(HologramEntity hologram, boolean bodyVisible, boolean translucent, boolean glowing) {
        ResourceLocation texture = getTextureLocation(hologram);
        return HologramShaders.hologram() != null
                ? HologramRenderTypes.hologram(texture)
                : RenderType.entityTranslucentEmissive(texture);
    }

    @Override
    protected boolean shouldShowName(HologramEntity hologram) {
        return false;
    }

    @Override
    protected float getShadowRadius(HologramEntity hologram) {
        return 0.0F;
    }

    /** O item na mao tambem entra no efeito: o mesmo shader, com a textura do atlas de itens. */
    private static final class HologramItemLayer extends ItemInHandLayer<HologramEntity, PlayerModel<HologramEntity>> {
        private HologramItemLayer(HologramRenderer parent, EntityRendererProvider.Context context) {
            super(parent, context.getItemInHandRenderer());
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffers, int packedLight, HologramEntity hologram,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (!HologramClientSettings.items()) {
                return;
            }
            // Todo o item vai para o mesmo modo de desenho da projecao, com a textura do atlas de itens.
            MultiBufferSource target = HologramShaders.hologram() == null
                    ? buffers
                    : type -> buffers.getBuffer(HologramRenderTypes.hologram(InventoryMenu.BLOCK_ATLAS));
            super.render(pose, target, LightTexture.FULL_BRIGHT, hologram, limbSwing, limbSwingAmount,
                    partialTick, ageInTicks, netHeadYaw, headPitch);
        }
    }
}
