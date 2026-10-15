package com.nexussmp.nexuscore.morph;

import com.nexussmp.nexuscore.core.registry.NexusRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * O anel da metamorfose.
 *
 * <p>Na mao, bater numa criatura copia a forma dela para o anel ({@link MorphCapture}). No dedo —
 * a vaga de anel do Curios — quem usa vira aquela forma ({@link MorphEquip}).
 *
 * <p>E um item comum de proposito, sem nada do Curios dentro: o item e registrado sempre, com ou
 * sem o Curios instalado, e uma classe dele aqui quebraria o jogo sem o mod. O Curios aceita o anel
 * pela tag {@code curios:ring}; o que ele faz no dedo e lido de fora, pelo modulo.
 */
public final class MorphRingItem extends Item {

    public MorphRingItem(Properties properties) {
        super(properties);
    }

    /** Com uma forma guardada, brilha como item encantado: da para ver de longe que esta carregado. */
    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(NexusRegistries.MORPH_FORM.get()) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        MorphForm form = stack.get(NexusRegistries.MORPH_FORM.get());
        if (form == null) {
            lines.add(Component.translatable("nexuscore.morph.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("nexuscore.morph.form", form.displayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        lines.add(Component.translatable("nexuscore.morph.hint").withStyle(ChatFormatting.GRAY));
        if (flag.isAdvanced()) {
            lines.add(Component.literal(form.type().toString()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
