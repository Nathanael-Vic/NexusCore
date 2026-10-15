package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.nexussmp.nexuscore.compat.waterframes.client.NeutralFormatting;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import me.srrapero720.waterframes.common.screens.widgets.WidgetStatusIcon;
import net.minecraft.ChatFormatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.control.simple.GuiIcon;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.Icon;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/**
 * Icone de status ao lado da URL ("sem video", carregando, erro...): fundo tipo slot atras do icone e
 * tooltip sem azul. Vermelho so nos estados de erro.
 */
@Mixin(value = WidgetStatusIcon.class, remap = false)
public abstract class WidgetStatusIconMixin extends GuiIcon {

    protected WidgetStatusIconMixin(String name, Icon icon) {
        super(name, icon);
    }

    @Override
    public StyleDisplay getBackground(GuiStyle style, StyleDisplay display) {
        return WaterFramesSettings.vanillaStyle() ? WaterFramesSprites.INSET : super.getBackground(style, display);
    }

    @ModifyExpressionValue(method = "getStatusTooltip", require = 0, at = {
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;AQUA:Lnet/minecraft/ChatFormatting;"),
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;GREEN:Lnet/minecraft/ChatFormatting;"),
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;YELLOW:Lnet/minecraft/ChatFormatting;"),
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;RED:Lnet/minecraft/ChatFormatting;"),
            @At(value = "FIELD", target = "Lnet/minecraft/ChatFormatting;DARK_RED:Lnet/minecraft/ChatFormatting;")
    })
    private ChatFormatting nexus$statusColor(ChatFormatting original) {
        return NeutralFormatting.errorAware(original);
    }
}
