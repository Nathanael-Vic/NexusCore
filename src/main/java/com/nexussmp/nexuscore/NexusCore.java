package com.nexussmp.nexuscore;

import com.mojang.logging.LogUtils;
import com.nexussmp.nexuscore.backrooms.BackroomsModule;
import com.nexussmp.nexuscore.compat.curios.CuriosVault;
import com.nexussmp.nexuscore.compat.envelope.EnvelopeCompatModule;
import com.nexussmp.nexuscore.core.command.NexusCommands;
import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import com.nexussmp.nexuscore.core.module.ModuleManager;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.monolith.MonolithModule;
import com.nexussmp.nexuscore.monolith.MonolithRegistries;
import com.nexussmp.nexuscore.core.registry.NexusSounds;
import com.nexussmp.nexuscore.hologram.HologramModule;
import com.nexussmp.nexuscore.magic.MagicModule;
import com.nexussmp.nexuscore.morph.MorphModule;
import com.nexussmp.nexuscore.parrot.ParrotModule;
import com.nexussmp.nexuscore.quest.QuestModule;
import com.nexussmp.nexuscore.radio.RadioModule;
import com.nexussmp.nexuscore.relic.RelicModule;
import com.nexussmp.nexuscore.stats.StatsModule;
import com.nexussmp.nexuscore.vampire.VampireModule;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.util.List;

/**
 * Ponto de entrada comum (servidor dedicado e cliente).
 * Nada aqui pode tocar em classes de cliente: isso fica em {@link NexusCoreClient}.
 */
@Mod(NexusCore.MOD_ID)
public final class NexusCore {
    public static final String MOD_ID = "nexuscore";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NexusCore(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);

        NexusRegistries.register(modBus);
        MonolithRegistries.register(modBus);
        NexusSounds.register(modBus);
        NeoForge.EVENT_BUS.addListener(NexusCommands::register);

        // As vagas extras que entram no guarda-volumes junto com o inventario. Fica no setup, e nao
        // no construtor, porque so ali a lista de mods instalados ja esta completa.
        modBus.addListener(FMLCommonSetupEvent.class, event -> CuriosVault.install());

        ModuleManager.registerAll(List.of(new StatsModule(), new HologramModule(), new QuestModule(),
                new BackroomsModule(), new RadioModule(), new VampireModule(), new MorphModule(),
                new EnvelopeCompatModule(), new MagicModule(), new ParrotModule(), new RelicModule(),
                new MonolithModule()), modBus, container);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
