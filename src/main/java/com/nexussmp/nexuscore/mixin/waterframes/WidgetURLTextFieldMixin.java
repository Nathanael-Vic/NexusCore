package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nexussmp.nexuscore.compat.waterframes.client.NeutralFormatting;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import me.srrapero720.waterframes.common.screens.styles.ScreenStyles;
import me.srrapero720.waterframes.common.screens.widgets.WidgetURLTextField;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.control.GuiFocusControl;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/** Campo da URL: campo de texto vanilla; borda vermelha so quando a URL e invalida. */
@Mixin(value = WidgetURLTextField.class, remap = false)
public abstract class WidgetURLTextFieldMixin {

    @ModifyReturnValue(method = "getBorder", at = @At("RETURN"))
    private StyleDisplay nexus$field(StyleDisplay original) {
        if (!WaterFramesSettings.vanillaStyle()) {
            return original;
        }
        if (original == ScreenStyles.RED_BORDER && WaterFramesSettings.keepErrorRed()) {
            return WaterFramesSprites.FIELD_ERROR;
        }
        return ((GuiFocusControl) (Object) this).isFocused()
                ? WaterFramesSprites.TEXT_FIELD_HIGHLIGHTED
                : WaterFramesSprites.TEXT_FIELD;
    }

    @ModifyReturnValue(method = "getBackground", at = @At("RETURN"))
    private StyleDisplay nexus$noBackground(StyleDisplay original) {
        // o sprite do campo ja tem o fundo preto
        return WaterFramesSettings.vanillaStyle() ? StyleDisplay.NONE : original;
    }

    @ModifyExpressionValue(method = "getTooltip", require = 0, at = {
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;BLUE:Lnet/minecraft/ChatFormatting;"),
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;RED:Lnet/minecraft/ChatFormatting;")
    })
    private ChatFormatting nexus$tooltipColor(ChatFormatting original) {
        return NeutralFormatting.errorAware(original);
    }
}
