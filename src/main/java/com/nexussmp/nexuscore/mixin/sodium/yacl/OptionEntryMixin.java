/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.mixin.sodium.yacl;

import com.google.common.collect.ImmutableList;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.gui.AbstractWidget;
import dev.isxander.yacl3.gui.OptionListWidget;
import dev.isxander.yacl3.gui.TextScaledButtonWidget;
import dev.isxander.yacl3.gui.utils.WidgetUtils;
import java.util.List;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OptionListWidget.OptionEntry.class, remap = false)
public class OptionEntryMixin {
    @Unique
    private static final GuiEventListener xso$NON_FOCUSABLE_SPACER = new GuiEventListener() {
        @Override
        public void setFocused(boolean focused) {}

        @Override
        public boolean isFocused() {
            return false;
        }

        @Override
        public ComponentPath nextFocusPath(@NonNull FocusNavigationEvent event) {
            return null;
        }
    };

    @Shadow
    @Final
    public Option<?> option;

    @Shadow
    @Final
    private TextScaledButtonWidget resetButton;

    @Shadow
    @Final
    public AbstractWidget widget;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void xso$activateResetButtonWithKeyboard(
            int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (this.resetButton != null
                && this.resetButton.isFocused()
                && WidgetUtils.keyPressed(this.resetButton, keyCode, scanCode, modifiers)) {
            ((OptionListWidget.OptionEntry) (Object) this).setFocused(this.widget);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "children", at = @At("HEAD"), cancellable = true)
    private void xso$skipSpacerFromTabFocus(CallbackInfoReturnable<List<? extends GuiEventListener>> cir) {
        if (xso$isEmptySpacerOption()) {
            cir.setReturnValue(ImmutableList.of(xso$NON_FOCUSABLE_SPACER));
        }
    }

    @Unique
    private boolean xso$isEmptySpacerOption() {
        return this.option instanceof LabelOption labelOption
                && labelOption.label().getString().isBlank();
    }
}
