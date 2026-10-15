package com.nexussmp.nexuscore.mixin.morph;

import net.minecraft.client.renderer.entity.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Para a sombra no chao ser do tamanho da criatura do anel, e nao da de um jogador.
 *
 * <p>Quem desenha a sombra de uma entidade e o jogo, depois do desenho dela, lendo o raio da sombra
 * no desenhista da entidade — o do jogador, mesmo quando quem apareceu foi um ghast.
 */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {

    @Accessor("shadowRadius")
    float nexus$getShadowRadius();

    @Accessor("shadowRadius")
    void nexus$setShadowRadius(float radius);
}
