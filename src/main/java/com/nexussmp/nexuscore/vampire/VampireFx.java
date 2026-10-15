package com.nexussmp.nexuscore.vampire;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ambient.Bat;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * A coroacao de um lorde vampiro — e o que sobra quando ele perde o posto.
 *
 * <p>Tres segundos, tudo do jogo, nada de textura nova:
 * <ol>
 *   <li>A tela de quem esta sendo coroado escurece (o efeito de escuridao do warden) e um coracao
 *       comeca a bater, cada vez mais depressa.</li>
 *   <li>Uma espiral de sangue sobe dos pes ate acima da cabeca, com uma nevoa rente ao chao.</li>
 *   <li>O coracao para. No silencio, um sino grave da meia-noite, um estouro de sangue e tinta, e
 *       uma revoada de morcegos sai da pessoa e se espalha — some em fumaca pouco depois.</li>
 *   <li>Na tela dela, "Lorde Vampiro" em vermelho escuro.</li>
 * </ol>
 * Quem esta em volta ve e ouve tudo; a escuridao e o titulo sao so de quem foi coroado.
 *
 * <p>Os morcegos sao morcegos de verdade, soltos com a IA deles, e apagados em fumaca depois de dois
 * segundos e meio: dura o bastante para encher o lugar, nao o bastante para virar praga.
 */
public final class VampireFx {
    /** Quando o coracao para e a revoada sai. */
    private static final int FINALE_TICK = 44;
    /** Os batimentos, cada vez mais juntos: o ultimo e quase em cima do penultimo. */
    private static final int[] HEARTBEATS = {0, 13, 24, 32, 38, 42};
    private static final int BAT_COUNT = 8;
    private static final int BAT_LIFE_TICKS = 50;
    private static final DustParticleOptions BLOOD = new DustParticleOptions(new Vector3f(0.55F, 0.02F, 0.05F), 1.3F);
    private static final DustParticleOptions DARK_BLOOD = new DustParticleOptions(new Vector3f(0.25F, 0.0F, 0.02F), 1.6F);

    /** Quem esta sendo coroado agora, e em que tick do ritual. */
    private static final Map<UUID, Integer> RITUALS = new HashMap<>();
    /** Os morcegos da revoada, e quantos ticks faltam para cada um sumir. */
    private static final Map<Bat, Integer> BATS = new HashMap<>();

