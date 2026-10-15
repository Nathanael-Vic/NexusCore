package com.nexussmp.nexuscore.relic.heart;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import com.nexussmp.nexuscore.relic.RelicSlots;
import com.nexussmp.nexuscore.relic.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O coracao de brasa: quanto mais perto de morrer, mais forte quem o carrega.
 *
 * <p><b>A brasa.</b> Acima de metade da vida ele dorme. Abaixo, acende, e a forca cresce ate o fim:
 * a intensidade vai de 0 (metade da vida) a 1 (um decimo da vida). No maximo, o golpe sai
 * {@value #MAX_DAMAGE_BONUS} vezes mais forte (alem do normal) e o que chega machuca
 * {@value #MAX_RESISTANCE} a menos. Em volta da pessoa, brasas — mais quanto mais perto do fim — e,
 * bem perto, o coracao batendo forte so para ela.
 *
 * <p><b>Sobreviver.</b> Quem passou pela brasa e saiu da luta ({@value #CALM_TICKS} ticks sem bater
 * nem apanhar) recebe de volta {@value #RECOVERY} do que perdeu, uma vez por luta.
 *
 * <p><b>Morrer.</b> Morreu com ele no pescoco, ele vira cinzas e dorme por
 * {@value #DORMANT_TICKS} ticks (meia hora): nao acende, nao cura, e a textura fica cinza. Vestido
 * na vaga {@code necklace} do Curios.
 */
public final class EmberHeart {
    /** Abaixo desta fracao da vida, a brasa acende. */
    private static final float IGNITE_AT = 0.5F;
    /** Nesta fracao, ela esta no maximo. */
    private static final float FULL_AT = 0.1F;
    private static final float MAX_DAMAGE_BONUS = 0.7F;
    private static final float MAX_RESISTANCE = 0.45F;
    private static final int CALM_TICKS = 8 * 20;
    private static final float RECOVERY = 0.4F;
    public static final int DORMANT_TICKS = 30 * 60 * 20;

    /** Quando cada pessoa lutou por ultimo, e se a brasa acendeu nessa luta. */
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    private static final class Fight {
        private long lastBlow;
        private boolean ignited;
    }

    /** O coracao vestido e acordado, ou vazio. */
    static ItemStack awake(ServerPlayer player) {
        ItemStack heart = RelicSlots.worn(player, NexusRegistries.EMBER_HEART.get());
        return !heart.isEmpty() && Relics.ready(player.level(), heart) ? heart : ItemStack.EMPTY;
    }

    /** 0 com metade da vida ou mais; 1 com um decimo ou menos. */
    static float intensity(ServerPlayer player) {
        float fraction = player.getHealth() / player.getMaxHealth();
        return Mth.clamp((IGNITE_AT - fraction) / (IGNITE_AT - FULL_AT), 0.0F, 1.0F);
    }

    /** Golpe dado e golpe levado passam por aqui: um sai mais forte, o outro chega mais fraco. */
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && !awake(attacker).isEmpty()) {
            float heat = intensity(attacker);
            event.setAmount(event.getAmount() * (1.0F + MAX_DAMAGE_BONUS * heat));
            fought(attacker, heat);
        }
        if (event.getEntity() instanceof ServerPlayer victim && !awake(victim).isEmpty()) {
            float heat = intensity(victim);
            event.setAmount(event.getAmount() * (1.0F - MAX_RESISTANCE * heat));
            fought(victim, heat);
        }
    }

    private static void fought(ServerPlayer player, float heat) {
        Fight fight = FIGHTS.computeIfAbsent(player.getUUID(), ignored -> new Fight());
        fight.lastBlow = player.level().getGameTime();
        fight.ignited |= heat > 0.0F;
    }

    /** Morreu com o coracao no pescoco: cinzas. */
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        FIGHTS.remove(player.getUUID());
        ItemStack heart = awake(player);
        if (heart.isEmpty()) {
            return;
        }
        heart.set(NexusRegistries.RELIC_READY_AT.get(), Relics.now(player.level()) + DORMANT_TICKS);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.2, player.getZ(), 16, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 1.0F, 0.6F);
        player.sendSystemMessage(Component.translatable("nexuscore.relic.ember_heart.ashes").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** As brasas em volta, o coracao batendo, e a cura de quem saiu vivo. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 10 != 0 || !player.isAlive()) {
            return;
        }
        if (awake(player).isEmpty()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        float heat = intensity(player);
        if (heat > 0.0F) {
            int embers = 1 + (int) (heat * 6);
            level.sendParticles(ParticleTypes.SMALL_FLAME, player.getX(), player.getY() + 1.0, player.getZ(),
                    embers, 0.35, 0.5, 0.35, 0.01);
            if (heat > 0.5F) {
                level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 1.2, player.getZ(), 1, 0.2, 0.2, 0.2, 0.0);
            }
            if (heat > 0.6F && player.tickCount % 30 == 0) {
                player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 0.6F + heat * 0.4F, 1.1F);
            }
        }
        Fight fight = FIGHTS.get(player.getUUID());
        if (fight != null && fight.ignited && level.getGameTime() - fight.lastBlow >= CALM_TICKS) {
            FIGHTS.remove(player.getUUID());
            float missing = player.getMaxHealth() - player.getHealth();
            if (missing > 0.0F) {
                player.heal(missing * RECOVERY);
                level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.6, 0.4, 0.04);
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 0.5F, 1.4F);
                player.displayClientMessage(Component.translatable("nexuscore.relic.ember_heart.recovered")
                        .withStyle(ChatFormatting.GOLD), true);
            }
        }
    }

    public static void forget(UUID player) {
        FIGHTS.remove(player);
    }

    public static void clear() {
        FIGHTS.clear();
    }

    private EmberHeart() {}
}
