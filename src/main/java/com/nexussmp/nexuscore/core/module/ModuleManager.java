package com.nexussmp.nexuscore.core.module;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.util.ModPresence;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/** Decide quais modulos registram e loga uma linha por modulo. */
public final class ModuleManager {

    public static void registerAll(List<NexusModule> modules, IEventBus modBus, ModContainer container) {
        for (NexusModule module : modules) {
            String skipReason = skipReason(module);
            if (skipReason != null) {
                NexusCore.LOGGER.info("[NexusCore] {}: ignorado ({})", module.id(), skipReason);
                continue;
            }
            module.register(modBus, container);
            NexusCore.LOGGER.info("[NexusCore] {}: ativo", module.id());
        }
    }

    /** {@code null} quando o modulo pode registrar. */
    private static String skipReason(NexusModule module) {
        if (module.clientOnly() && FMLEnvironment.dist != Dist.CLIENT) {
            return "so cliente";
        }
        for (ModRequirement requirement : module.requirements()) {
            ModPresence.Result result = ModPresence.check(requirement);
            if (!result.ok()) {
                return result.describe(requirement);
            }
        }
        return null;
    }

    private ModuleManager() {}
}
