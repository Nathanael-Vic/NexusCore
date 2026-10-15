/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.mixin.sodium.yacl;

import com.nexussmp.nexuscore.compat.sodium.util.CategoryDescriptions;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.gui.OptionDescriptionWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = YACLScreen.CategoryTab.class, remap = false)
public class CategoryTabMixin {

    @Shadow
    private OptionDescriptionWidget descriptionWidget;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void xso$setDefaultDescription(
            YACLScreen screen, ConfigCategory category, ScreenRectangle tabArea, CallbackInfo ci) {
        this.descriptionWidget.setOptionDescription(CategoryDescriptions.getDefault(category.name()));
    }
}
