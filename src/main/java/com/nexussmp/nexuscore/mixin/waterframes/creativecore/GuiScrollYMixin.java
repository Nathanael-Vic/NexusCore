package com.nexussmp.nexuscore.mixin.waterframes.creativecore;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nexussmp.nexuscore.compat.waterframes.client.NexusWaterFramesStyle;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.control.parent.GuiScrollY;
import team.creative.creativecore.common.gui.style.ControlFormatting;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/** A barra de rolagem da playlist desenhava o polegar como botao; vanilla usa widget/scroller. */
@Mixin(value = GuiScrollY.class, remap = false)
public abstract class GuiScrollYMixin {

    @WrapOperation(
            method = "renderContent(Lnet/minecraft/client/gui/GuiGraphics;Lteam/creative/creativecore/common/gui/style/ControlFormatting;ILteam/creative/creativecore/common/util/math/geo/Rect;Lteam/creative/creativecore/common/util/math/geo/Rect;DII)V",
            at = @At(value = "INVOKE",
                    target = "Lteam/creative/creativecore/common/gui/style/GuiStyle;get(Lteam/creative/creativecore/common/gui/style/ControlFormatting$ControlStyleFace;Z)Lteam/creative/creativecore/common/gui/style/display/StyleDisplay;"))
    private StyleDisplay nexus$scroller(GuiStyle style, ControlFormatting.ControlStyleFace face, boolean mouseOver,
                                        Operation<StyleDisplay> original) {
        if (WaterFramesSettings.vanillaStyle() && NexusWaterFramesStyle.isOurs(style)) {
            return WaterFramesSprites.SCROLLER;
        }
        return original.call(style, face, mouseOver);
    }
}
