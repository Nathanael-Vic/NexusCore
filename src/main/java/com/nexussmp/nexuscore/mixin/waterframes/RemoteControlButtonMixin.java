package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/**
 * Botoes do controle remoto ({@code RemoteControlScreen$1} liga/desliga e {@code $2} os demais) forcam
 * vermelho/azul fixos e, com isso, perdem o realce de mouse. Aqui devolvemos o display que o proprio estilo
 * ja resolveu (botao vanilla normal ou realcado). Desligar nao e erro, entao nao fica vermelho.
 */
@Mixin(targets = {
        "me.srrapero720.waterframes.common.screens.RemoteControlScreen$1",
        "me.srrapero720.waterframes.common.screens.RemoteControlScreen$2"
}, remap = false)
public abstract class RemoteControlButtonMixin {

    @ModifyReturnValue(method = "getBorder", at = @At("RETURN"))
    private StyleDisplay nexus$border(StyleDisplay original, @Local(argsOnly = true) StyleDisplay resolvedByStyle) {
        return WaterFramesSettings.vanillaStyle() ? resolvedByStyle : original;
    }

    @ModifyReturnValue(method = "getBackground", at = @At("RETURN"))
    private StyleDisplay nexus$background(StyleDisplay original, @Local(argsOnly = true) StyleDisplay resolvedByStyle) {
        return WaterFramesSettings.vanillaStyle() ? resolvedByStyle : original;
    }
}
