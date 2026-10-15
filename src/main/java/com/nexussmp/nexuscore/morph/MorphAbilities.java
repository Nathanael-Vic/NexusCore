package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.core.config.ServerConfig;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WaterBoundPathNavigation;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Vex;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O que o corpo novo sabe fazer: o basico, sem ataque nem poder especial.
 *
 * <ul>
 *   <li><b>Voar</b>, para bicho que voa: o voo do criativo (dois pulos), pelo atributo de voo do
 *       NeoForge — que se tira sem mexer em quem esta no criativo de verdade.</li>
 *   <li><b>Nadar rapido</b>, para bicho da agua: o dobro da velocidade de nado.</li>
 *   <li><b>Respirar na agua</b>, para bicho que respira nela.</li>
 *   <li><b>Nao queimar</b>, para bicho que nao pega fogo (blaze, ghast, magma): fogo e lava nao machucam.</li>
 * </ul>
 *
 * <p><b>Quem voa e quem nada</b> nao vem escrito em lugar nenhum; o jeito de saber e olhar como o
 * bicho se move. Bicho que voa anda com o GPS de voo do jogo, ou o controle de voo; bicho da agua,
 * com o GPS de agua ou o anfibio. E o que os bichos de mod tambem usam, entao a regra vale para eles
 * sem lista nenhuma. Alguns do jogo voam do jeito deles (morcego, blaze, vex) e entram pelo nome.
 *
 * <p>Tudo e decidido no servidor, ao trocar de forma. Cada uma das quatro liga e desliga na
 * configuracao.
 */
final class MorphAbilities {
    private static final ResourceLocation FLIGHT = NexusCore.id("morph_flight");
    private static final ResourceLocation SWIM = NexusCore.id("morph_swim");
    /** Soma a velocidade base de nado uma vez: nada o dobro. */
    private static final double SWIM_BONUS = 1.0;

    record Traits(boolean flies, boolean swims, boolean breathesWater, boolean fireImmune) {
        static final Traits NONE = new Traits(false, false, false, false);
    }

    private static final Map<UUID, Traits> CURRENT = new HashMap<>();

    /** Liga e desliga o que muda com a forma. Chamado quando a forma troca e quando a pessoa renasce. */
    static void update(ServerPlayer player) {
        Traits traits = traitsOf(MorphStands.of(player));
        if (traits.equals(Traits.NONE)) {
            CURRENT.remove(player.getUUID());
        } else {
            CURRENT.put(player.getUUID(), traits);
        }
        modifier(player, NeoForgeMod.CREATIVE_FLIGHT, FLIGHT, traits.flies() ? 1.0 : 0.0,
                AttributeModifier.Operation.ADD_VALUE);
        modifier(player, NeoForgeMod.SWIM_SPEED, SWIM, traits.swims() ? SWIM_BONUS : 0.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        // Perdeu o voo no meio do ar: desce, a nao ser que possa voar por outro motivo (criativo).
        if (player.getAbilities().flying && !player.mayFly()) {
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
    }

    private static Traits traitsOf(@Nullable LivingEntity creature) {
        if (creature == null) {
            return Traits.NONE;
        }
        PathNavigation navigation = creature instanceof Mob mob ? mob.getNavigation() : null;
        boolean flies = creature instanceof FlyingAnimal || creature instanceof FlyingMob
                || creature instanceof Bat || creature instanceof Blaze || creature instanceof Vex
                || navigation instanceof FlyingPathNavigation
                || (creature instanceof Mob mob && mob.getMoveControl() instanceof FlyingMoveControl);
        boolean swims = creature instanceof WaterAnimal
                || navigation instanceof WaterBoundPathNavigation
                || navigation instanceof AmphibiousPathNavigation;
        // O golfinho e da agua mas respira ar: sobe para respirar como qualquer mamifero. A pergunta
        // e a do NeoForge, "se afoga na agua?": e ela que os bichos de mod respondem.
        boolean breathes = !creature.canDrownInFluidType(NeoForgeMod.WATER_TYPE.value())
                || (creature instanceof WaterAnimal && !(creature instanceof Dolphin));
        boolean swimming = ServerConfig.get(ServerConfig.MORPH_SWIM);
        return new Traits(
                flies && ServerConfig.get(ServerConfig.MORPH_FLIGHT),
                swims && swimming,
                breathes && swimming,
                creature.fireImmune() && ServerConfig.get(ServerConfig.MORPH_FIRE_IMMUNITY));
    }

    private static void modifier(ServerPlayer player, Holder<Attribute> attribute, ResourceLocation id,
                                 double amount, AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        instance.removeModifier(id);
        if (amount != 0.0) {
            instance.addTransientModifier(new AttributeModifier(id, amount, operation));
        }
    }

    /**
     * Debaixo d'agua, quem virou peixe respira. O evento vem de toda criatura viva, todo tick: o
     * teste de jogador vem primeiro, para os bichos do mundo sairem antes de qualquer consulta.
     */
    static void onBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Traits traits = CURRENT.get(player.getUUID());
        if (traits != null && traits.breathesWater()) {
            event.setCanBreathe(true);
        }
    }

    /** Fogo e lava nao machucam quem virou bicho que nao queima. */
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && fireImmune(player)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
            event.setCanceled(true);
        }
    }

    /** O bicho que nao queima tambem nao anda pegando fogo. */
    static void tick(ServerPlayer player) {
        if (player.isOnFire() && fireImmune(player)) {
            player.clearFire();
        }
    }

    private static boolean fireImmune(ServerPlayer player) {
        Traits traits = CURRENT.get(player.getUUID());
        return traits != null && traits.fireImmune();
    }

    static void forget(UUID player) {
        CURRENT.remove(player);
    }

    static void clear() {
        CURRENT.clear();
    }

    private MorphAbilities() {}
}
