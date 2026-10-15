package com.nexussmp.nexuscore.backrooms.client.render;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.backrooms.entity.SkinwalkerEntity;
import com.nexussmp.nexuscore.core.client.PlayerSkins;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Ele e desenhado com o modelo de jogador e a skin de alguem do servidor — e essa e a graca: de
 * longe, no fim de um corredor, e so mais uma pessoa.
 *
 * <p>Quando se revela, a skin da lugar a uma textura toda preta. A troca nao e seca: durante a
 * revelacao os dois se alternam, cada vez mais para o lado do preto, o que da a impressao de que a
 * imagem esta falhando. Nao e animacao — e a mesma ideia de uma lampada ruim.
 */
public final class SkinwalkerRenderer extends LivingEntityRenderer<SkinwalkerEntity, PlayerModel<SkinwalkerEntity>> {
    private static final ResourceLocation DARK = NexusCore.id("textures/entity/skinwalker_dark.png");

    private final PlayerModel<SkinwalkerEntity> wideModel;
    private final PlayerModel<SkinwalkerEntity> slimModel;

    public SkinwalkerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        this.wideModel = getModel();
        this.slimModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(SkinwalkerEntity skinwalker) {
        byte phase = skinwalker.phase();
        if (phase == SkinwalkerEntity.PHASE_HUNT) {
            return DARK;
        }
        if (phase == SkinwalkerEntity.PHASE_REVEAL && skinwalker.revealFlicker()) {
            return DARK;
        }
        return skin(skinwalker).texture();
    }

    @Override
    public void render(SkinwalkerEntity skinwalker, float yaw, float partialTick,
                       com.mojang.blaze3d.vertex.PoseStack pose,
                       net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight) {
        this.model = skin(skinwalker).model() == PlayerSkin.Model.SLIM ? slimModel : wideModel;
        super.render(skinwalker, yaw, partialTick, pose, buffers, packedLight);
    }

    private PlayerSkin skin(SkinwalkerEntity skinwalker) {
        return PlayerSkins.of(skinwalker.skinProfile(), skinwalker.getUUID());
    }

    /**
     * O nick por cima da cabeca, como o de qualquer jogador — e ele some no instante em que o
     * disfarce cai.
     *
     * <p>A entidade ja carrega o nome da pessoa imitada e o marca como visivel; sem esta permissao
     * aqui, porem, o desenho nunca acontecia.
     */
    @Override
    protected boolean shouldShowName(SkinwalkerEntity skinwalker) {
        return skinwalker.phase() == SkinwalkerEntity.PHASE_STALK
                && skinwalker.isCustomNameVisible()
                && super.shouldShowName(skinwalker);
    }
}
