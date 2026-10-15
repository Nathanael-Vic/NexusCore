package com.nexussmp.nexuscore.mixin.waterframes.creativecore;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.nexussmp.nexuscore.compat.waterframes.client.NexusWaterFramesStyle;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.GuiControl;
import team.creative.creativecore.common.gui.control.simple.GuiSlider;
import team.creative.creativecore.common.gui.style.ControlFormatting;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/** Sliders com os sprites do slider vanilla: trilho e alca (realcada ao passar o mouse ou arrastar). */
@Mixin(value = GuiSlider.class, remap = false)
public abstract class GuiSliderMixin extends GuiControl {

    @Shadow
    protected boolean grabbedSlider;

    protected GuiSliderMixin(String name) {
        super(name);
    }

    @Override
    public StyleDisplay getBackground(GuiStyle style, StyleDisplay display) {
        if (WaterFramesSettings.vanillaStyle() && NexusWaterFramesStyle.isOurs(style)) {
            return WaterFramesSprites.SLIDER;
        }
        return super.getBackground(style, display);
    }

    @WrapOperation(
            method = "renderContent(Lnet/minecraft/client/gui/GuiGraphics;Lteam/creative/creativecore/common/util/math/geo/Rect;Lteam/creative/creativecore/common/util/math/geo/Rect;DII)V",
            at = @At(value = "INVOKE",
                    target = "Lteam/creative/creativecore/common/gui/style/GuiStyle;get(Lteam/creative/creativecore/common/gui/style/ControlFormatting$ControlStyleFace;Z)Lteam/creative/creativecore/common/gui/style/display/StyleDisplay;"))
    private StyleDisplay nexus$handle(GuiStyle style, ControlFormatting.ControlStyleFace face, boolean mouseOver,
                                      Operation<StyleDisplay> original,
                                      @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
        if (!(WaterFramesSettings.vanillaStyle() && NexusWaterFramesStyle.isOurs(style))) {
            return original.call(style, face, mouseOver);
        }
        return grabbedSlider || rect.inside(mouseX, mouseY)
                ? WaterFramesSprites.SLIDER_HANDLE_HIGHLIGHTED
                : WaterFramesSprites.SLIDER_HANDLE;
    }
}
