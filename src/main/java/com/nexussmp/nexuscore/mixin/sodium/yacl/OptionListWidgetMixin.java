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
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.gui.DescriptionWithName;
import dev.isxander.yacl3.gui.OptionListWidget;
import java.util.function.Predicate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = OptionListWidget.class, remap = false)
public class OptionListWidgetMixin {

    @Shadow
    @Final
    private ConfigCategory category;

    @Unique
    private DescriptionWithName xso$categoryDefault;

    @ModifyVariable(method = "setHoverDescription", at = @At("HEAD"), argsOnly = true)
    private DescriptionWithName xso$replaceEmptyHover(DescriptionWithName incoming) {
        if (CategoryDescriptions.isLabelOptionSpacer(incoming) || CategoryDescriptions.hasEmptyDescription(incoming)) {
            if (xso$categoryDefault == null) {
                xso$categoryDefault = CategoryDescriptions.getDefault(category.name());
            }
            return xso$categoryDefault;
        }
        return incoming;
    }

    @ModifyVariable(
            method =
                    "nextEntry(Lnet/minecraft/client/gui/navigation/ScreenDirection;Ljava/util/function/Predicate;Ldev/isxander/yacl3/gui/OptionListWidget$Entry;)Ldev/isxander/yacl3/gui/OptionListWidget$Entry;",
            at = @At("HEAD"),
            argsOnly = true)
    private Predicate<OptionListWidget.Entry> xso$skipEmptySpacers(Predicate<OptionListWidget.Entry> predicate) {
        return entry -> predicate.test(entry) && !xso$isEmptySpacer(entry);
    }

    @Unique
    private static boolean xso$isEmptySpacer(OptionListWidget.Entry entry) {
        if (entry instanceof OptionListWidget.OptionEntry optionEntry) {
            Option<?> option = optionEntry.option;
            if (option instanceof LabelOption labelOption) {
                return labelOption.label().getString().isBlank();
            }
        }
        return false;
    }
}
