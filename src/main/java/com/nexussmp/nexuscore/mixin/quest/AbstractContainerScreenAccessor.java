package com.nexussmp.nexuscore.mixin.quest;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Para o painel de quests empurrar o inventario para o lado, como o livro de receitas faz.
 * A posicao da tela e calculada no {@code init}; aqui so ajustamos depois dela.
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    void nexus$setLeftPos(int leftPos);
}
