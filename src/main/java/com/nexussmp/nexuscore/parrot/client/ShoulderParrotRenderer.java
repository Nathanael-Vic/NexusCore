package com.nexussmp.nexuscore.parrot.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nexussmp.nexuscore.parrot.ShoulderParrot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Parrot;

/**
 * O papagaio de ombro e desenhado como qualquer papagaio — mesma textura, mesmo modelo, sentado —,
 * com tres animacoes por cima.
 *
 * <ul>
 *   <li><b>Chegada pelo comando:</b> desce do alto girando, cresce passando um pouco do tamanho e
 *       assenta com um pulinho elastico. Ao renascer junto com o dono, so um "pop" curto.</li>
 *   <li><b>Saida:</b> encolhe girando cada vez mais rapido e some num estalo de penas
 *       (as particulas saem da propria entidade).</li>
 *   <li><b>Falando:</b> estufa e balanca no ritmo da fala, para ninguem ter duvida de quem disse.</li>
 * </ul>
 *
 * <p>Sem sombra (ela cairia no chao, longe do ombro) e sem plaquinha de nome por cima (ficaria
 * encavalada com o nome do dono): o nome dele aparece no Jade, ao passar o mouse. E quem carrega nao
 * ve o proprio papagaio em primeira pessoa — ele estaria grudado na camera.
 */
public final class ShoulderParrotRenderer extends ParrotRenderer {
    /** De onde ele desce na chegada pelo comando, em blocos acima do ombro. */
    private static final float DROP_HEIGHT = 1.6F;

    public ShoulderParrotRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    @Override
    public boolean shouldRender(Parrot parrot, Frustum frustum, double camX, double camY, double camZ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (parrot.getVehicle() != null && parrot.getVehicle() == minecraft.getCameraEntity()
                && minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }
        return super.shouldRender(parrot, frustum, camX, camY, camZ);
    }

    @Override
    public void render(Parrot parrot, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        float drop = 0.0F;
        if (parrot instanceof ShoulderParrot shoulder && shoulder.arrival() == ShoulderParrot.ARRIVAL_GRAND) {
            float progress = arrivalProgress(shoulder, partialTick);
            float fall = 1.0F - progress;
            drop = fall * fall * DROP_HEIGHT;
        }
        pose.pushPose();
        pose.translate(0.0F, drop, 0.0F);
        super.render(parrot, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }

    @Override
    protected void scale(Parrot parrot, PoseStack pose, float partialTick) {
        if (!(parrot instanceof ShoulderParrot shoulder)) {
            return;
        }
        float size = 1.0F;
        float spin = 0.0F;

        byte arrival = shoulder.arrival();
        if (arrival != ShoulderParrot.ARRIVAL_NONE) {
            float progress = arrivalProgress(shoulder, partialTick);
            if (arrival == ShoulderParrot.ARRIVAL_GRAND) {
                size = elastic(progress);
                float left = 1.0F - progress;
                spin = left * left * 720.0F;
            } else {
                size = back(Math.min(1.0F, progress * 2.0F));
            }
        }

        if (shoulder.isLeaving() && shoulder.leavingSince() >= 0) {
            float progress = Mth.clamp((shoulder.tickCount - shoulder.leavingSince() + partialTick)
                    / ShoulderParrot.LEAVING_TICKS, 0.0F, 1.0F);
            // Um respiro antes de sumir: estufa um pouco, depois encolhe ate nada.
            size *= progress < 0.2F ? 1.0F + progress : Math.max(0.0F, 1.2F * (1.0F - (progress - 0.2F) / 0.8F));
            spin += progress * progress * 900.0F;
        }

        if (shoulder.isTalking() && shoulder.talkingSince() >= 0) {
            float time = shoulder.tickCount - shoulder.talkingSince() + partialTick;
            float puff = 0.07F * Math.abs(Mth.sin(time * 0.9F));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.45F) * 7.0F));
            pose.scale(size * (1.0F + puff * 0.5F), size * (1.0F + puff), size * (1.0F + puff * 0.5F));
        } else {
            pose.scale(size, size, size);
        }
        if (spin != 0.0F) {
            pose.mulPose(Axis.YP.rotationDegrees(spin));
        }
    }

    @Override
    protected boolean shouldShowName(Parrot parrot) {
        return false;
    }

    private static float arrivalProgress(ShoulderParrot parrot, float partialTick) {
        return Mth.clamp((parrot.tickCount + partialTick) / ShoulderParrot.ARRIVAL_TICKS, 0.0F, 1.0F);
    }

    /** Cresce, passa do ponto e volta balancando ate parar no tamanho certo. */
    private static float elastic(float t) {
        if (t <= 0.0F || t >= 1.0F) {
            return t <= 0.0F ? 0.0F : 1.0F;
        }
        return (float) (Math.pow(2.0, -9.0 * t) * Math.sin((t * 10.0 - 0.75) * (2.0 * Math.PI / 3.0)) + 1.0);
    }

    /** Cresce passando um pouco do tamanho e volta: um "pop". */
    private static float back(float t) {
        float c = 1.70158F;
        float u = t - 1.0F;
        return 1.0F + (c + 1.0F) * u * u * u + c * u * u;
    }
}
