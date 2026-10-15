package com.nexussmp.nexuscore.core.util;

/**
 * Um mod de que um modulo depende, com a faixa de versao testada (sintaxe Maven, igual ao mods.toml).
 */
public record ModRequirement(String modId, String versionRange) {

    public static ModRequirement any(String modId) {
        return new ModRequirement(modId, "[0,)");
    }
}
