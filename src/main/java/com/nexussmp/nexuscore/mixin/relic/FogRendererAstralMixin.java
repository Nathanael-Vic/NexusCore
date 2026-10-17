package com.nexussmp.nexuscore.mixin.relic;

import com.nexussmp.nexuscore.relic.client.AstralClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A nevoa de quem esta fora do corpo, aplicada por ultimo.
 *
 * <p>O evento de nevoa do NeoForge roda no meio de {@code setupFog}, e o Sodium Extra reescreve a
 * nevoa no fim dele: com a nevoa desligada nas opcoes do Sodium Extra, a da mascara sumia e o mundo
 * acabava seco na borda dos chunks. Prioridade acima da padrao para este retorno rodar depois do dele.
 */
@Mixin(value = FogRenderer.class, priority = 1500)
public abstract class FogRendererAstralMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void nexus$astralFog(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean foggy,
                                        float partialTick, CallbackInfo callback) {
        AstralClient.applyFog(renderDistance, partialTick);
    }
}
