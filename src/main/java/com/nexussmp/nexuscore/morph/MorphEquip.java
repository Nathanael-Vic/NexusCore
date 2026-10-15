package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.core.config.CommonConfig;
import com.nexussmp.nexuscore.core.module.ModuleIds;
import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Vestir o anel transforma; tirar desfaz.
 *
 * <p>O servidor confere o que cada um esta vestindo a cada meio segundo, em vez de ouvir o Curios
 * trocar de item. Conferir e barato, e pega todos os caminhos de uma vez: vestir, tirar, morrer com o
 * anel, entrar no servidor ja com ele, e a configuracao mudar e a forma deixar de valer.
 *
 * <p>So transforma se a criatura <b>couber</b> onde a pessoa esta. Vestir um enderman dentro de uma
 * casa poria os olhos dentro do teto, e o jogo sufoca quem tem a cabeca num bloco. Sem espaco, avisa
 * e continua conferindo: a transformacao acontece assim que houver lugar.
 */
final class MorphEquip {
    private static final int INTERVAL_TICKS = 10;
    /** O aviso de falta de espaco se repete, mas nao a cada meio segundo. */
    private static final int NO_ROOM_NOTICE_TICKS = 60;

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || (player.tickCount + player.getId()) % INTERVAL_TICKS != 0) {
            return;
        }
        MorphAbilities.tick(player);
        MorphForm worn = CommonConfig.isModuleEnabled(ModuleIds.MORPH) ? wornForm(player) : null;
        MorphForm current = MorphState.form(player);
        if (Objects.equals(worn, current)) {
            return;
        }
        if (worn != null && !fits(player, worn)) {
            if (player.tickCount % NO_ROOM_NOTICE_TICKS < INTERVAL_TICKS) {
                player.displayClientMessage(Component.translatable("nexuscore.morph.no_room")
                        .withStyle(ChatFormatting.RED), true);
            }
            return;
        }
        MorphState.set(player, worn);
        MorphAbilities.update(player);
        broadcast(player);
    }

    /** A forma do primeiro anel carregado que a pessoa esta vestindo, se ainda valer. */
    @Nullable
    private static MorphForm wornForm(ServerPlayer player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(curios -> curios.findFirstCurio(MorphEquip::isChargedRing))
                .map(SlotResult::stack)
                .map(stack -> stack.get(NexusRegistries.MORPH_FORM.get()))
                .filter(MorphSettings::allowed)
                .orElse(null);
    }

    private static boolean isChargedRing(ItemStack stack) {
        return stack.is(NexusRegistries.MORPH_RING.get()) && stack.has(NexusRegistries.MORPH_FORM.get());
    }

    private static boolean fits(ServerPlayer player, MorphForm form) {
        EntityDimensions size = MorphState.dimensionsOf(player.level(), form);
        return size == null
                || player.level().noCollision(player, size.makeBoundingBox(player.position()).deflate(1.0E-7));
    }

    /** Conta para quem se transformou e para todo mundo que esta vendo essa pessoa. */
    private static void broadcast(ServerPlayer player) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, sync(player));
    }

    private static MorphPayloads.Sync sync(ServerPlayer player) {
        return new MorphPayloads.Sync(player.getUUID(), Optional.ofNullable(MorphState.form(player)));
    }

    /** Quem passa a ver alguem fica sabendo da forma dele — ou de que ele nao tem nenhuma. */
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer viewer) {
            PacketDistributor.sendToPlayer(viewer, sync(target));
        }
    }

    /**
     * Voltar da morte ou trocar de dimensao cria a entidade da pessoa de novo, dos dois lados. A forma
     * continua a mesma; o tamanho e os clientes precisam ser lembrados.
     */
    static void onRecreated(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && MorphState.form(player) != null) {
            player.refreshDimensions();
            // A entidade nova nasce sem os atributos da antiga: o voo e o nado voltam aqui.
            MorphAbilities.update(player);
            broadcast(player);
        }
    }

    static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID player = event.getEntity().getUUID();
        MorphState.forget(player);
        MorphStands.forget(player);
        MorphAbilities.forget(player);
        MorphSounds.forget(player);
    }

    static void clear() {
        MorphState.clearServer();
        MorphStands.clearServer();
        MorphAbilities.clear();
        MorphSounds.clear();
    }

    private MorphEquip() {}
}
