package com.nexussmp.nexuscore.mixin.space;

import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * As estrelas do Space sao as mesmas do overworld: em vez de gerar outro campo de estrelas,
 * o ceu do Space desenha o buffer que o jogo ja montou.
 */
@Mixin(LevelRenderer.class)
public interface LevelRendererStarsAccessor {

    @Accessor("starBuffer")
    VertexBuffer nexus$starBuffer();
}
