package com.nexussmp.nexuscore.stats;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Nomes exibidos de dimensoes, biomas e estruturas.
 * Biomas usam a traducao do proprio jogo/mod; estruturas e dimensoes nao tem traducao vanilla, entao usam
 * chaves do NexusCore e caem no ID formatado ({@code ancient_city} → "Ancient City") quando faltar.
 */
public final class ExplorationNames {

    public static Component dimension(ResourceLocation id) {
        return translated("nexuscore.dimension", id);
    }

    public static Component biome(ResourceLocation id) {
        return translated("biome", id);
    }

    public static Component structure(ResourceLocation id) {
        return translated("structure", id);
    }

    private static Component translated(String prefix, ResourceLocation id) {
        return Component.translatableWithFallback(prefix + "." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
                pretty(id.getPath()));
    }

    static String pretty(String path) {
        String last = path.substring(path.lastIndexOf('/') + 1);
        StringBuilder out = new StringBuilder(last.length());
        boolean upper = true;
        for (char c : last.toCharArray()) {
            if (c == '_' || c == '-') {
                out.append(' ');
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    private ExplorationNames() {}
}