    /** Comeca a coroacao. A mensagem de lorde chega no fim, junto com o titulo. */
    public static void crown(ServerPlayer player) {
        RITUALS.put(player.getUUID(), 0);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, FINALE_TICK + 20, 0, true, false, false));
        player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.AMBIENT_CAVE.value(),
                SoundSource.PLAYERS, 0.7F, 0.7F);
    }

    /** Perdeu o posto: o sangue escorre, vira fumaca, e uma alma escapa. */
    public static void fade(ServerPlayer player) {
        RITUALS.remove(player.getUUID());
        ServerLevel level = player.serverLevel();
        level.sendParticles(BLOOD, player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.35, 0.6, 0.35, 0.0);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(),
                20, 0.4, 0.6, 0.4, 0.02);
        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1.6, player.getZ(), 4, 0.2, 0.2, 0.2, 0.03);
        level.playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.7F, 0.6F);
        player.sendSystemMessage(Component.translatable("nexuscore.vampire.no_longer_lord").withStyle(ChatFormatting.GRAY));
    }

    static void tick(MinecraftServer server) {
        if (!RITUALS.isEmpty()) {
            Iterator<Map.Entry<UUID, Integer>> rituals = RITUALS.entrySet().iterator();
            while (rituals.hasNext()) {
                Map.Entry<UUID, Integer> entry = rituals.next();
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                int tick = entry.getValue();
                if (player == null) {
                    rituals.remove();
                } else if (tick >= FINALE_TICK) {
                    finale(player);
                    rituals.remove();
                } else {
                    ritual(player, tick);
                    entry.setValue(tick + 1);
                }
            }
        }
        if (!BATS.isEmpty()) {
            Iterator<Map.Entry<Bat, Integer>> bats = BATS.entrySet().iterator();
            while (bats.hasNext()) {
                Map.Entry<Bat, Integer> entry = bats.next();
                Bat bat = entry.getKey();
                int left = entry.getValue() - 1;
                if (bat.isRemoved()) {
                    bats.remove();
                } else if (left <= 0) {
                    if (bat.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.SMOKE, bat.getX(), bat.getY() + 0.3, bat.getZ(), 6, 0.1, 0.1, 0.1, 0.01);
                    }
                    bat.discard();
                    bats.remove();
                } else {
                    entry.setValue(left);
                }
            }
        }
    }

    /** Um tick do ritual: a espiral subindo, a nevoa no chao e o coracao. */
    private static void ritual(ServerPlayer player, int tick) {
        ServerLevel level = player.serverLevel();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        float progress = tick / (float) FINALE_TICK;

        // Dois bracos de sangue, um de cada lado, subindo e fechando em volta da pessoa.
        double radius = Mth.lerp(progress, 1.0, 0.45);
        double height = progress * 2.3;
        for (int arm = 0; arm < 2; arm++) {
            double angle = tick * 0.42 + arm * Math.PI;
            level.sendParticles(BLOOD, x + Math.cos(angle) * radius, y + height, z + Math.sin(angle) * radius,
                    1, 0.0, 0.0, 0.0, 0.0);
        }
        // Nevoa rente ao chao, girando devagar ao contrario.
        if (tick % 2 == 0) {
            double angle = -tick * 0.2;
            for (int i = 0; i < 3; i++) {
                double around = angle + i * (Math.PI * 2.0 / 3.0);
                level.sendParticles(ParticleTypes.SMOKE, x + Math.cos(around) * 1.2, y + 0.1, z + Math.sin(around) * 1.2,
                        1, 0.05, 0.0, 0.05, 0.0);
            }
        }
        for (int beat : HEARTBEATS) {
            if (beat == tick) {
                level.playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS,
                        1.0F, 0.85F + progress * 0.25F);
                level.sendParticles(DARK_BLOOD, x, y + 1.1, z, 6, 0.15, 0.2, 0.15, 0.0);
            }
        }
    }

    /** O coracao parou: sino, estouro, revoada e o titulo. */
    private static void finale(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        RandomSource random = level.getRandom();
        double x = player.getX();
        double y = player.getY() + 1.1;
        double z = player.getZ();

        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.2F, 0.5F);
        level.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 0.8F, 0.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, player.blockPosition(), SoundEvents.BAT_TAKEOFF, SoundSource.PLAYERS, 1.0F, 1.15F);

        level.sendParticles(BLOOD, x, y, z, 60, 0.5, 0.7, 0.5, 0.0);
        level.sendParticles(ParticleTypes.SQUID_INK, x, y, z, 30, 0.3, 0.4, 0.3, 0.12);

        for (int i = 0; i < BAT_COUNT; i++) {
            Bat bat = EntityType.BAT.create(level);
            if (bat == null) {
                continue;
            }
            double angle = i * (Math.PI * 2.0 / BAT_COUNT);
            bat.moveTo(x + Math.cos(angle) * 0.3, y + random.nextDouble() * 0.5, z + Math.sin(angle) * 0.3,
                    (float) Math.toDegrees(angle) - 90.0F, 0.0F);
            bat.setResting(false);
            bat.setDeltaMovement(Math.cos(angle) * 0.35, 0.25 + random.nextDouble() * 0.2, Math.sin(angle) * 0.35);
            if (level.addFreshEntity(bat)) {
                BATS.put(bat, BAT_LIFE_TICKS + random.nextInt(15));
            }
        }

        player.connection.send(new ClientboundSetTitlesAnimationPacket(8, 50, 20));
        player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("nexuscore.vampire.lord_subtitle")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable("nexuscore.vampire.lord_title")
                .withStyle(ChatFormatting.DARK_RED)));
        player.sendSystemMessage(Component.translatable("nexuscore.vampire.you_are_lord").withStyle(ChatFormatting.DARK_RED));
    }

    static void forget(UUID player) {
        RITUALS.remove(player);
    }

    static void clear() {
        RITUALS.clear();
        BATS.clear();
    }

    private VampireFx() {}
}
