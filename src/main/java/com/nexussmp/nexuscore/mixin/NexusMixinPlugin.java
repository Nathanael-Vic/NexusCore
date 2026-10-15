package com.nexussmp.nexuscore.mixin;

import com.nexussmp.nexuscore.core.util.CompatRequirements;
import com.nexussmp.nexuscore.core.util.ModPresence;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decide mixin por mixin, pelo pacote, se o mod alvo esta presente e na faixa testada.
 * Sem o mod (ou fora da faixa), o mixin simplesmente nao e aplicado: sem crash.
 */
public final class NexusMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("NexusCore/Mixin");
    private static final String BASE = "com.nexussmp.nexuscore.mixin.";

    /** Loga cada motivo de pulo uma unica vez, nao uma vez por mixin. */
    private final Set<String> loggedSkips = new HashSet<>();

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(BASE + "sodium.integration.")) {
            return all(mixinClassName, CompatRequirements.SODIUM, integrationFor(mixinClassName));
        }
        if (mixinClassName.startsWith(BASE + "sodium.")) {
            return all(mixinClassName, CompatRequirements.SODIUM);
        }
        if (mixinClassName.startsWith(BASE + "envelope.")) {
            return all(mixinClassName, CompatRequirements.ENVELOPE);
        }
        if (mixinClassName.startsWith(BASE + "magic.")) {
            return all(mixinClassName, CompatRequirements.IRONS_SPELLBOOKS);
        }
        if (mixinClassName.startsWith(BASE + "waterframes.")) {
            return all(mixinClassName, CompatRequirements.WATERFRAMES, CompatRequirements.CREATIVECORE);
        }
        return true;
    }

    private static ModRequirement integrationFor(String mixinClassName) {
        if (mixinClassName.contains("FancyMenu")) {
            return CompatRequirements.FANCYMENU;
        }
        // DynamicLightsConfigAccessor e SettingEntryAccessor
        return CompatRequirements.LAMBDYNAMICLIGHTS;
    }

    private boolean all(String mixinClassName, ModRequirement... requirements) {
        for (ModRequirement requirement : requirements) {
            ModPresence.Result result = ModPresence.check(requirement);
            if (!result.ok()) {
                String reason = result.describe(requirement);
                if (loggedSkips.add(reason) && result.status() == ModPresence.Status.OUT_OF_RANGE) {
                    LOGGER.warn("[NexusCore] mixins que dependem de {} desligados: {}", requirement.modId(), reason);
                }
                return false;
            }
        }
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
