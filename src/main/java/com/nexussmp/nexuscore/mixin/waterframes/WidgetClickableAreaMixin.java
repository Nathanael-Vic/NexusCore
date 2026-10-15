package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nexussmp.nexuscore.compat.waterframes.client.NeutralFormatting;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import me.srrapero720.waterframes.common.screens.widgets.WidgetClickableArea;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.control.simple.GuiIcon;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.Icon;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/** Seletor de posicao do video (3x3): fundo tipo slot e tooltip sem azul. */
@Mixin(value = WidgetClickableArea.class, remap = false)
public abstract class WidgetClickableAreaMixin extends GuiIcon {

    protected WidgetClickableAreaMixin(String name, Icon icon) {
        super(name, icon);
    }

    @Override
    public StyleDisplay getBackground(GuiStyle style, StyleDisplay display) {
        return WaterFramesSettings.vanillaStyle() ? WaterFramesSprites.INSET : super.getBackground(style, display);
    }

    @ModifyExpressionValue(method = "getTooltip", require = 0,
            at = @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;AQUA:Lnet/minecraft/ChatFormatting;"))
    private ChatFormatting nexus$tooltipColor(ChatFormatting original) {
        return NeutralFormatting.neutral(original);
    }
}
