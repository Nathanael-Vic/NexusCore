package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nexussmp.nexuscore.compat.waterframes.client.NexusWaterFramesStyle;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import me.srrapero720.waterframes.common.screens.RemoteControlScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/** Tela do controle remoto. */
@Mixin(value = RemoteControlScreen.class, remap = false)
public abstract class RemoteControlScreenMixin {

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
        return WaterFramesSettings.vanillaStyle() ? StyleDisplay.NONE : original;
    }
}
