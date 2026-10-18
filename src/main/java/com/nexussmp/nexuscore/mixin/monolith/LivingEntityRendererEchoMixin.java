package com.nexussmp.nexuscore.mixin.monolith;

import com.nexussmp.nexuscore.monolith.client.EchoClient;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A criatura "sem textura" dos monolitos: o modelo continua o mesmo, mas a pele vira o xadrez roxo e
 * preto de textura faltando. So o corpo; armadura e olhos brilhantes ficam como estao.
 *
 * <p>Vale para quem usa o desenho padrao do jogo. Criaturas de mod com animacao propria (GeckoLib)
 * desenham por outro caminho e ficam com a textura delas.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererEchoMixin {

    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void nexus$nullTexture(LivingEntity entity, boolean bodyVisible, boolean translucent, boolean glowing,
                                   CallbackInfoReturnable<RenderType> callback) {
        if (bodyVisible && EchoClient.glitched(entity)) {
            callback.setReturnValue(RenderType.entityCutoutNoCull(EchoClient.NULL_TEXTURE));
        }
    }
}
