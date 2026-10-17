package com.nexussmp.nexuscore.compat.xaero;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Desliga o minimapa e o mapa-mundi do Xaero para quem esta nas backrooms ou fora do corpo.
 *
 * <p>Um mapa acaba com o lugar: o labirinto vira um desenho, a porta de saida vira um ponto, e o
 * corredor sem fim deixa de ser sem fim. Aqui dentro nao existe mapa. E a alma fora do corpo esta
 * em outro plano, so olhando: o radar mostraria tudo o que a nevoa esconde.
 *
 * <p>O jeito de fazer isso e do proprio Xaero: os dois mods registram efeitos de status que
 * desligam as suas telas ("Disabled by a status effect!"), justamente para o servidor poder
 * proibi-las. Entao nao ha API, mixin nem dependencia — sao efeitos procurados pelo id no registro
 * do jogo. Sem os mods instalados, a lista sai vazia e tudo aqui vira nada.
 *
 * <p>O efeito e curto e renovado de tempos em tempos, em vez de infinito: se o servidor cair com
 * alguem la dentro, ou algum caminho torto tirar a pessoa sem passar pela volta, o bloqueio expira
 * sozinho em poucos segundos em vez de acompanhar o jogador para sempre.
 *
 * <p>Vale para os dois lados, e por um motivo pratico: o Xaero costuma estar so no cliente. Um
 * efeito que o servidor nao conhece ele nao tem como mandar, entao o servidor aplica quando tambem
 * tem os mods instalados, e o cliente aplica em si mesmo pelo {@link XaeroMapsClient}. Onde os dois
 * existem, o segundo so ve o efeito ja aplicado e nao faz nada.
 */
public final class XaeroMaps {
    /** Quanto tempo cada aplicacao dura. */
    private static final int DURATION_TICKS = 300;
    /** Abaixo disto a aplicacao e renovada — evita mandar pacote de efeito a cada checagem. */
    private static final int REFRESH_BELOW_TICKS = 120;

    private static final List<ResourceLocation> IDS = List.of(
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_minimap"),
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_entity_radar"),
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_waypoints"),
            ResourceLocation.fromNamespaceAndPath("xaerominimap", "no_cave_maps"),
            ResourceLocation.fromNamespaceAndPath("xaeroworldmap", "no_world_map"),
            ResourceLocation.fromNamespaceAndPath("xaeroworldmap", "no_cave_maps"));

    /** Resolvido uma vez, na primeira necessidade: o registro ja esta fechado quando o mundo abre. */
    private static List<Holder<MobEffect>> effects;

    /** Liga (ou renova) o bloqueio. Barato o bastante para ser chamado de segundo em segundo. */
    public static void block(Player player) {
        for (Holder<MobEffect> effect : effects()) {
            MobEffectInstance active = player.getEffect(effect);
            if (active == null || active.getDuration() < REFRESH_BELOW_TICKS) {
                // Sem particula e sem icone: e uma regra do lugar, nao um efeito para a pessoa ler.
                player.addEffect(new MobEffectInstance(effect, DURATION_TICKS, 0, true, false, false));
            }
        }
    }

    /** Devolve os mapas assim que a pessoa sai. */
    public static void release(Player player) {
        for (Holder<MobEffect> effect : effects()) {
            player.removeEffect(effect);
        }
    }

    private static List<Holder<MobEffect>> effects() {
        if (effects == null) {
            List<Holder<MobEffect>> found = new ArrayList<>(IDS.size());
            for (ResourceLocation id : IDS) {
                BuiltInRegistries.MOB_EFFECT.getHolder(id).ifPresent(found::add);
            }
            effects = List.copyOf(found);
        }
        return effects;
    }

    private XaeroMaps() {}
}
