/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.mixin.sodium;

import com.nexussmp.nexuscore.compat.sodium.util.XsoTabNavigationScope;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TabManager.class)
public class TabManagerMixin {

    @Inject(method = "setCurrentTab", at = @At("RETURN"))
    private void onTabChanged(Tab tab, boolean clickSound, CallbackInfo ci) {
        if (tab instanceof YACLScreen.CategoryTab categoryTab) {
            categoryTab.updateButtons();
        }

        if (Minecraft.getInstance().screen instanceof XsoTabNavigationScope scoped) {
            scoped.xso$onTabChanged();
        }
    }
}
