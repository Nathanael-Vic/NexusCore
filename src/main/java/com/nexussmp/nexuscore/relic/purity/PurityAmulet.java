package com.nexussmp.nexuscore.relic.purity;

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
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * O amuleto da pureza: tres gotas de agua clara presas no cristal.
 *
 * <p>Cada efeito ruim que tentar pegar quem o usa — veneno, wither, fraqueza, lentidao, cegueira,
 * fome, nausea, a escuridao do warden, o que for — e bebido por uma gota antes de pegar. A gota
 * escurece, o efeito nao acontece, e um som de vidro avisa. Efeito bom nao gasta nada.
 *
 * <p>Sem gotas, ele nao protege: o efeito pega normal. Elas voltam sozinhas, uma a cada
 * {@value #REFILL_TICKS} ticks (um minuto e meio), enquanto ele esta vestido — na vaga {@code charm}
 * do Curios. Da para ver quantas restam no proprio amuleto: cada gota acesa e uma carga.
 *
 * <p>E protecao para o susto, nao imunidade: tres golpes de aranha da caverna seguidos ja secam o
 * amuleto, e quem fica parado numa nuvem de veneno gasta tudo em segundos.
 */
public final class PurityAmulet {
    public static final int MAX_DROPS = 3;
    public static final int REFILL_TICKS = 90 * 20;

    public static int drops(ItemStack amulet) {
        Integer drops = amulet.get(NexusRegistries.PURITY_DROPS.get());
        return drops == null ? MAX_DROPS : drops;
    }

    /** Um efeito ruim vem vindo: se houver gota, ele nao pega. */
    public static void onApplicable(MobEffectEvent.Applicable event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (!(event.getEntity() instanceof ServerPlayer player)
                || effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL) {
            return;
        }
        ItemStack amulet = RelicSlots.worn(player, NexusRegistries.PURITY_AMULET.get());
        int drops = amulet.isEmpty() ? 0 : drops(amulet);
        if (drops <= 0) {
            return;
        }
        event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        setDrops(player, amulet, drops - 1);

        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.SPLASH, player.getX(), player.getY() + 1.0, player.getZ(), 16, 0.3, 0.5, 0.3, 0.1);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 6, 0.25, 0.3, 0.25, 0.02);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.6F);
        level.playSound(null, player.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.3F);
        player.displayClientMessage(Component.translatable("nexuscore.relic.purity_amulet.absorbed",
                effect.getEffect().value().getDisplayName(), drops - 1).withStyle(ChatFormatting.AQUA), true);
    }

    /** Uma gota volta a cada minuto e meio, enquanto o amuleto esta vestido. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ItemStack amulet = RelicSlots.worn(player, NexusRegistries.PURITY_AMULET.get());
        if (amulet.isEmpty() || drops(amulet) >= MAX_DROPS || !Relics.ready(player.level(), amulet)) {
            return;
        }
        int drops = drops(amulet) + 1;
        setDrops(player, amulet, drops);
        player.playNotifySound(SoundEvents.AMETHYST_CLUSTER_STEP, SoundSource.PLAYERS, 0.8F, 1.8F);
    }

    /** Muda as gotas e, se ainda falta alguma, marca quando vem a proxima. */
    private static void setDrops(ServerPlayer player, ItemStack amulet, int drops) {
        amulet.set(NexusRegistries.PURITY_DROPS.get(), drops);
        if (drops < MAX_DROPS) {
            Long readyAt = amulet.get(NexusRegistries.RELIC_READY_AT.get());
            long now = Relics.now(player.level());
            // Gastar outra gota nao atrasa a que ja vinha vindo.
            if (readyAt == null || readyAt <= now) {
                amulet.set(NexusRegistries.RELIC_READY_AT.get(), now + REFILL_TICKS);
            }
        } else {
            amulet.remove(NexusRegistries.RELIC_READY_AT.get());
        }
    }

    private PurityAmulet() {}
}
