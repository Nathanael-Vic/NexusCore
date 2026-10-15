package com.nexussmp.nexuscore.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * O que se ve e se ouve quando a magia recusa alguem — e quando ela chega ou vai embora.
 *
 * <p>A recusa e um "chiado": um fio de fumaca na mao e o som de brasa apagando, como feitico que
 * morreu na boca. Uma vez por segundo no maximo, porque tentar lancar de novo e o reflexo de todo
 * mundo e fumaca sem parar vira incendio.
 */
public final class MagicFx {
    private static final long FIZZLE_INTERVAL_MS = 1000L;
    private static final Map<UUID, Long> LAST_FIZZLE = new HashMap<>();

    /** Quando o despertar estoura. */
    private static final int AWAKEN_FINALE_TICK = 42;
    /** Uma nota do arpejo a cada tantos ticks: sete notas ate o estouro. */
    private static final int ARPEGGIO_STEP = 6;
    /** Quanto tempo ela sobe: o bastante para sair um palmo do chao, nao para bater no teto. */
    private static final int LEVITATION_TICKS = 18;
    /** A descida devagar depois do estouro, ate encostar no chao. */
    private static final int SLOW_FALL_TICKS = 60;
    /** Quem esta despertando agora, e em que tick. */
    private static final Map<UUID, Integer> AWAKENINGS = new HashMap<>();

    /** Recusa com o chiado e a frase na barra de acao. */
    public static void refuse(ServerPlayer player, String key) {
        if (fizzle(player)) {
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
    }

    /**
     * So o chiado, sem frase: quando quem escreve a frase e o proprio Iron's Spells.
     *
     * @return {@code false} se ainda nao deu o intervalo desde o ultimo, e nada aconteceu
     */
    public static boolean fizzle(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = LAST_FIZZLE.get(player.getUUID());
        if (last != null && now - last < FIZZLE_INTERVAL_MS) {
            return false;
        }
        LAST_FIZZLE.put(player.getUUID(), now);
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getEyeY() - 0.4, player.getZ(),
                8, 0.25, 0.15, 0.25, 0.01);
        level.playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.35F, 1.8F);
        return true;
    }

    /**
     * Virou mago: o despertar. Uns dois segundos, tudo do jogo.
     *
     * <ol>
     *   <li>A pessoa sobe um palmo do chao e fica flutuando.</li>
     *   <li>As letras da mesa de encantamento vem de todo lado e entram nela, enquanto um anel
     *       roxo gira em volta, fechando.</li>
     *   <li>Um arpejo de cristal de ametista, nota por nota, cada uma mais aguda.</li>
     *   <li>No fim, um estouro de luz e de letras para fora, o som de feitico lancado, e ela desce
     *       devagar, como pena. Na tela dela, "Mago".</li>
     * </ol>
     * Quem esta em volta ve e ouve tudo; o titulo e so de quem despertou.
     */
    public static void awaken(ServerPlayer player) {
        AWAKENINGS.put(player.getUUID(), 0);
        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_TICKS, 0, true, false, false));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    static void tick(MinecraftServer server) {
        if (AWAKENINGS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> awakenings = AWAKENINGS.entrySet().iterator();
        while (awakenings.hasNext()) {
            Map.Entry<UUID, Integer> entry = awakenings.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            int tick = entry.getValue();
            if (player == null) {
                awakenings.remove();
            } else if (tick >= AWAKEN_FINALE_TICK) {
                awakenFinale(player);
                awakenings.remove();
            } else {
                awakenStep(player, tick);
                entry.setValue(tick + 1);
            }
        }
    }

    /** Um tick do despertar: letras entrando, o anel girando e uma nota do arpejo. */
    private static void awakenStep(ServerPlayer player, int tick) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.getRandom();
        double x = player.getX();
        double y = player.getY() + 1.1;
        double z = player.getZ();
        float progress = tick / (float) AWAKEN_FINALE_TICK;

        // A letra da mesa de encantamento nasce no ponto + deslocamento e voa ate o ponto: com
        // quantidade zero, o deslocamento vai inteiro, e cada letra sai de um lugar em volta.
        for (int i = 0; i < 4; i++) {
            double yaw = random.nextDouble() * Math.PI * 2.0;
            double pitch = (random.nextDouble() - 0.3) * Math.PI * 0.6;
            double reach = 2.0 + random.nextDouble();
            level.sendParticles(ParticleTypes.ENCHANT, x, y, z, 0,
                    Math.cos(yaw) * Math.cos(pitch) * reach, Math.sin(pitch) * reach, Math.sin(yaw) * Math.cos(pitch) * reach, 1.0);
        }
        // O anel roxo: tres pontos girando na altura do peito, fechando ate encostar.
        double radius = Mth.lerp(progress, 1.4, 0.4);
        for (int i = 0; i < 3; i++) {
            double angle = tick * 0.35 + i * (Math.PI * 2.0 / 3.0);
            level.sendParticles(ParticleTypes.WITCH, x + Math.cos(angle) * radius, y - 0.2, z + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        if (tick % ARPEGGIO_STEP == 0) {
            float pitch = 0.7F + tick / (float) ARPEGGIO_STEP * 0.18F;
            level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    1.4F, pitch);
        }
    }

    /** O estouro: luz e letras para fora, o feitico lancado, e a descida devagar. */
    private static void awakenFinale(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY() + 1.1;
        double z = player.getZ();
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 40, 0.1, 0.1, 0.1, 0.18);
        level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.6, z, 120, 0.4, 0.4, 0.4, 1.2);
        level.sendParticles(ParticleTypes.WITCH, x, y, z, 30, 0.5, 0.6, 0.5, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2F, 1.2F);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.5F);

        player.removeEffect(MobEffects.LEVITATION);
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALL_TICKS, 0, true, false, false));

        player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("nexuscore.magic.mage_subtitle")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("nexuscore.magic.mage_title")
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
        player.sendSystemMessage(Component.translatable("nexuscore.magic.you_are_mage").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Deixou de ser mago: a magia sai como fumaca. */
    public static void fade(ServerPlayer player) {
        AWAKENINGS.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(),
                24, 0.4, 0.6, 0.4, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.sendSystemMessage(Component.translatable("nexuscore.magic.no_longer_mage").withStyle(ChatFormatting.GRAY));
    }

    static void forget(UUID player) {
        LAST_FIZZLE.remove(player);
        AWAKENINGS.remove(player);
    }

    static void clear() {
        LAST_FIZZLE.clear();
        AWAKENINGS.clear();
    }

    private MagicFx() {}
}
