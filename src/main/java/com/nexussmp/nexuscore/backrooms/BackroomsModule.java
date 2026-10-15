package com.nexussmp.nexuscore.backrooms;

import com.nexussmp.nexuscore.backrooms.client.BackroomsTransition;
import com.nexussmp.nexuscore.backrooms.client.SkinwalkerEffects;
import com.nexussmp.nexuscore.backrooms.network.BackroomsTransitionPayload;
import com.nexussmp.nexuscore.backrooms.network.SkinwalkerPayload;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.module.NexusModule;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * As backrooms: o andar gerado, a queda e a volta.
 *
 * <p>O pacote da animacao e registrado sempre (protocolo nao pode variar com config); com o modulo
 * desligado, o evento nao sorteia e o comando recusa.
 */
public final class BackroomsModule implements NexusModule {
    private static final String PROTOCOL_VERSION = "1";
    /** De quanto em quanto tempo o sorteio acontece, por pessoa. */
    private static final int ROLL_INTERVAL_TICKS = 1200;

    @Override
    public String id() {
        return ModuleIds.BACKROOMS;
    }

    @Override
    public void register(IEventBus modBus, ModContainer container) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(PROTOCOL_VERSION)
                // So executa no cliente; o servidor dedicado nunca carrega a classe da animacao.
                .playToClient(BackroomsTransitionPayload.TYPE, BackroomsTransitionPayload.STREAM_CODEC,
                        (payload, context) -> BackroomsTransition.start(payload))
                .playToClient(SkinwalkerPayload.TYPE, SkinwalkerPayload.STREAM_CODEC,
                        (payload, context) -> SkinwalkerEffects.start(payload)));

        IEventBus game = NeoForge.EVENT_BUS;
        game.addListener(ServerTickEvent.Post.class, event -> BackroomsManager.tick(event.getServer()));
        game.addListener(PlayerInteractEvent.RightClickBlock.class, BackroomsModule::onRightClick);
        game.addListener(PlayerTickEvent.Post.class, BackroomsModule::onPlayerTick);
        game.addListener(ServerStoppedEvent.class, event -> BackroomsManager.clear());

        // As regras de la dentro: sem fome, sem cavar, sem morrer.
        game.addListener(PlayerTickEvent.Post.class, BackroomsRules::onPlayerTick);
        game.addListener(BlockEvent.BreakEvent.class, BackroomsRules::onBreak);
        game.addListener(BlockEvent.EntityPlaceEvent.class, BackroomsRules::onPlace);
        game.addListener(EntityJoinLevelEvent.class, BackroomsRules::onEntityJoin);
        // Dano e morte chegam na frente dos outros mods de proposito. O PlayerRevive derruba a
        // pessoa ao ver o dano fatal e o Corpse deixa o corpo ao ver a morte; cancelando primeiro,
        // nenhum dos dois chega a agir aqui dentro. A morte ainda escuta o evento ja cancelado,
        // para a rede de seguranca valer mesmo que outro mod cancele antes por conta propria.
        game.addListener(EventPriority.HIGHEST, LivingIncomingDamageEvent.class, BackroomsRules::onDamage);
        game.addListener(EventPriority.HIGHEST, true, LivingDeathEvent.class, BackroomsRules::onDeath);
        game.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> SkinwalkerDirector.forget(event.getEntity().getUUID()));
    }

    /** Qualquer porta de madeira la dentro e saida: em vez de abrir, devolve a pessoa. */
    private static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!BackroomsManager.isExitDoor(event.getLevel(), event.getPos())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            BackroomsManager.bringBack(player);
        }
    }

    /**
     * O sorteio do evento raro.
     *
     * <p>Uma vez por minuto, por pessoa, e so no mundo principal. Com a chance padrao (1 em 20160)
     * da mais ou menos uma queda a cada duas semanas de jogo sem parar — e o tempo de espera depois
     * de cair impede duas seguidas.
     */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % ROLL_INTERVAL_TICKS != 0
                || !BackroomsSettings.enabled()) {
            return;
        }
        int odds = BackroomsSettings.eventOdds();
        if (odds <= 0
                || !player.level().dimension().equals(Level.OVERWORLD)
                || !player.gameMode.getGameModeForPlayer().isSurvival()
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        BackroomsData data = BackroomsData.get(player.server);
        if (data.newest() == null) {
            return;
        }
        // Quem nunca caiu nao tem espera nenhuma. Subtrair a hora de agora do "nunca" estourava a
        // conta, dava negativo, e todo mundo ficava em espera para sempre: o evento nao acontecia.
        long last = data.lastEvent(player.getUUID());
        long cooldown = (long) BackroomsSettings.eventCooldownMinutes() * ROLL_INTERVAL_TICKS;
        if (last != BackroomsData.NEVER && level.getGameTime() - last < cooldown) {
            return;
        }
        if (level.getRandom().nextInt(odds) != 0) {
            return;
        }
        if (BackroomsManager.startFall(player)) {
            data.markEvent(player.getUUID(), level.getGameTime());
        }
    }
}
