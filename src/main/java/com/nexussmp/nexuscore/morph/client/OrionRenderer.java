package com.nexussmp.nexuscore.morph.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.morph.orion.OrionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * O desenho do ORION: o modelo do Blockbench ({@link OrionModel}) animado aqui, na hora. Quem esta
 * na forma dele e desenhado por este renderizador ({@link MorphRenderer} passa o movimento da
 * pessoa para a criatura do anel).
 *
 * <ul>
 *   <li><b>Andar:</b> pernas e bracos balancam no ritmo do passo (parado, param), o corpo pende de lado
 *       e da um pulinho a cada passo; parado, ele "respira" de leve.</li>
 *   <li><b>Cabeca</b> olha para onde a pessoa olha. <b>Antena</b> balanca sempre um pouco.</li>
 *   <li><b>Dano</b> pisca vermelho como qualquer criatura.</li>
 * </ul>
 */
public final class OrionRenderer extends EntityRenderer<OrionEntity> {
    private static final ResourceLocation TEXTURE = NexusCore.id("textures/entity/orion.png");
    private static final ResourceLocation MODEL = NexusCore.id("models/entity/orion.json");
    /** O modelo tem 20 px de altura: assim ele fica com uns 0,75 bloco. */
    private static final float MODEL_SCALE = 0.6F / 16.0F;
    /**
     * Ritmo das pernas por unidade de passo do jogo. O de gente e 0,6662; as pernas dele sao 4x mais
     * curtas, entao com o ritmo de gente cada passo cobria um quarto do chao, e ele deslizava. Com 2,0
     * (e a lentidao da forma) o pe quase acompanha o chao: passinhos rapidos de robozinho.
     */
    private static final float STRIDE = 2.0F;
    /** Quanto as pernas abrem, em radianos (~57 graus). */
    private static final float LEG_SWING = 1.0F;

    private final OrionModel model;

    public OrionRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.25F;
        this.model = OrionModel.load(context.getResourceManager(), MODEL);
    }

    @Override
    public void render(OrionEntity robot, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light) {
        float age = robot.tickCount + partialTick;
        float bodyRot = Mth.rotLerp(partialTick, robot.yBodyRotO, robot.yBodyRot);
        float headRot = Mth.rotLerp(partialTick, robot.yHeadRotO, robot.yHeadRot);
        float headYaw = Mth.clamp(Mth.wrapDegrees(headRot - bodyRot), -60.0F, 60.0F);
        float headPitch = Mth.lerp(partialTick, robot.xRotO, robot.getXRot());
        float walk = robot.walkAnimation.position(partialTick);
        float speed = Math.min(1.0F, robot.walkAnimation.speed(partialTick));

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        float phase = walk * STRIDE;
        float swing = Mth.cos(phase) * LEG_SWING * speed;
        model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light,
                LivingEntityRenderer.getOverlayCoords(robot, 0.0F),
                (bone, rotation, offset) -> {
                    switch (bone) {
                        case "raiz" -> offset[1] = Math.abs(Mth.sin(phase)) * 0.6F * speed
                                + Mth.sin(age * 0.08F) * 0.15F;
                        case "corpo" -> rotation[2] = Mth.sin(phase) * 0.07F * speed;
                        case "perna_direita" -> rotation[0] = swing;
                        case "perna_esquerda" -> rotation[0] = -swing;
                        case "braco_direito" -> rotation[0] = -swing * 0.8F;
                        case "braco_esquerdo" -> rotation[0] = swing * 0.8F;
                        case "cabeca" -> {
                            rotation[1] = (float) Math.toRadians(-headYaw);
                            rotation[0] = (float) Math.toRadians(-headPitch);
                        }
                        case "antena" -> rotation[2] = Mth.sin(age * 0.12F) * 0.08F - Mth.sin(phase) * 0.15F * speed;
                        default -> {
                        }
                    }
                });
        pose.popPose();
        super.render(robot, entityYaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(OrionEntity robot) {
        return TEXTURE;
    }
}
