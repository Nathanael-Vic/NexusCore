package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import me.srrapero720.waterframes.common.screens.DisplayScreen;
import me.srrapero720.waterframes.common.screens.styles.IconStyles;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.style.Icon;

/**
 * Troca os icones de proporcao de lugar.
 *
 * <p>No WaterFrames, o botao ao lado do campo de largura mostra a seta vertical e o do lado da altura
 * mostra a horizontal — cada icone indica o lado que o botao **calcula**, mas na tela parece que o
 * campo do lado e que muda naquele sentido. Aqui os dois trocam, para o icone combinar com o campo.
 */
@Mixin(value = DisplayScreen.class, remap = false)
public abstract class DisplayResizeIconsMixin {
    private static final String ICON = "Lteam/creative/creativecore/common/gui/style/Icon;";
    private static final String STYLES = "Lme/srrapero720/waterframes/common/screens/styles/IconStyles;";

    @ModifyExpressionValue(method = "<init>",
            at = @At(value = "FIELD", target = STYLES + "EXPAND_Y:" + ICON, opcode = Opcodes.GETSTATIC))
    private Icon nexus$horizontalNextToWidth(Icon original) {
        return WaterFramesSettings.swapResizeIcons() ? IconStyles.EXPAND_X : original;
    }

    @ModifyExpressionValue(method = "<init>",
            at = @At(value = "FIELD", target = STYLES + "EXPAND_X:" + ICON, opcode = Opcodes.GETSTATIC))
    private Icon nexus$verticalNextToHeight(Icon original) {
        return WaterFramesSettings.swapResizeIcons() ? IconStyles.EXPAND_Y : original;
    }
}
