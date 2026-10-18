package com.nexussmp.nexuscore.monolith;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;
import java.util.List;

/**
 * As criaturas que nascem perto de um monolito saem erradas, e ficam assim para sempre (o jeito fica
 * salvo nelas, mesmo que andem para longe).
 *
 * <ul>
 *   <li><b>Estranho:</b> alguns bichos param e ficam encarando quem chega perto.</li>
 *   <li><b>Errado:</b> nomes de sistema ({@code entity_0482}, {@code placeholder_cow}), que aparecem
 *       ao mirar nelas.</li>
 *   <li><b>Quebrado:</b> a textura roxa e preta de "textura faltando" (quem desenha e o cliente), uns
 *       de cabeca para baixo, outros do tamanho errado.</li>
 * </ul>
 */
public final class EchoMobs {
    private static final String TAG = "nexuscore:echo";
    private static final int STARE = 1;
    public static final int GLITCH = 2;
    /** A criatura ja foi sorteada (para nao sortear de novo quando o chunk recarrega). */
    private static final int ROLLED = 4;

    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        CompoundTag data = mob.getPersistentData();
        int flags = data.getInt(TAG);
        // Os bichos que nascem com o mundo chegam "do disco" na primeira vez; o que vale e nunca ter
        // sido sorteado, nao de onde veio.
        if ((flags & ROLLED) == 0) {
            flags = roll(level, mob);
            data.putInt(TAG, flags);
        }
        if ((flags & STARE) != 0) {
            mob.goalSelector.addGoal(0, new StareGoal(mob));
        }
    }

    private static int roll(ServerLevel level, Mob mob) {
        List<MonolithSites.Site> sites = MonolithSites.of(level);
        float f = sites.isEmpty() ? 0.0F : Echo.intensity(sites, mob.getX(), mob.getZ());
        if (f <= 0.0F) {
            return ROLLED;
        }
        RandomSource random = mob.getRandom();
        int flags = ROLLED;
        if (mob instanceof Animal && random.nextFloat() < (f > Echo.STRANGE ? 0.25F : 0.2F)) {
            flags |= STARE;
        }
        boolean named = false;
        if (f > Echo.STRANGE && !mob.hasCustomName() && random.nextFloat() < (f > Echo.WRONG ? 0.45F : 0.3F)) {
            mob.setCustomName(Component.literal(systemName(mob, random)));
            named = true;
        }
        if (f > Echo.WRONG) {
            if (random.nextFloat() < 0.75F) {
                flags |= GLITCH;
            }
            if (!named && !mob.hasCustomName() && random.nextFloat() < 0.15F) {
                // O nome que o proprio jogo usa para virar a criatura de cabeca para baixo.
                mob.setCustomName(Component.literal("Dinnerbone"));
            }
            AttributeInstance scale = mob.getAttribute(Attributes.SCALE);
            if (scale != null && random.nextFloat() < 0.3F) {
                scale.setBaseValue(0.45 + random.nextDouble() * 1.3);
            }
        }
        return flags;
    }

    private static String systemName(Mob mob, RandomSource random) {
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
        return switch (random.nextInt(4)) {
            case 0 -> "entity_" + String.format("%04d", random.nextInt(10_000));
            case 1 -> "placeholder_" + type;
            case 2 -> type + ".name";
            default -> "null";
        };
    }

    public static boolean glitched(Entity entity) {
        return (entity.getPersistentData().getInt(TAG) & GLITCH) != 0;
    }

    /** Quem passa a ver a criatura fica sabendo que ela e "sem textura" (o cliente nao le o NBT). */
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer player && glitched(event.getTarget())) {
            PacketDistributor.sendToPlayer(player, new MonolithPayloads.Glitched(event.getTarget().getId()));
        }
    }

    /** Parada, encarando: so enquanto tem alguem por perto. Sem ninguem, volta a ser um bicho normal. */
    private static final class StareGoal extends Goal {
        private final Mob mob;
        private Player target;

        StareGoal(Mob mob) {
            this.mob = mob;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            target = mob.level().getNearestPlayer(mob, 20.0);
            return target != null && !target.isSpectator();
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && target.isAlive() && !target.isSpectator() && mob.distanceToSqr(target) < 24.0 * 24.0;
        }

        @Override
        public void start() {
            mob.getNavigation().stop();
        }

        @Override
        public void stop() {
            target = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            mob.getNavigation().stop();
            mob.getLookControl().setLookAt(target, 360.0F, 90.0F);
            // O corpo vira junto com a cabeca: encarando de frente, nao de canto de olho.
            mob.setYBodyRot(mob.getYHeadRot());
        }
    }

    private EchoMobs() {}
}
