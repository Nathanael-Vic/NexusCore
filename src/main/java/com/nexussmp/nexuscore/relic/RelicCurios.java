package com.nexussmp.nexuscore.relic;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/**
 * O lado do Curios das reliquias de vestir. So carrega com o Curios instalado ({@link RelicSlots}).
 *
 * <p>Cada uma se veste com o clique direito, como armadura, e na morte fica na vaga
 * ({@code ALWAYS_KEEP}) — o Curios a leva junto para o corpo novo.
 */
final class RelicCurios {

    /** No setup do mod, com o Curios ja carregado. */
    static void register() {
        CuriosApi.registerCurio(NexusRegistries.WIND_FEATHER.get(), new Worn(SoundEvents.ARMOR_EQUIP_ELYTRA.value()));
        CuriosApi.registerCurio(NexusRegistries.EMBER_HEART.get(), new Worn(SoundEvents.ARMOR_EQUIP_CHAIN.value()));
        CuriosApi.registerCurio(NexusRegistries.PURITY_AMULET.get(), new Worn(SoundEvents.AMETHYST_BLOCK_CHIME));
    }

    static ItemStack find(LivingEntity entity, Item item) {
        return CuriosApi.getCuriosInventory(entity)
                .flatMap(handler -> handler.findFirstCurio(item))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    /** Veste com o clique direito, com o som de cada uma, e nunca cai na morte. */
    private record Worn(SoundEvent sound) implements ICurioItem {

        @Override
        public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
            return true;
        }

        @Override
        public ICurio.SoundInfo getEquipSound(SlotContext slotContext, ItemStack stack) {
            return new ICurio.SoundInfo(sound, 1.0F, 1.0F);
        }

        @Override
        public void onEquipFromUse(SlotContext slotContext, ItemStack stack) {
            LivingEntity wearer = slotContext.entity();
            wearer.level().playSound(null, wearer.blockPosition(), sound, wearer.getSoundSource(), 1.0F, 1.0F);
        }

        @Override
        public ICurio.DropRule getDropRule(SlotContext slotContext, DamageSource source, boolean recentlyHit, ItemStack stack) {
            return ICurio.DropRule.ALWAYS_KEEP;
        }
    }

    private RelicCurios() {}
}
