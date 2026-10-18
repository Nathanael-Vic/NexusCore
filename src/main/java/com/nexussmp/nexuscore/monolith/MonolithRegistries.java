package com.nexussmp.nexuscore.monolith;

import com.nexussmp.nexuscore.NexusCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * O que os monolitos registram no jogo: o posicionamento "um so" e a geracao do estranho em volta.
 * Registrados sempre, como o resto do {@code NexusRegistries}; quem os usa e o pacote de dados dos
 * monolitos, que so entra com o Ancient Remnants instalado.
 */
public final class MonolithRegistries {
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENTS =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, NexusCore.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, NexusCore.MOD_ID);

    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<LoneMonolithPlacement>> LONE_MONOLITH =
            PLACEMENTS.register("lone_monolith", () -> () -> LoneMonolithPlacement.CODEC);

    public static final DeferredHolder<Feature<?>, EchoFeature> ECHO =
            FEATURES.register("monolith_echo", () -> new EchoFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modBus) {
        PLACEMENTS.register(modBus);
        FEATURES.register(modBus);
    }

    private MonolithRegistries() {}
}
