package com.nexussmp.nexuscore.morph.client;

import com.mojang.authlib.GameProfile;
import com.nexussmp.nexuscore.NexusCore;
import com.nexussmp.nexuscore.morph.MorphForm;
import com.nexussmp.nexuscore.morph.MorphPayloads;
import com.nexussmp.nexuscore.morph.MorphStands;
import com.nexussmp.nexuscore.morph.MorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O que o cliente sabe das transformacoes, e o que ele desenha no lugar das pessoas.
 *
 * <p>Para bicho, a criatura e a do {@link MorphStands} — a mesma a quem o jogo pergunta os sons.
 * Para disfarce de jogador, um {@link DisguisePlayer}, guardado aqui.
 *
 * <p><b>A criatura vive.</b> A cada tick, a criatura de cada pessoa transformada roda o tick dela, com
 * tudo o que a pessoa esta fazendo copiado antes. E dali que saem as particulas do bicho (a fumaca
 * do blaze, o portal do enderman) e as animacoes que andam com o tempo e nao com o passo (a galinha
 * batendo asa, o sapo, o camelo, os bichos de mod que animam no tick). O que o tick mexe de posicao
 * e de pose e sobrescrito de novo antes de cada desenho; o que ele mexe nos outros — empurrar quem
 * esta encostado — e desligado no mixin, porque quem esta encostado e a propria pessoa.
 *
 * <p><b>O que der errado fica de fora.</b> Criatura de mod e desenhada e movida por codigo do mod, que
 * nunca esperou ser usado assim. Um tipo que nao pode ser criado aqui, ou cujo desenho quebra, e
 * posto de lado ate sair do servidor: a pessoa aparece como jogador. Um tipo cujo <i>tick</i> quebra
 * so deixa de viver: continua aparecendo, sem as particulas. Nos dois casos o log diz qual foi, uma
 * vez. Um bicho teimoso nunca derruba o jogo de ninguem.
 */
public final class MorphClient {
    private static final Map<UUID, Disguise> DISGUISES = new HashMap<>();
    /** Tipos que nao aparecem: a pessoa volta a ser desenhada como jogador. */
    private static final Set<ResourceLocation> BROKEN = new HashSet<>();
    /** Tipos que aparecem mas nao vivem: o tick deles quebrou. */
    private static final Set<ResourceLocation> STILL = new HashSet<>();

    private record Disguise(MorphForm form, DisguisePlayer entity) {}

    public static void apply(MorphPayloads.Sync payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        MorphState.setClient(level, payload.player(), payload.form().orElse(null));
        DISGUISES.remove(payload.player());
    }

    /** O que se desenha no lugar desta pessoa, ou {@code null} para desenhar ela mesma. */
    @Nullable
    static LivingEntity standFor(Player player, MorphForm form) {
        if (form.isPlayer()) {
            return disguiseFor(player, form);
        }
        if (BROKEN.contains(form.type())) {
            return null;
        }
        LivingEntity stand = MorphStands.of(player);
        if (stand == null) {
            markBroken(form, null);
            return null;
        }
        stand.setSilent(true);
        return stand;
    }

    @Nullable
    private static DisguisePlayer disguiseFor(Player player, MorphForm form) {
        Disguise disguise = DISGUISES.get(player.getUUID());
        if (disguise != null && disguise.form().equals(form) && disguise.entity().level() == player.level()) {
            return disguise.entity();
        }
        GameProfile copied = form.playerProfile();
        if (copied == null || !(player.level() instanceof ClientLevel level)) {
            return null;
        }
        DisguisePlayer entity = new DisguisePlayer(level, copied, player.getUUID());
        DISGUISES.put(player.getUUID(), new Disguise(form, entity));
        return entity;
    }

    /** A cada tick, a criatura de cada pessoa transformada em bicho vive um tick. */
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        for (AbstractClientPlayer player : minecraft.level.players()) {
            MorphForm form = MorphState.form(player);
            // Invisivel nao solta particula: seria um rastro dizendo onde a pessoa esta.
            if (form == null || form.isPlayer() || player.isInvisible() || STILL.contains(form.type())) {
                continue;
            }
            LivingEntity stand = standFor(player, form);
            if (stand == null) {
                continue;
            }
            MorphRenderer.mirror(player, stand);
            try {
                stand.tick();
            } catch (RuntimeException | LinkageError error) {
                if (STILL.add(form.type())) {
                    NexusCore.LOGGER.warn("[NexusCore] morph: o tick de {} quebrou; quem usar essa forma aparece "
                            + "sem as particulas e as animacoes de tick", form.type(), error);
                }
            }
        }
    }

    /** Poe um tipo de lado ate sair do servidor, e diz no log uma vez so. */
    static void markBroken(MorphForm form, @Nullable Throwable error) {
        if (form.isPlayer() || !BROKEN.add(form.type())) {
            return;
        }
        if (error == null) {
            NexusCore.LOGGER.warn("[NexusCore] morph: {} nao pode ser criado no cliente; quem usar essa forma "
                    + "aparece como jogador", form.type());
        } else {
            NexusCore.LOGGER.warn("[NexusCore] morph: o desenho de {} quebrou; quem usar essa forma aparece "
                    + "como jogador", form.type(), error);
        }
    }

    /** Saiu do servidor: nada daqui vale no proximo. */
    public static void clear() {
        DISGUISES.clear();
        BROKEN.clear();
        STILL.clear();
        MorphState.clearClient();
        MorphStands.clearClient();
    }

    private MorphClient() {}
}
