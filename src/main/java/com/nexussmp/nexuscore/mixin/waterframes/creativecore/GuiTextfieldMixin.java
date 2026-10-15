package com.nexussmp.nexuscore.mixin.waterframes.creativecore;

import com.nexussmp.nexuscore.compat.waterframes.client.NexusWaterFramesStyle;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import org.spongepowered.asm.mixin.Mixin;
import team.creative.creativecore.common.gui.control.GuiFocusControl;
import team.creative.creativecore.common.gui.control.simple.GuiTextfield;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/**
 * Campos de texto (contadores, campo do slider) com o sprite do campo vanilla, destacado quando focado.
 * O campo de URL do WaterFrames sobrescreve estes metodos e tem mixin proprio.
 */
@Mixin(value = GuiTextfield.class, remap = false)
public abstract class GuiTextfieldMixin extends GuiFocusControl {

    protected GuiTextfieldMixin(String name) {
        super(name);
    }

    @Override
    public StyleDisplay getBorder(GuiStyle style, StyleDisplay display) {
        if (WaterFramesSettings.vanillaStyle() && NexusWaterFramesStyle.isOurs(style)) {
            return isFocused() ? WaterFramesSprites.TEXT_FIELD_HIGHLIGHTED : WaterFramesSprites.TEXT_FIELD;
        }
        return super.getBorder(style, display);
    }

    @Override
    public StyleDisplay getBackground(GuiStyle style, StyleDisplay display) {
        if (WaterFramesSettings.vanillaStyle() && NexusWaterFramesStyle.isOurs(style)) {
            return StyleDisplay.NONE;
        }
        return super.getBackground(style, display);
    }
}
