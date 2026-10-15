package com.nexussmp.nexuscore.backrooms;

import com.nexussmp.nexuscore.compat.corpse.Corpses;
import com.nexussmp.nexuscore.compat.playerrevive.PlayerReviveCompat;
import com.nexussmp.nexuscore.compat.xaero.XaeroMaps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * As regras de la dentro: nao se quebra bloco, nao se passa fome, nao se morre e nao se usa mapa.
 *
 * <p>O lugar nao e um desafio de sobrevivencia — e um susto. Cavar uma saida estragaria o labirinto,
 * fome so atrapalharia quem vai passar alguns minutos ali, e morrer de verdade tiraria a graca: quem
 * e pego volta para o mundo, com as coisas de volta, como se tivesse saido por uma porta.
 *
 * <p>As duas regras que precisam <b>chegar primeiro</b> que as dos outros mods — dano e morte — sao
 * registradas em {@code EventPriority.HIGHEST} por {@link BackroomsModule}. E o que garante que o
 * PlayerRevive nao derrube ninguem no estado de abatido aqui dentro e que o Corpse nunca veja uma
 * morte para deixar corpo no corredor.
 */
public final class BackroomsRules {
    /** De quanto em quanto tempo a fome e reposta (e o resto das regras de tempo e conferido). */
    private static final int FOOD_INTERVAL_TICKS = 20;

    public static boolean inBackrooms(Level level) {
        return level.dimension().equals(BackroomsDimensions.BACKROOMS);
    }

    /** Fome congelada, mapas desligados e ninguem caido: o pente-fino de uma vez por segundo. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || player.tickCount % FOOD_INTERVAL_TICKS != 0
                || !inBackrooms(player.level())) {
            return;
        }
        // Quem chegou caido (PlayerRevive) nao fica: aqui nao existe abatido, e caido ninguem anda
        // ate uma porta. Volta para o mundo, exatamente no estado em que entrou.
        if (PlayerReviveCompat.isDowned(player)) {
            BackroomsManager.bringBack(player);
            return;
        }
        // Minimapa e mapa-mundi ficam desligados enquanto a pessoa estiver la dentro.
        XaeroMaps.block(player);
        if (player.getFoodData().getFoodLevel() < 20) {
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(5.0F);
        }
    }

    /** Cavar para fora nao e uma opcao. No criativo continua valendo, para poder arrumar as coisas. */
    public static void onBreak(BlockEvent.BreakEvent event) {
        // O evento traz um "acesso ao mundo", nao necessariamente um mundo: um mod que o dispare
        // durante a geracao, por exemplo, manda outra coisa. Conferir antes de converter.
        if (event.getLevel() instanceof Level level && inBackrooms(level) && !event.getPlayer().isCreative()) {
            event.setCanceled(true);
        }
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level) || !inBackrooms(level)) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || !player.isCreative()) {
            event.setCanceled(true);
        }
    }

    /**
     * Dano nenhum passa.
     *
     * <p>O que o skinwalker faz nao e dano: ele pega a pessoa e ela acorda no mundo. Deixar dano de
     * verdade acontecer ali significaria perder item e nivel por causa de uma brincadeira.
     *
     * <p>Esta e a regra que, na pratica, impede o estado de abatido do PlayerRevive: ele decide
     * derrubar a pessoa ao ver o dano fatal, e aqui o dano e cancelado antes de chegar nele.
     */
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player && inBackrooms(event.getEntity().level())) {
            event.setCanceled(true);
        }
    }

    /**
     * Rede de seguranca: se por algum caminho a morte chegar, ela vira uma saida pela porta.
     *
     * <p>Roda cedo de proposito. O Corpse escuta a morte por ultimo para deixar o corpo, e o
     * PlayerRevive escuta cedo para derrubar em vez de matar; cancelando aqui, nenhum dos dois ve
     * morte nenhuma acontecer nas backrooms.
     */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !inBackrooms(player.level())) {
            return;
        }
        event.setCanceled(true);
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        BackroomsManager.bringBack(player);
    }

    /**
     * Corpo nenhum se levanta aqui: o unico rastro do lugar e a cabeca que o skinwalker deixa.
     *
     * <p>So no servidor — e ele quem decide o que existe; o cliente apenas desenha o que recebeu.
     */
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && inBackrooms(event.getLevel()) && Corpses.isCorpse(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    private BackroomsRules() {}
}
