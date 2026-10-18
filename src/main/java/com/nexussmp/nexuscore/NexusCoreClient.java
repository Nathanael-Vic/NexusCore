package com.nexussmp.nexuscore;

import com.nexussmp.nexuscore.backrooms.client.BackroomsClientModule;
import com.nexussmp.nexuscore.compat.cosmeticarmor.CosmeticArmorCompatModule;
import com.nexussmp.nexuscore.compat.envelope.client.EnvelopeClientModule;
import com.nexussmp.nexuscore.compat.fog.FogGuard;
import com.nexussmp.nexuscore.compat.options.OptionsCompatModule;
import com.nexussmp.nexuscore.compat.sodium.SodiumCompatModule;
import com.nexussmp.nexuscore.compat.waterframes.WaterFramesCompatModule;
import com.nexussmp.nexuscore.core.client.NexusResources;
import com.nexussmp.nexuscore.core.client.PlayerSkins;
import com.nexussmp.nexuscore.core.client.menu.MainMenu;
import com.nexussmp.nexuscore.core.config.ClientConfig;
import com.nexussmp.nexuscore.core.module.ModuleManager;
import com.nexussmp.nexuscore.display.client.DisplayClientModule;
import com.nexussmp.nexuscore.hologram.client.HologramClientModule;
import com.nexussmp.nexuscore.magic.client.MagicClientModule;
import com.nexussmp.nexuscore.monolith.client.MonolithClientModule;
import com.nexussmp.nexuscore.morph.client.MorphClientModule;
import com.nexussmp.nexuscore.parrot.client.ParrotClientModule;
import com.nexussmp.nexuscore.quest.client.QuestClientModule;
import com.nexussmp.nexuscore.radio.client.RadioClientModule;
import com.nexussmp.nexuscore.relic.client.RelicClientModule;
import com.nexussmp.nexuscore.space.client.SpaceClientModule;
import com.nexussmp.nexuscore.vampire.client.VampireClientModule;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.List;

/** Ponto de entrada so do cliente. O servidor dedicado nunca carrega esta classe. */
@Mod(value = NexusCore.MOD_ID, dist = Dist.CLIENT)
public final class NexusCoreClient {

    public NexusCoreClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        // Caches de cliente que varios modulos usam: esvaziam ao sair do servidor, para o proximo
        // mundo nao herdar skin de gente que nem esta la. Fica aqui, e nao dentro de um modulo,
        // porque nao pertence a nenhum deles em particular.
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class,
                event -> PlayerSkins.clear());
        // O menu principal do NexusSMP no lugar da tela de titulo (desliga em nexuscore-client.toml).
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Opening.class, MainMenu::onOpening);
        // A nevoa dos biomas que a encurtam volta a aparecer com o Cirrus instalado (ver FogGuard).
        // Por ultimo e ouvindo os cancelados: o que vale e o que algum mod pediu.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, true, ViewportEvent.RenderFog.class, FogGuard::onRenderFog);
        // O resource pack do SMP, embutido e obrigatorio.
        modBus.addListener(AddPackFindersEvent.class, NexusResources::onAddPackFinders);
        ModuleManager.registerAll(List.of(
                new SodiumCompatModule(),
                new WaterFramesCompatModule(),
                new OptionsCompatModule(),
                new CosmeticArmorCompatModule(),
                new DisplayClientModule(),
                new SpaceClientModule(),
                new HologramClientModule(),
                new QuestClientModule(),
                new BackroomsClientModule(),
                new RadioClientModule(),
                new VampireClientModule(),
                new MorphClientModule(),
                new EnvelopeClientModule(),
                new MagicClientModule(),
                new ParrotClientModule(),
                new RelicClientModule(),
                new MonolithClientModule()), modBus, container);
    }
}
