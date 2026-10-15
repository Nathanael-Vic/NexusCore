package com.nexussmp.nexuscore.mixin.waterframes.creativecore;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nexussmp.nexuscore.compat.waterframes.client.PanelText;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.creative.creativecore.client.render.text.CompiledText;
import team.creative.creativecore.common.gui.GuiControl;
import team.creative.creativecore.common.gui.control.simple.GuiLabel;

/** Rotulo sobre o painel: #404040 sem sombra. Botoes (GuiButton herda daqui) tem face CLICKABLE e ficam de fora. */
@Mixin(value = GuiLabel.class, remap = false)
public abstract class GuiLabelMixin {

    @WrapOperation(method = "renderContent(Lnet/minecraft/client/gui/GuiGraphics;II)V", at = @At(value = "INVOKE",
            target = "Lteam/creative/creativecore/client/render/text/CompiledText;render(Lnet/minecraft/client/gui/GuiGraphics;)V"))
    private void nexus$panelLabel(CompiledText text, GuiGraphics graphics, Operation<Void> original) {
        if (PanelText.appliesTo((GuiControl) (Object) this)) {
            PanelText.render(text, graphics, original);
        } else {
            original.call(text, graphics);
        }
    }
}
