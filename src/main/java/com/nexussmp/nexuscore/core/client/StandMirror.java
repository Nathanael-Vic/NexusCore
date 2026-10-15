package com.nexussmp.nexuscore.core.client;

import com.nexussmp.nexuscore.mixin.morph.WalkAnimationStateAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;

/**
 * Copia de uma criatura para outra tudo o que as animacoes leem.
 *
 * <p>Serve quando uma criatura e desenhada no lugar de outra: o anel da metamorfose desenha o bicho
 * no lugar da pessoa, e o correio do lorde desenha o morcego no lugar do pombo. A criatura
 * desenhada nunca entra no mundo, entao nao anda sozinha — antes de cada tick e de cada quadro ela
 * recebe da original a posicao, para onde olham o corpo e a cabeca, o balanco das pernas, a idade em
 * ticks (que move asas, caudas e as animacoes do GeckoLib), se esta no chao, correndo, agachada,
 * levando dano ou batendo.
 *
 * <p>Faltar um desses e o motivo classico de criatura trocada que aparece parada, ou nem aparece: o
 * desenho dela le um relogio que ninguem acertou.
 */
public final class StandMirror {

    public static void mirror(LivingEntity source, LivingEntity stand) {
        stand.setPos(source.getX(), source.getY(), source.getZ());
        stand.xo = source.xo;
        stand.yo = source.yo;
        stand.zo = source.zo;
        stand.xOld = source.xOld;
        stand.yOld = source.yOld;
        stand.zOld = source.zOld;
        stand.setYRot(source.getYRot());
        stand.yRotO = source.yRotO;
        stand.setXRot(source.getXRot());
        stand.xRotO = source.xRotO;
        stand.yBodyRot = source.yBodyRot;
        stand.yBodyRotO = source.yBodyRotO;
        stand.yHeadRot = source.yHeadRot;
        stand.yHeadRotO = source.yHeadRotO;
        copyWalk(source.walkAnimation, stand.walkAnimation);
        stand.tickCount = source.tickCount;
        stand.setOnGround(source.onGround());
        stand.setDeltaMovement(source.getDeltaMovement());
        stand.setShiftKeyDown(source.isShiftKeyDown());
        stand.setSprinting(source.isSprinting());
        stand.setInvisible(source.isInvisible());
        stand.hurtTime = source.hurtTime;
        stand.hurtDuration = source.hurtDuration;
        stand.deathTime = source.deathTime;
        stand.attackAnim = source.attackAnim;
        stand.oAttackAnim = source.oAttackAnim;
        stand.swinging = source.swinging;
        stand.swingTime = source.swingTime;
        stand.swingingArm = source.swingingArm;
    }

    /** O balanco das pernas. Os campos sao privados no jogo; o mixin da acesso a eles. */
    private static void copyWalk(WalkAnimationState from, WalkAnimationState to) {
        WalkAnimationStateAccessor source = (WalkAnimationStateAccessor) from;
        WalkAnimationStateAccessor target = (WalkAnimationStateAccessor) to;
        target.nexus$setSpeedOld(source.nexus$getSpeedOld());
        to.setSpeed(from.speed());
        target.nexus$setPosition(from.position());
    }

    private StandMirror() {}
}
