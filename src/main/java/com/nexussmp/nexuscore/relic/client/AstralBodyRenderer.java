package com.nexussmp.nexuscore.relic.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexussmp.nexuscore.core.client.PlayerSkins;
import com.nexussmp.nexuscore.relic.astral.AstralBody;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * O corpo deixado para tras e desenhado como a pessoa: modelo de jogador (fino ou largo, conforme a
 * skin), armadura e o que segurava, e o nome em cima. Parado, de cabeca baixa.
 */
public final class AstralBodyRenderer extends LivingEntityRenderer<AstralBody, PlayerModel<AstralBody>> {
    private final PlayerModel<AstralBody> wideModel;
    private final PlayerModel<AstralBody> slimModel;

    public AstralBodyRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = getModel();
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(AstralBody body) {
        return skin(body).texture();
    }

    @Override
    public void render(AstralBody body, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        this.model = skin(body).model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;
        super.render(body, yaw, partialTick, pose, buffers, light);
    }

    @Override
    protected boolean shouldShowName(AstralBody body) {
        return body.isCustomNameVisible() && super.shouldShowName(body);
    }

    private static PlayerSkin skin(AstralBody body) {
        return PlayerSkins.of(body.profile(), body.getUUID());
    }
}
