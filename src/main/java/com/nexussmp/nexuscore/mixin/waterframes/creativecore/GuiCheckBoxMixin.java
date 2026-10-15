package com.nexussmp.nexuscore.mixin.waterframes.creativecore;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.nexussmp.nexuscore.compat.waterframes.client.PanelText;
import com.nexussmp.nexuscore.compat.waterframes.client.WaterFramesSprites;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.client.render.text.CompiledText;
import team.creative.creativecore.common.gui.GuiControl;
import team.creative.creativecore.common.gui.control.simple.GuiCheckBox;
import team.creative.creativecore.common.gui.style.ControlFormatting;
import team.creative.creativecore.common.gui.style.GuiStyle;
import team.creative.creativecore.common.gui.style.display.StyleDisplay;

/**
 * Checkbox do CreativeCore: caixa 7x7 feita de borda fina + fundo 5x5 + um "x" em texto.
 * O sprite de checkbox do jogo tem 20px e nao cabe sem mudar o layout, entao usamos sprites 7x7 no mesmo
 * estilo (caixa afundada e marca de selecao). Posicoes e tamanhos continuam os do CreativeCore.
 */
@Mixin(value = GuiCheckBox.class, remap = false)
public abstract class GuiCheckBoxMixin {
    private static final String RENDER = "renderContent(Lnet/minecraft/client/gui/GuiGraphics;II)V";

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lteam/creative/creativecore/common/gui/style/GuiStyle;get(Lteam/creative/creativecore/common/gui/style/ControlFormatting$ControlStyleBorder;)Lteam/creative/creativecore/common/gui/style/display/StyleDisplay;"))
    private StyleDisplay nexus$box(GuiStyle style, ControlFormatting.ControlStyleBorder border, Operation<StyleDisplay> original,
                                   @Local(argsOnly = true, ordinal = 0) int mouseX, @Local(argsOnly = true, ordinal = 1) int mouseY) {
        GuiControl self = (GuiControl) (Object) this;
        if (!PanelText.appliesTo(self)) {
            return original.call(style, border);
        }
        return self.rect.inside(mouseX, mouseY) ? WaterFramesSprites.CHECKBOX_HIGHLIGHTED : WaterFramesSprites.CHECKBOX;
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lteam/creative/creativecore/common/gui/style/GuiStyle;get(Lteam/creative/creativecore/common/gui/style/ControlFormatting$ControlStyleFace;Z)Lteam/creative/creativecore/common/gui/style/display/StyleDisplay;"))
    private StyleDisplay nexus$noInnerFill(GuiStyle style, ControlFormatting.ControlStyleFace face, boolean mouseOver,
                                           Operation<StyleDisplay> original) {
        // o sprite da caixa ja tem o miolo
        return PanelText.appliesTo((GuiControl) (Object) this) ? StyleDisplay.NONE : original.call(style, face, mouseOver);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"))
    private int nexus$checkmark(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow,
                                Operation<Integer> original) {
        if (!PanelText.appliesTo((GuiControl) (Object) this)) {
            return original.call(graphics, font, text, x, y, color, shadow);
        }
        // o CreativeCore desenha o "x" em (caixa.x + 1, caixa.y - 1)
        int boxX = x - 1;
        int boxY = y + 1;
        graphics.blitSprite(WaterFramesSprites.CHECKMARK, boxX, boxY, WaterFramesSprites.CHECKBOX_SIZE, WaterFramesSprites.CHECKBOX_SIZE);
        return 0;
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lteam/creative/creativecore/client/render/text/CompiledText;render(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void nexus$panelLabel(CompiledText text, GuiGraphics graphics, Operation<Void> original) {
        if (PanelText.appliesTo((GuiControl) (Object) this)) {
            PanelText.render(text, graphics, original);
        } else {
            original.call(text, graphics);
        }
    }
}
