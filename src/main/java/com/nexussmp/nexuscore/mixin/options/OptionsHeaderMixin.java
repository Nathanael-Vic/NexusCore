package com.nexussmp.nexuscore.mixin.options;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.nexussmp.nexuscore.compat.options.client.OptionsScreenSettings;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * Devolve o cabecalho da tela de opcoes ao tamanho de fabrica.
 *
 * <p>O cabecalho da tela tem altura fixa (61 px) e cabe exatamente o titulo mais a linha com campo de
 * visao e dificuldade. Quando um mod embrulha essa linha para pendurar um botao dele ali (o Sable faz
 * isso com o "Sub-Level Settings"), sobram 25 px que o cabecalho nao tem: o titulo sobe ate encostar
 * no topo e tudo fica amassado. Aqui desembrulhamos: fica so o elemento original do jogo.
 *
 * <p>Prioridade acima do padrao de proposito — assim este ajuste roda depois do mod que embrulhou.
 */
@Mixin(value = OptionsScreen.class, priority = 1500)
public abstract class OptionsHeaderMixin {

    @ModifyReturnValue(method = "createOnlineButton", at = @At("RETURN"))
    private LayoutElement nexus$unwrapHeaderExtras(LayoutElement original) {
        // O jogo devolve o botao de dificuldade, o de online ou um EqualSpacingLayout; LinearLayout
        // aqui so aparece quando alguem embrulhou.
        if (!(original instanceof LinearLayout wrapper) || !OptionsScreenSettings.removeHeaderExtras()) {
            return original;
        }
        List<LayoutElement> children = new ArrayList<>();
        wrapper.visitChildren(children::add);
        return children.isEmpty() ? original : children.get(0);
    }
}
