package com.nexussmp.nexuscore.morph;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * O que o anel e para o Curios: um anel que se veste com o clique direito, como uma armadura.
 *
 * <p>Com o anel na mao, o clique direito o poe na primeira vaga de anel livre; com todas ocupadas,
 * troca pelo que estava na primeira, e o anel antigo volta para a mao. Quem faz isso e o proprio
 * Curios — aqui so se diz que pode, e o som de vestir.
 *
 * <p>Fica fora do {@link MorphRingItem} de proposito: o item existe sem o Curios, esta classe nao.
 * O registro acontece no modulo, que so liga com o Curios instalado.
 */
final class MorphRingCurio implements ICurioItem {
    private static final ICurio.SoundInfo EQUIP_SOUND = new ICurio.SoundInfo(SoundEvents.ARMOR_EQUIP_GOLD.value(), 1.0F, 1.2F);

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return true;
    }

    @Override
    public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
        return EQUIP_SOUND;
    }

    /** O padrao do Curios ignora {@link #getEquipSound} e toca o som generico; aqui toca o do anel. */
    @Override
    public void onEquipFromUse(SlotContext slotContext, ItemStack stack) {
        LivingEntity wearer = slotContext.entity();
        wearer.level().playSound(null, wearer.blockPosition(), EQUIP_SOUND.soundEvent(), wearer.getSoundSource(),
                EQUIP_SOUND.volume(), EQUIP_SOUND.pitch());
    }

    /** O anel e reliquia: na morte fica no dedo, e nao vai para o corpo (ver {@code RelicKeeper}). */
    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }
}
