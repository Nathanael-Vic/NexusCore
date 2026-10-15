package com.nexussmp.nexuscore.core.config;

import com.nexussmp.nexuscore.core.module.ModuleIds;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.LinkedHashMap;
import java.util.Map;

/** {@code config/nexuscore-common.toml}: liga/desliga de cada modulo. */
public final class CommonConfig {
    public static final ModConfigSpec SPEC;
    private static final Map<String, ModConfigSpec.BooleanValue> MODULE_ENABLED = new LinkedHashMap<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Liga ou desliga cada modulo do NexusCore.",
                        "Um modulo desligado continua registrado (blocos e rede nao podem sumir do mundo),",
                        "mas nao executa comportamento nenhum.")
                .push("modules");
        for (String id : ModuleIds.ALL) {
            // "compat.sodium" vira a secao [modules.compat.sodium]
            String[] path = id.split("\\.");
            builder.push(java.util.List.of(path));
            MODULE_ENABLED.put(id, builder.define("enabled", true));
            builder.pop(path.length);
        }
        builder.pop();
        SPEC = builder.build();
    }

    /**
     * {@code true} por padrao enquanto a config ainda nao carregou (ex.: tela de titulo aparecendo cedo),
     * para nunca desligar um modulo por acidente.
     */
    public static boolean isModuleEnabled(String moduleId) {
        ModConfigSpec.BooleanValue value = MODULE_ENABLED.get(moduleId);
        if (value == null || !SPEC.isLoaded()) {
            return true;
        }
        return value.get();
    }

    private CommonConfig() {}
}
