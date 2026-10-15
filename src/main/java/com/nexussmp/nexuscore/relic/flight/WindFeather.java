package com.nexussmp.nexuscore.relic.flight;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.relic.RelicSlots;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A pena dos ventos: planar sem elytra.
 *
 * <p>Vestida nas costas (a vaga {@code back} do Curios), ela faz exatamente o que a elytra faz:
 * pulou no ar, abre o voo. Por cima, ela segura um pouco mais no ar ({@link #LIFT}): a mesma
 * descida rende quase o dobro de distancia que numa elytra. O jogo trata o voo como o de elytra —
 * pose, camera, a conta do servidor — porque, para ele, e. A diferenca entra em tres pontos
 * ({@code mixin.relic}): onde o jogo pergunta "o peito deixa voar?", a resposta passa a ser
 * "deixa, ou a pena deixa".
 *
 * <p><b>O preco.</b> Pousou, a pena descansa {@value #COOLDOWN_TICKS} ticks antes de abrir de novo;
 * e fogos de artificio nao funcionam com ela — e planar, nao foguete. Quem tem elytra de verdade
 * continua usando fogos com a elytra.
 */
public final class WindFeather {
    public static final int COOLDOWN_TICKS = 8 * 20;
    /** Quanto ela segura por tick, contra a gravidade (0.08). Metade do que se perde planando reto. */
    public static final double LIFT = 0.012;

    /** Quem esta no ar pela pena agora, so no servidor: e na hora de pousar que a recarga comeca. */
    private static final Set<UUID> GLIDING = new HashSet<>();

    /** A pena deixa esta entidade planar agora? Perguntado a cada tick, dos dois lados. */
    public static boolean canGlide(LivingEntity entity) {
        if (!(entity instanceof Player player) || !RelicSlots.present()) {
            return false;
        }
        return !RelicSlots.worn(player, NexusRegistries.WIND_FEATHER.get()).isEmpty()
                && !player.getCooldowns().isOnCooldown(NexusRegistries.WIND_FEATHER.get());
    }

    /** Esta no ar pela pena, e nao por uma elytra de verdade no peito? */
    public static boolean glidingByFeather(Player player) {
        return player.isFallFlying() && !player.getItemBySlot(EquipmentSlot.CHEST).canElytraFly(player)
                && !RelicSlots.worn(player, NexusRegistries.WIND_FEATHER.get()).isEmpty();
    }

    /** Pousou depois de planar pela pena: comeca a recarga. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (glidingByFeather(player)) {
            GLIDING.add(player.getUUID());
        } else if (!player.isFallFlying() && GLIDING.remove(player.getUUID())) {
            player.getCooldowns().addCooldown(NexusRegistries.WIND_FEATHER.get(), COOLDOWN_TICKS);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_ELYTRA.value(),
                    SoundSource.PLAYERS, 0.6F, 1.4F);
        }
    }

    /** Fogos nao empurram quem plana pela pena — dos dois lados, para o cliente nem gastar o foguete. */
    public static void onUseItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (!event.getItemStack().is(Items.FIREWORK_ROCKET) || !glidingByFeather(player)) {
            return;
        }
        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
        if (player instanceof ServerPlayer server) {
            server.displayClientMessage(Component.translatable("nexuscore.relic.wind_feather.no_rockets")
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    public static void forget(UUID player) {
        GLIDING.remove(player);
    }

    public static void clear() {
        GLIDING.clear();
    }

    private WindFeather() {}
}
