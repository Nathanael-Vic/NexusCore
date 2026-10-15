/*
 * Parte do NexusCore.
 * Derivado de Xander's Sodium Options: Continued 3.4.2
 * (isXander, IMB11, soradotwav) - https://github.com/soradotwav/xanders-sodium-options
 * Licenciado sob a GNU Lesser General Public License v3.0 ou posterior (LGPL-3.0-or-later).
 * Alteracoes: portado para Minecraft 1.21.1 / NeoForge e reorganizado nos pacotes do NexusCore.
 */
package com.nexussmp.nexuscore.compat.sodium.integration;

import com.nexussmp.nexuscore.compat.sodium.util.CategoryDescriptions;

import net.neoforged.fml.ModList;

public enum Compat {
    // IDs do NeoForge nao aceitam hifen: o upstream usava "sodium-extra" e nunca detectava o mod.
    SODIUM_EXTRA("sodium_extra", "xso.category.sodium_extra"),
    MORE_CULLING("moreculling", "xso.category.moreculling"),
    IRIS("iris", "xso.category.iris"),
    LAMBDYNAMICLIGHTS("lambdynlights_runtime", "xso.category.lambdynlights"),
    XSO("nexuscore", "xso.category.xso"),
    ENTITY_VIEW_DISTANCE("entity_view_distance", "xso.category.entity_view_distance");

    public final String modId;
    public final String descriptionKey;
    public final boolean isLoaded;

    Compat(String modId, String descriptionKey) {
        this.modId = modId;
        this.descriptionKey = descriptionKey;
        this.isLoaded = mod(modId);
    }

    public void registerCategory(String categoryName) {
        CategoryDescriptions.registerCategoryModId(categoryName, this.modId);
    }

    private static boolean mod(String id) {
        return ModList.get().isLoaded(id);
    }
}

