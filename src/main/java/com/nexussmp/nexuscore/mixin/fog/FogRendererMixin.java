package com.nexussmp.nexuscore.mixin.fog;

import com.nexussmp.nexuscore.compat.fog.FogGuard;
import com.nexussmp.nexuscore.relic.client.AstralClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A ultima palavra sobre a nevoa, no fim de {@code FogRenderer.setupFog}.
 *
 * <p>Dois mods do pack mexem na nevoa nesse mesmo ponto: o Cirrus recoloca o comeco dela (sem olhar o
 * fim que outro mod pediu) e o Sodium Extra aplica as opcoes dele (que podem deixa-la desligada).
 * Com prioridade acima da padrao, este retorno roda depois dos dois e, nesta ordem:
 * <ol>
 *   <li>devolve a nevoa dos biomas que a encurtam, quando o Cirrus a apagou ({@link FogGuard});</li>
 *   <li>aplica a nevoa da mascara astral por cima de tudo ({@link AstralClient#applyFog}), para que
 *       nenhuma opcao de video consiga tira-la.</li>
 * </ol>
 */
@Mixin(value = FogRenderer.class, priority = 1500)
public abstract class FogRendererMixin {

    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void nexus$lastWord(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean foggy,
                                       float partialTick, CallbackInfo callback) {
        FogGuard.repair(mode);
        AstralClient.applyFog(renderDistance, partialTick);
    }
}
