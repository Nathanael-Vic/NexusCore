package com.nexussmp.nexuscore.mixin.waterframes;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSettings;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import me.srrapero720.waterframes.common.screens.styles.ScreenStyles;
import me.srrapero720.waterframes.common.screens.widgets.WidgetPlaylistEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/** Linha da playlist: area "afundada" tipo slot, mais clara quando realcada. */
@Mixin(value = WidgetPlaylistEntry.class, remap = false)
public abstract class WidgetPlaylistEntryMixin {

    @ModifyReturnValue(method = "getBackground", at = @At("RETURN"))
    private StyleDisplay nexus$inset(StyleDisplay original) {
        if (!WaterFramesSettings.vanillaStyle()) {
            return original;
        }
        return original == ScreenStyles.DARK_BLUE_HIGHLIGHT
                ? WaterFramesSprites.INSET_HIGHLIGHTED
                : WaterFramesSprites.INSET;
    }
}
