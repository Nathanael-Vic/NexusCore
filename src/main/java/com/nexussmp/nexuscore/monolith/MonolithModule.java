package com.nexussmp.nexuscore.monolith;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import com.nexussmp.nexuscore.core.util.ModRequirement;
import com.nexussmp.nexuscore.monolith.client.EchoClient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.PistonEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/**
 * Os monolitos do Ancient Remnants, do jeito do NexusSMP: so as estruturas, e o mundo quebrando em
 * volta delas.
 *
 * <ul>
 *   <li><b>O mod desligado.</b> Pelo pacote de dados embutido ({@code datapacks/monoliths}): sem
 *       bencao (a lista de cada estrutura fica vazia), sem receitas do Codex, sem fragmentos de
 *       pesquisa nos baus, sem spawners. Os itens somem do criativo e do JEI.</li>
 *   <li><b>Um de cada, raro.</b> {@link LoneMonolithPlacement}: seis no mundo, um de cada, longe do
 *       centro e em direcoes diferentes.</li>
 *   <li><b>Intocaveis.</b> {@link MonolithGuard}.</li>
 *   <li><b>O eco.</b> Quanto mais perto, mais errado ({@link Echo}): o terreno ({@link EchoFeature}),
 *       as criaturas ({@link EchoMobs}), o vazio e a tela ({@link EchoPlayers}, {@code EchoClient}).</li>
 * </ul>
 *
 * <p>Detalhes em {@code docs/MONOLITOS.md}.
 */
public final class MonolithModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";

    @Override
    public String id() {
        return ModuleIds.MONOLITH;
    }

    @Override
    public List<ModRequirement> requirements() {
        return List.of(ModRequirement.any(MonolithSites.NAMESPACE));
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        // Obrigatorio e por cima de tudo: e ele que desliga o resto do mod e posiciona as estruturas.
        modBus.addListener(AddPackFindersEvent.class, event -> event.addPackFinders(NexusCore.id("datapacks/monoliths"),
                PackType.SERVER_DATA, Component.literal("NexusCore: Monólitos"), PackSource.BUILT_IN, true, Pack.Position.TOP));
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, MonolithModule::hideItems);
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
            // Os lambdas do cliente so executam la; o servidor dedicado nunca carrega essa classe.
            registrar.playToClient(MonolithPayloads.Intensity.TYPE, MonolithPayloads.Intensity.STREAM_CODEC,
                    (payload, context) -> EchoClient.setIntensity(payload.value()));
            registrar.playToClient(MonolithPayloads.Glitched.TYPE, MonolithPayloads.Glitched.STREAM_CODEC,
                    (payload, context) -> EchoClient.markGlitched(payload.entityId()));
        });

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(EventPriority.HIGHEST, BlockEvent.BreakEvent.class, MonolithGuard::onBreak);
        game.addListener(EventPriority.HIGHEST, BlockEvent.EntityPlaceEvent.class, MonolithGuard::onPlace);
        game.addListener(EventPriority.HIGHEST, PlayerInteractEvent.RightClickBlock.class, MonolithGuard::onUseBlock);
        game.addListener(ExplosionEvent.Detonate.class, MonolithGuard::onExplosion);
        game.addListener(PistonEvent.Pre.class, MonolithGuard::onPiston);
        game.addListener(BlockEvent.FluidPlaceBlockEvent.class, MonolithGuard::onFluidPlace);
        game.addListener(BlockEvent.FarmlandTrampleEvent.class, MonolithGuard::onTrample);
        game.addListener(EntityMobGriefingEvent.class, MonolithGuard::onMobGriefing);

        game.addListener(EntityJoinLevelEvent.class, EchoMobs::onJoin);
        game.addListener(PlayerEvent.StartTracking.class, EchoMobs::onStartTracking);
        game.addListener(PlayerTickEvent.Post.class, EchoPlayers::onPlayerTick);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class, EchoPlayers::onLogout);
        game.addListener(ServerStoppedEvent.class, event -> EchoPlayers.clear());
    }

    /** O Codex e os fragmentos continuam registrados (o mod precisa), so nao aparecem para ninguem. */
    private static void hideItems(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> hidden = new ArrayList<>();
        for (ItemStack stack : event.getParentEntries()) {
            if (MonolithSites.NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())) {
                hidden.add(stack);
            }
        }
        for (ItemStack stack : event.getSearchEntries()) {
            if (MonolithSites.NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace())
                    && !hidden.contains(stack)) {
                hidden.add(stack);
            }
        }
        for (ItemStack stack : hidden) {
            event.remove(stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
