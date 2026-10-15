package com.nexussmp.nexuscore.compat.waterframes.client;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.resources.ResourceLocation;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/**
 * Sprites da interface. Os do namespace {@code nexuscore} sao arte nossa; os de controle usam direto os
 * sprites do Minecraft, entao um resource pack que muda os botoes do jogo muda os do WaterFrames junto.
 */
public final class WaterFramesSprites {
    // arte nossa: assets/nexuscore/textures/gui/sprites/waterframes/
    public static final StyleDisplay PANEL = own("panel");
    public static final StyleDisplay INSET = own("inset");
    public static final StyleDisplay INSET_HIGHLIGHTED = own("inset_highlighted");
    public static final StyleDisplay FIELD_ERROR = own("field_error");
    public static final StyleDisplay CHECKBOX = own("checkbox");
    public static final StyleDisplay CHECKBOX_HIGHLIGHTED = own("checkbox_highlighted");
    public static final ResourceLocation CHECKMARK = NexusCore.id("waterframes/checkmark");

    // sprites do jogo
    public static final StyleDisplay BUTTON = vanilla("widget/button");
    public static final StyleDisplay BUTTON_HIGHLIGHTED = vanilla("widget/button_highlighted");
    public static final StyleDisplay BUTTON_DISABLED = vanilla("widget/button_disabled");
    public static final StyleDisplay SLIDER = vanilla("widget/slider");
    public static final StyleDisplay SLIDER_HANDLE = vanilla("widget/slider_handle");
    public static final StyleDisplay SLIDER_HANDLE_HIGHLIGHTED = vanilla("widget/slider_handle_highlighted");
    public static final StyleDisplay TEXT_FIELD = vanilla("widget/text_field");
    public static final StyleDisplay TEXT_FIELD_HIGHLIGHTED = vanilla("widget/text_field_highlighted");
    public static final StyleDisplay SCROLLER = vanilla("widget/scroller");

    /** Tamanho fixo do checkbox do CreativeCore (nao mudar: define o layout). */
    public static final int CHECKBOX_SIZE = 7;

    private static StyleDisplay own(String name) {
        return new NineSliceDisplay(NexusCore.id("waterframes/" + name));
    }

    private static StyleDisplay vanilla(String path) {
        return new NineSliceDisplay(ResourceLocation.withDefaultNamespace(path));
    }

    private WaterFramesSprites() {}
}
