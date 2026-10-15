/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.mixin.sodium.integration;

import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "de.keksuccino.fancymenu.customization.ScreenCustomization", remap = false)
public abstract class FancyMenuScreenCustomizationMixin {
    @Inject(method = "reInitCurrentScreen(ZZ)V", at = @At("HEAD"), cancellable = true, require = 0)
    private static void xso$skipReInitForYaclScreen(boolean resetGuiScale, boolean restoreScreen, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof YACLScreen) {
            ci.cancel();
        }
    }
}
