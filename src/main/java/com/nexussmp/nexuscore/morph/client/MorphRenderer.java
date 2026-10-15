package com.nexussmp.nexuscore.morph.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nexussmp.nexuscore.core.client.StandMirror;
import com.nexussmp.nexuscore.mixin.morph.EntityRendererAccessor;
import com.nexussmp.nexuscore.morph.MorphForm;
import com.nexussmp.nexuscore.morph.MorphState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Desenha a criatura no lugar de quem esta transformado.
 *
 * <p>Quando o jogo vai desenhar um jogador transformado, o desenho dele e cancelado e o da criatura
 * entra no mesmo lugar, com o desenhista da propria criatura — o mesmo que desenha ela solta no
 * mundo. E por isso que um bicho de mod aparece com o modelo, a textura e as animacoes do mod.
 *
 * <p><b>Espelhar.</b> A criatura solta nao anda sozinha: antes de cada desenho, ela recebe da pessoa
 * tudo o que as animacoes leem — posicao, para onde olha o corpo e a cabeca, o balanco das pernas, a
 * idade em ticks (que move asas, caudas e as animacoes do GeckoLib), se esta no chao, correndo,
 * agachada, levando dano ou batendo. Faltar um desses e o motivo classico de morph que aparece
 * parado, ou nao aparece: o desenho do bicho le um relogio que ninguem acertou.
 *
 * <p><b>Sombra.</b> A do bicho: o jogo desenha a sombra depois, lendo o raio no desenhista do
 * jogador, entao o raio dele e trocado pelo do bicho so para quem esta transformado.
 *
 * <p><b>Nome.</b> Quem vira bicho fica sem nome em cima — o desenho do jogador, que e quem poe o
 * nome, nem chega a acontecer. Quem vira outro jogador ganha o nome dele, porque o boneco
 * ({@link DisguisePlayer}) tem o perfil de quem foi copiado.
 */
public final class MorphRenderer {
    /**
     * O raio de sombra original de cada desenhista de jogador, visto antes de qualquer troca. Os
     * desenhistas sao refeitos quando os recursos recarregam; os velhos saem daqui sozinhos.
     */
    private static final Map<EntityRenderer<?>, Float> PLAYER_SHADOWS = new WeakHashMap<>();

    static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player instanceof DisguisePlayer) {
            return;
        }
        // O desenhista de jogador e um so para todos os jogadores: a sombra volta ao raio de jogador
        // a cada um, e so quem virou bicho troca, logo abaixo.
        EntityRendererAccessor playerShadow = (EntityRendererAccessor) event.getRenderer();
        float playerRadius = PLAYER_SHADOWS.computeIfAbsent(event.getRenderer(), renderer -> playerShadow.nexus$getShadowRadius());
        playerShadow.nexus$setShadowRadius(playerRadius);

        MorphForm form = MorphState.form(player);
        if (form == null) {
            return;
        }
        LivingEntity stand = MorphClient.standFor(player, form);
        if (stand == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        mirror(player, stand);
        EntityRenderer<? super LivingEntity> renderer =
                Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(stand);

        // Uma pilha so nossa, copiada da do jogo: se o desenho de um mod quebrar no meio, a pilha do
        // jogo continua inteira e o resto do quadro sai normal.
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(event.getPoseStack().last().pose());
        poseStack.last().normal().set(event.getPoseStack().last().normal());
        // O jogo ja desceu a pilha pelo desenho do jogador (agachado, ele desce um pouco); troca
        // esse ajuste pelo da criatura.
        Vec3 playerOffset = player instanceof AbstractClientPlayer clientPlayer
                ? event.getRenderer().getRenderOffset(clientPlayer, partialTick)
                : Vec3.ZERO;
        Vec3 offset = renderer.getRenderOffset(stand, partialTick).subtract(playerOffset);
        poseStack.translate(offset.x, offset.y, offset.z);

        float yaw = Mth.rotLerp(partialTick, player.yRotO, player.getYRot());
        try {
            renderer.render(stand, yaw, partialTick, poseStack, event.getMultiBufferSource(), event.getPackedLight());
        } catch (RuntimeException | LinkageError error) {
            // O jogador normal aparece neste quadro; o tipo fica de lado ate sair do servidor.
            MorphClient.markBroken(form, error);
            return;
        }
        if (!(stand instanceof DisguisePlayer)) {
            playerShadow.nexus$setShadowRadius(shadowOf(stand, renderer));
        }
        event.setCanceled(true);
    }

    /**
     * O raio da sombra da criatura: o que o desenhista dela usa, pela metade para filhote — a mesma
     * conta que o jogo faz quando desenha um filhote solto no mundo.
     */
    private static float shadowOf(LivingEntity stand, EntityRenderer<? super LivingEntity> renderer) {
        float radius = ((EntityRendererAccessor) renderer).nexus$getShadowRadius();
        return stand instanceof Mob mob && mob.isBaby() ? radius * 0.5F : radius;
    }

    /**
     * Tudo o que as animacoes da criatura leem, copiado da pessoa ({@link StandMirror}), mais o que
     * e so de disfarce. Antes de cada tick e de cada quadro.
     */
    static void mirror(Player player, LivingEntity stand) {
        StandMirror.mirror(player, stand);
        if (stand instanceof DisguisePlayer disguise) {
            // Quem esta disfarcado de gente continua sendo gente: agacha, nada, plana e segura o que
            // tem na mao e veste o que tem no corpo.
            disguise.setPose(player.getPose());
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.ANIMAL_ARMOR) {
                    disguise.setItemSlot(slot, player.getItemBySlot(slot));
                }
            }
        }
    }

    /** Quem virou bicho nao tem braco de gente em primeira pessoa. O item na mao continua. */
    static void onRenderArm(RenderArmEvent event) {
        MorphForm form = MorphState.form(event.getPlayer());
        if (form != null && !form.isPlayer()) {
            event.setCanceled(true);
        }
    }

    /**
     * O nome do disfarce aparece para os outros, nunca para quem esta disfarcado: o jogo nao mostra
     * o proprio nome de ninguem, nem na camera de fora.
     */
    static void onNameTag(RenderNameTagEvent event) {
        Entity camera = Minecraft.getInstance().getCameraEntity();
        if (event.getEntity() instanceof DisguisePlayer disguise
                && camera != null && disguise.wearer().equals(camera.getUUID())) {
            event.setCanRender(TriState.FALSE);
        }
    }

    /**
     * A entidade de alguem pode chegar aqui depois da forma (quem volta da morte, troca de mundo ou
     * entra no alcance de visao). Ajusta o tamanho assim que ela aparece.
     */
    static void onJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() && event.getEntity() instanceof Player player
                && !(player instanceof DisguisePlayer) && MorphState.form(player) != null) {
            player.refreshDimensions();
        }
    }

    private MorphRenderer() {}
}
