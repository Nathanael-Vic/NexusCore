package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nexussmp.nexuscore.compat.waterframes.client.NeutralFormatting;
import com.nexussmp.nexuscore.compat.waterframes.client.NexusWaterFramesStyle;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import me.srrapero720.waterframes.common.screens.DisplayScreen;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/** Tela do frame/TV/projetor: estilo, painel e textos de estado sem azul. */
@Mixin(value = DisplayScreen.class, remap = false)
public abstract class DisplayScreenMixin {

    @ModifyReturnValue(method = "getStyle", at = @At("RETURN"))
    private GuiStyle nexus$style(GuiStyle original) {
        return WaterFramesSettings.vanillaStyle() ? NexusWaterFramesStyle.get() : original;
    }

    @ModifyReturnValue(method = "getBorder", at = @At("RETURN"))
    private StyleDisplay nexus$panel(StyleDisplay original) {
        return WaterFramesSettings.vanillaStyle() ? WaterFramesSprites.PANEL : original;
    }

    @ModifyReturnValue(method = "getBackground", at = @At("RETURN"))
    private StyleDisplay nexus$noBackground(StyleDisplay original) {
        // o sprite do painel ja cobre o miolo
        return WaterFramesSettings.vanillaStyle() ? StyleDisplay.NONE : original;
    }

    /** AQUA nos rotulos e GREEN/RED de ligado/desligado: nao e erro, vira a cor padrao. */
    @ModifyExpressionValue(method = "tick", require = 0, at = {
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;AQUA:Lnet/minecraft/ChatFormatting;"),
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;GREEN:Lnet/minecraft/ChatFormatting;"),
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;RED:Lnet/minecraft/ChatFormatting;")
    })
    private ChatFormatting nexus$neutralText(ChatFormatting original) {
        return NeutralFormatting.neutral(original);
    }
}
