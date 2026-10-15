package com.nexussmp.nexuscore.mixin.morph;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Para o anel copiar o balanco das pernas da pessoa para a criatura desenhada no lugar dela.
 *
 * <p>O jogo so deixa ler a velocidade e a posicao do passo, e mudar a velocidade; a velocidade do
 * quadro anterior e a posicao nao tem como escrever. Sem as tres, a criatura andaria aos trancos:
 * o desenho mistura o quadro anterior com o atual.
 */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationStateAccessor {

    @Accessor("speedOld")
    float nexus$getSpeedOld();

    @Accessor("speedOld")
    void nexus$setSpeedOld(float speedOld);

    @Accessor("position")
    void nexus$setPosition(float position);
}
