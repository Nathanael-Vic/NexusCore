package com.nexussmp.nexuscore.relic;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Onde se vestem as reliquias de usar no corpo: a pena nas costas, o coracao no pescoco e o amuleto
 * como berloque — as vagas do Curios.
 *
 * <p>Esta e a porta segura: nenhuma assinatura aqui fala do Curios, e o {@link RelicCurios} so e
 * tocado depois de {@link #present()} dizer que o mod esta la. Sem o Curios as reliquias de vestir
 * existem (os registros sao fixos), so nao tem onde vestir — e nada aqui quebra.
 */
public final class RelicSlots {
    private static Boolean present;

    public static boolean present() {
        if (present == null) {
            present = ModList.get().isLoaded("curios");
        }
        return present;
    }

    /** A reliquia vestida, a pilha de verdade (mexer nela mexe na vaga); vazio se nao esta vestida. */
    public static ItemStack worn(LivingEntity entity, Item item) {
        return present() ? RelicCurios.find(entity, item) : ItemStack.EMPTY;
    }

    private RelicSlots() {}
}
